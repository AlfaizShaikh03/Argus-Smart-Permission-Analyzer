package com.yourname.smartpermissionanalyzer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yourname.smartpermissionanalyzer.services.BackgroundScanService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Check if real-time scanning was enabled before reboot
                val prefs = context.getSharedPreferences("smart_permission_analyzer_settings", Context.MODE_PRIVATE)
                val realTimeScanning = prefs.getBoolean("real_time_scanning", true)

                if (realTimeScanning) {
                    // Restart background scanning service
                    BackgroundScanService.startService(context)
                }
            }
        }
    }
}
