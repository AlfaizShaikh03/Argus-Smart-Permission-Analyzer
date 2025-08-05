package com.yourname.smartpermissionanalyzer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.yourname.smartpermissionanalyzer.domain.events.DomainEventBus
import com.yourname.smartpermissionanalyzer.domain.events.DomainEvent
import com.yourname.smartpermissionanalyzer.services.BackgroundScanService

@AndroidEntryPoint
class AppInstallationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var domainEventBus: DomainEventBus

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.encodedSchemeSpecificPart ?: return

        CoroutineScope(Dispatchers.IO).launch {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    // Emit domain event
                    domainEventBus.emit(DomainEvent.AppInstalled(packageName))

                    // ✅ NEW: Trigger immediate scan of new app if background service is running
                    triggerImmediateScan(context, packageName)
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    domainEventBus.emit(DomainEvent.AppUninstalled(packageName))
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    domainEventBus.emit(DomainEvent.AppUpdated(packageName, "previous", "new"))

                    // ✅ NEW: Trigger immediate scan of updated app
                    triggerImmediateScan(context, packageName)
                }
            }
        }
    }

    // ✅ NEW: Trigger immediate scan for newly installed/updated apps
    private fun triggerImmediateScan(context: Context, packageName: String) {
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = "SCAN_SPECIFIC_APP"
            putExtra("package_name", packageName)
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            // Handle service start errors
        }
    }
}
