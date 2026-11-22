package com.callfleet.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.callfleet.app.CallFleetApp
import com.callfleet.app.data.CallLogRepository
import com.callfleet.app.databinding.ActivityMainBinding
import com.callfleet.app.service.MonitoringService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = CallLogAdapter()

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.READ_CALL_LOG] == true) {
            startMonitoring()
            refreshList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.deviceLabel.text = "Device: ${CallFleetApp.label(this)}"
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.btnGrant.setOnClickListener { requestNeededPermissions() }

        if (hasCallLog()) {
            startMonitoring()
            refreshList()
        } else {
            requestNeededPermissions()
        }
    }

    private fun neededPermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    private fun requestNeededPermissions() = requestPerms.launch(neededPermissions())

    private fun hasCallLog() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    private fun startMonitoring() {
        val intent = Intent(this, MonitoringService::class.java)
        ContextCompat.startForegroundService(this, intent)
        binding.status.text = "Monitoring active — syncing to admin panel."
    }

    private fun refreshList() {
        if (!hasCallLog()) return
        val entries = CallLogRepository(this).readSince(0L).take(200)
        adapter.submit(entries)
        binding.emptyView.visibility =
            if (entries.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
