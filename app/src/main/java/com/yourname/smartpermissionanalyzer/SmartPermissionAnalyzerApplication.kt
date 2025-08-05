package com.yourname.smartpermissionanalyzer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmartPermissionAnalyzerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // ✅ FIXED: Create application scope for background work
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        println("Argus Application started successfully")

        // ✅ FIXED: Move heavy operations to background thread
        applicationScope.launch {
            initializeAppAsync()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    // ✅ FIXED: Async initialization to prevent blocking
    private suspend fun initializeAppAsync() {
        try {
            // Create notification channels in background
            createNotificationChannels()
            println("Argus: Notification channels created")

            // Any other heavy initialization can go here
            println("Argus: Background initialization complete")
        } catch (e: Exception) {
            println("Argus: Background initialization error: ${e.message}")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val securityChannel = NotificationChannel(
                "security_scan_channel",
                "Security Scanning",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of security scans"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                "security_alerts_channel",
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important security alerts and notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(securityChannel)
            notificationManager?.createNotificationChannel(alertsChannel)
        }
    }
}
