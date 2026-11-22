package com.callfleet.app.service

import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Consent-based call recording.
 *
 * IMPORTANT REALITY CHECK (read this before expecting audio):
 * Since Android 10 (API 29) Google removed the ability for ordinary apps to
 * capture the *remote* party's voice. VOICE_CALL / VOICE_COMMUNICATION capture
 * is reserved for the system dialer or privileged apps. On most stock Android
 * devices this recorder will capture little or nothing useful; on some OEM
 * builds (certain Xiaomi, Samsung, etc.) it still works. We therefore probe
 * support at runtime and the app shows an honest "recording not supported on
 * this device" state where the OS blocks it. We never try to defeat that block.
 */
class CallRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun isLikelySupported(): Boolean {
        // Android 9 and below allowed VOICE_CALL capture broadly.
        // 10+ is device-dependent; we report cautiously.
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
    }

    @Suppress("DEPRECATION")
    fun start(outDir: File, callId: String): Boolean {
        return try {
            val file = File(outDir, "rec_${callId.replace("[^A-Za-z0-9]".toRegex(), "_")}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(CallFleetContext.appContext())
            } else {
                MediaRecorder()
            }
            // VOICE_COMMUNICATION is the best-effort source that the OS may permit.
            r.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            outputFile = file
            true
        } catch (e: Exception) {
            // Blocked by OS or mic unavailable — expected on many devices.
            cleanup()
            false
        }
    }

    fun stop(): File? {
        return try {
            recorder?.stop()
            val f = outputFile
            cleanup()
            f
        } catch (e: Exception) {
            cleanup()
            null
        }
    }

    private fun cleanup() {
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
    }
}

/** Tiny holder so the recorder can reach an application Context on API 31+. */
object CallFleetContext {
    private var ctx: android.content.Context? = null
    fun init(c: android.content.Context) { ctx = c.applicationContext }
    fun appContext(): android.content.Context = ctx!!
}
