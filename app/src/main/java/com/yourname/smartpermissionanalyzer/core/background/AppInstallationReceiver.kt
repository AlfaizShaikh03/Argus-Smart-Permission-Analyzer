package com.yourname.smartpermissionanalyzer.core.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.yourname.smartpermissionanalyzer.domain.events.DomainEventBus
import com.yourname.smartpermissionanalyzer.domain.events.DomainEvent
import dagger.hilt.android.EntryPointAccessors
import com.yourname.smartpermissionanalyzer.di.AppInstallationReceiverEntryPoint

class AppInstallationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        // Get dependencies using EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppInstallationReceiverEntryPoint::class.java
        )

        val eventBus = entryPoint.eventBus()
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!replacing) {
                            // Use emit() not publish()
                            eventBus.emit(DomainEvent.AppInstalled(packageName))
                        } else {
                            eventBus.emit(DomainEvent.AppUpdated(packageName, "", ""))
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!replacing) {
                            eventBus.emit(DomainEvent.AppUninstalled(packageName))
                        }
                    }
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        eventBus.emit(DomainEvent.AppUpdated(packageName, "", ""))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
                priority = 1000
            }
        }
    }
}
