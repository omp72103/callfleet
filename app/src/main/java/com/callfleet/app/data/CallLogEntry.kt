package com.callfleet.app.data

/**
 * One call as read from the device's own CallLog provider.
 * Mirrors the fields the admin dashboard displays.
 */
data class CallLogEntry(
    val callId: String = "",      // stable id = date+number, used as Firestore doc id
    val number: String = "",
    val name: String = "",        // contact name if the OS resolved one
    val type: String = "",        // INCOMING / OUTGOING / MISSED / REJECTED
    val timestamp: Long = 0L,     // epoch millis
    val durationSec: Long = 0L,
    val recordingUrl: String = "" // Storage URL if a recording was captured, else ""
)
