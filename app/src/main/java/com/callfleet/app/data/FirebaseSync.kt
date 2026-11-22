package com.callfleet.app.data

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Pushes this device's registration and call log into Firestore.
 *
 * Firestore layout:
 *   devices/{deviceId}                      -> DeviceInfo
 *   devices/{deviceId}/calls/{callId}       -> CallLogEntry
 *
 * The admin dashboard reads exactly this structure.
 */
class FirebaseSync {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** All devices sharing a fleet code read/write under the same fleet node. */
    private fun devices(fleetId: String) =
        db.collection("fleets").document(fleetId).collection("devices")

    /** Anonymous auth is enough to satisfy security rules; the fleet code is the shared key. */
    suspend fun ensureSignedIn(): String {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        return auth.currentUser!!.uid
    }

    suspend fun registerDevice(fleetId: String, deviceId: String, label: String, recordingSupported: Boolean) {
        val info = DeviceInfo(
            deviceId = deviceId,
            label = label,
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            recordingSupported = recordingSupported,
            consentAcceptedAt = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        devices(fleetId).document(deviceId)
            .set(info, SetOptions.merge()).await()
    }

    suspend fun touch(fleetId: String, deviceId: String, totalCalls: Int) {
        devices(fleetId).document(deviceId)
            .set(
                mapOf("lastSeen" to System.currentTimeMillis(), "totalCalls" to totalCalls),
                SetOptions.merge()
            ).await()
    }

    /**
     * Uploads a captured recording to Storage and links it on the call doc.
     * Only ever called when the OS actually let us record audio.
     */
    suspend fun uploadRecording(fleetId: String, deviceId: String, callId: String, file: File) {
        val ref = FirebaseStorage.getInstance().reference
            .child("recordings/$fleetId/$deviceId/${file.name}")
        ref.putFile(android.net.Uri.fromFile(file)).await()
        val url = ref.downloadUrl.await().toString()
        devices(fleetId).document(deviceId).collection("calls").document(callId)
            .set(mapOf("recordingUrl" to url), SetOptions.merge()).await()
    }

    suspend fun uploadCalls(fleetId: String, deviceId: String, entries: List<CallLogEntry>) {
        val batch = db.batch()
        val calls = devices(fleetId).document(deviceId).collection("calls")
        entries.forEach { entry ->
            batch.set(calls.document(entry.callId), entry, SetOptions.merge())
        }
        batch.commit().await()
    }
}
