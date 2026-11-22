package com.callfleet.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.callfleet.app.CallFleetApp
import com.callfleet.app.R
import com.callfleet.app.data.CallLogRepository
import com.callfleet.app.data.FirebaseSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * A foreground service with a PERMANENT notification. The notification is not
 * optional cosmetics — it is how the person holding this phone is continuously
 * told, by the OS, that the device is enrolled and its call log is being synced.
 * Removing it would turn this into covert spyware, which is exactly what this
 * app is designed NOT to be.
 */
class MonitoringService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sync = FirebaseSync()

    private val recorder = CallRecorder()
    private var callMonitor: CallStateMonitor? = null
    private var activeRecordingFile: File? = null
    private var callStartedAt = 0L

    override fun onCreate() {
        super.onCreate()
        CallFleetContext.init(this)
        startAsForeground()
        setupRecording()
        scope.launch { loop() }
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Only claim the microphone service type if RECORD_AUDIO is actually
            // granted, otherwise Android 14+ throws when the service starts.
            var type = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            val micGranted = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (micGranted) {
                type = type or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            androidx.core.app.ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
    }

    /**
     * Starts/stops the recorder on call state changes. This is the wiring that
     * was previously missing — without it, the mic permission did nothing.
     * Note: even wired, Android 10+ blocks most apps from capturing call audio,
     * so start() may return false and no file is produced. That is expected and
     * handled, not a crash.
     */
    private fun setupRecording() {
        val recDir = File(cacheDir, "recordings").apply { mkdirs() }
        callMonitor = CallStateMonitor(
            context = this,
            onCallActive = {
                callStartedAt = System.currentTimeMillis()
                val tempId = callStartedAt.toString()
                val ok = recorder.start(recDir, tempId)
                if (!ok) activeRecordingFile = null
            },
            onCallEnded = {
                val file = recorder.stop()
                activeRecordingFile = file
                // Give the system a moment to write the call-log row, then link it.
                scope.launch {
                    delay(4000)
                    linkRecordingToLatestCall(file)
                }
            }
        ).also { it.start() }
    }

    private suspend fun linkRecordingToLatestCall(file: File?) {
        if (file == null || !file.exists() || file.length() < 1024L) return
        if (!hasCallLogPermission()) return
        val deviceId = CallFleetApp.deviceId(applicationContext)
        val fleetId = CallFleetApp.fleetId(applicationContext)
        // The just-ended call should be the newest log entry since the call began.
        val recent = CallLogRepository(applicationContext).readSince(callStartedAt - 60_000L)
        val entry = recent.firstOrNull() ?: return
        try {
            sync.uploadCalls(fleetId, deviceId, listOf(entry))   // ensure the call doc exists
            sync.uploadRecording(fleetId, deviceId, entry.callId, file)
        } catch (_: Exception) { /* offline or storage disabled */ }
    }

    private suspend fun loop() {
        val ctx = applicationContext
        val deviceId = CallFleetApp.deviceId(ctx)
        val fleetId = CallFleetApp.fleetId(ctx)
        sync.ensureSignedIn()
        val recordingSupported = CallRecorder().isLikelySupported()
        sync.registerDevice(fleetId, deviceId, CallFleetApp.label(ctx), recordingSupported)

        val repo = CallLogRepository(ctx)
        var lastSynced = CallFleetApp.prefs(ctx).getLong(KEY_LAST, 0L)

        while (true) {
            if (hasCallLogPermission()) {
                try {
                    val entries = repo.readSince(lastSynced)
                    if (entries.isNotEmpty()) {
                        sync.uploadCalls(fleetId, deviceId, entries)
                        lastSynced = entries.maxOf { it.timestamp }
                        CallFleetApp.prefs(ctx).edit().putLong(KEY_LAST, lastSynced).apply()
                    }
                    sync.touch(fleetId, deviceId, entries.size)
                } catch (_: Exception) { /* offline: retry next cycle */ }
            }
            delay(SYNC_INTERVAL_MS)
        }
    }

    private fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL, "Call monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shown while this device is enrolled in CallFleet." }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("This device is monitored by CallFleet")
            .setContentText("Call history is being synced to the admin panel.")
            .setSmallIcon(R.drawable.ic_monitor)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        callMonitor?.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "callfleet_monitor"
        private const val NOTIF_ID = 1001
        private const val KEY_LAST = "last_synced_ts"
        private const val SYNC_INTERVAL_MS = 60_000L
    }
}
