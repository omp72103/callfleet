package com.callfleet.app

import android.app.Application
import android.content.Context
import java.util.UUID

class CallFleetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
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
