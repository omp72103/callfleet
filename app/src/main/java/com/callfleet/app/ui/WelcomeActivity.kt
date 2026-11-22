package com.callfleet.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.callfleet.app.CallFleetApp
import com.callfleet.app.databinding.ActivityWelcomeBinding

/**
 * Entry point. Two roles:
 *  - Enrol this device  -> consent + monitoring (the phone being tracked)
 *  - Admin panel        -> embedded dashboard to view all enrolled devices
 *
 * If this phone is already enrolled, we jump straight into monitoring.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CallFleetApp.hasConsent(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editFleet.setText(CallFleetApp.fleetId(this))

        binding.btnEnroll.setOnClickListener {
            if (saveFleet()) startActivity(Intent(this, ConsentActivity::class.java))
        }
        binding.btnAdmin.setOnClickListener {
            if (saveFleet()) startActivity(Intent(this, AdminActivity::class.java))
        }
    }

    /** The same code must be typed on every device you want to share one dashboard. */
    private fun saveFleet(): Boolean {
        val code = binding.editFleet.text.toString().trim()
        if (code.length < 4) {
            Toast.makeText(this, "Enter a fleet code (4+ characters), the same on every device.", Toast.LENGTH_LONG).show()
            return false
        }
        CallFleetApp.setFleetId(this, code)
        return true
    }
}
