package com.callfleet.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.callfleet.app.CallFleetApp
import com.callfleet.app.databinding.ActivityConsentBinding

/**
 * The app opens here every time until consent is given. The person using the
 * phone must read the disclosure and tap Agree. This is the legal and ethical
 * backbone of the app: no silent enrolment.
 */
class ConsentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConsentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CallFleetApp.hasConsent(this)) {
            goToMain()
            return
        }

        binding = ActivityConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAgree.setOnClickListener {
            val label = binding.editLabel.text.toString().trim()
            if (label.isEmpty()) {
                Toast.makeText(this, "Please name this device first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CallFleetApp.setLabel(this, label)
            CallFleetApp.setConsent(this, true)
            goToMain()
        }

        binding.btnDecline.setOnClickListener { finishAffinity() }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
