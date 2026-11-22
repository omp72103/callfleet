package com.callfleet.app.ui

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.callfleet.app.CallFleetApp
import com.callfleet.app.databinding.ActivityAdminBinding
import com.google.firebase.FirebaseApp
import org.json.JSONObject

/**
 * Embeds the exact same admin dashboard that ships as a standalone HTML file,
 * loaded from app assets, so an admin can browse every enrolled ("sub") device's
 * call logs without leaving the app. Firebase email/password sign-in and
 * Firestore reads happen inside the WebView just as in the browser.
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding.web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }
        binding.web.addJavascriptInterface(ExportBridge(), "AndroidExport")
        binding.web.addJavascriptInterface(BootBridge(), "CallFleetBoot")
        binding.web.loadUrl("file:///android_asset/admin-dashboard.html")
    }

    /**
     * Hands the embedded dashboard everything it needs to connect on its own:
     * the Firebase web config (derived from this app's own google-services.json)
     * and the fleet code. No email/password, no pasted config — the panel just
     * loads the fleet's pooled data.
     */
    inner class BootBridge {
        @JavascriptInterface
        fun getFleetId(): String = CallFleetApp.fleetId(this@AdminActivity)

        @JavascriptInterface
        fun getFirebaseConfig(): String {
            val o = FirebaseApp.getInstance().options
            val projectId = o.projectId ?: ""
            return JSONObject().apply {
                put("apiKey", o.apiKey)
                put("authDomain", "$projectId.firebaseapp.com")
                put("projectId", projectId)
                put("storageBucket", o.storageBucket ?: "")
                put("appId", o.applicationId)
                put("messagingSenderId", o.gcmSenderId ?: "")
            }.toString()
        }
    }

    override fun onBackPressed() {
        if (binding.web.canGoBack()) binding.web.goBack() else super.onBackPressed()
    }

    /** Lets the dashboard's export buttons save CSV/HTML into the phone's Downloads. */
    inner class ExportBridge {
        @JavascriptInterface
        fun saveFile(name: String, mime: String, base64: String) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) }
                }
                runOnUiThread { Toast.makeText(this@AdminActivity, "Saved $name to Downloads", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@AdminActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }
}
