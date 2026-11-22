package com.callfleet.app.service

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

/**
 * Watches the phone's call state so recording can start when a call goes active
 * and stop when it ends. Uses TelephonyCallback on Android 12+ and the older
 * PhoneStateListener below that. Requires READ_PHONE_STATE (granted at onboarding).
 */
class CallStateMonitor(
    private val context: Context,
    private val onCallActive: () -> Unit,
    private val onCallEnded: () -> Unit
) {
    private val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var legacyListener: PhoneStateListener? = null
    private var callback: TelephonyCallback? = null
    private var wasActive = false

    private fun handle(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!wasActive) { wasActive = true; onCallActive() }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasActive) { wasActive = false; onCallEnded() }
            }
            // RINGING: wait until it's answered (OFFHOOK) before recording.
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = handle(state)
            }
            callback = cb
            tm.registerTelephonyCallback(context.mainExecutor, cb)
        } else {
            @Suppress("DEPRECATION")
            val l = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) = handle(state)
            }
            legacyListener = l
            @Suppress("DEPRECATION")
            tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callback?.let { tm.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            legacyListener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
    }
}
