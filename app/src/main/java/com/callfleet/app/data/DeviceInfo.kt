package com.callfleet.app.data

data class DeviceInfo(
    val deviceId: String = "",
    val label: String = "",        // human-friendly name, e.g. "Field phone 1"
    val model: String = "",
    val androidVersion: String = "",
    val recordingSupported: Boolean = false,
    val consentAcceptedAt: Long = 0L,
    val lastSeen: Long = 0L,
    val totalCalls: Int = 0
)
