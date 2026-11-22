package com.callfleet.app

import android.app.Application
import android.content.Context
import android.content.Intent
import com.callfleet.app.ui.CrashActivity
import com.google.firebase.FirebaseApp
import java.io.File
import java.util.UUID

class CallFleetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Show crashes on screen instead of silently closing, and save them to a
        // file, so startup failures can be read rather than guessed at.
        installCrashHandler()

        // Initialise Firebase defensively: a bad/placeholder google-services.json
        // must never take the whole app down at launch.
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Throwable) {
            // Sync just won't work until a real config is added; the app still runs.
            e.printStackTrace()
        }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = android.util.Log.getStackTraceString(throwable)
                val text = "CallFleet crash\n\n${throwable}\n\n$trace"
                // Persist to Android/data/com.callfleet.app/files/last_crash.txt
                runCatching {
                    File(getExternalFilesDir(null), "last_crash.txt").writeText(text)
                }
                // Show it on screen in a fresh process task.
                val i = Intent(this, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(CrashActivity.EXTRA_TRACE, text)
                startActivity(i)
            } catch (_: Throwable) {
                // ignore – fall through to default handling
            }
            previous?.uncaughtException(thread, throwable)
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }

    companion object {
        lateinit var instance: CallFleetApp
            private set

        private const val PREFS = "callfleet_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CONSENT = "consent_accepted"
        private const val KEY_LABEL = "device_label"
        private const val KEY_FLEET = "fleet_code"

        fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun deviceId(ctx: Context): String {
            val p = prefs(ctx)
            return p.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
                p.edit().putString(KEY_DEVICE_ID, it).apply()
            }
        }

        fun hasConsent(ctx: Context) = prefs(ctx).getBoolean(KEY_CONSENT, false)
        fun setConsent(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_CONSENT, v).apply()
        fun label(ctx: Context) = prefs(ctx).getString(KEY_LABEL, "") ?: ""
        fun setLabel(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_LABEL, v).apply()

        /** Shared code entered identically on every device in one fleet. */
        fun fleetId(ctx: Context) = prefs(ctx).getString(KEY_FLEET, "") ?: ""
        fun setFleetId(ctx: Context, v: String) =
            prefs(ctx).edit().putString(KEY_FLEET, v.trim().lowercase()).apply()
    }
}
