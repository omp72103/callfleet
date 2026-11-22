package com.callfleet.app.data

import android.content.Context
import android.provider.CallLog
import androidx.annotation.RequiresPermission

/**
 * Reads this device's own call history from the system CallLog provider.
 * Nothing here is hidden: it only ever reads the standard log that the
 * phone's own Dialer app already shows the user.
 */
class CallLogRepository(private val context: Context) {

    @RequiresPermission(android.Manifest.permission.READ_CALL_LOG)
    fun readSince(sinceMillis: Long): List<CallLogEntry> {
        val entries = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        val selection = "${CallLog.Calls.DATE} > ?"
        val args = arrayOf(sinceMillis.toString())

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            args,
            "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            val numIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durIdx = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (c.moveToNext()) {
                val number = c.getString(numIdx) ?: "Unknown"
                val date = c.getLong(dateIdx)
                entries.add(
                    CallLogEntry(
                        callId = "${date}_${number}",
                        number = number,
                        name = c.getString(nameIdx) ?: "",
                        type = typeLabel(c.getInt(typeIdx)),
                        timestamp = date,
                        durationSec = c.getLong(durIdx)
                    )
                )
            }
        }
        return entries
    }

    private fun typeLabel(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
        CallLog.Calls.MISSED_TYPE -> "MISSED"
        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
        CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
        CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
        else -> "UNKNOWN"
    }
}
