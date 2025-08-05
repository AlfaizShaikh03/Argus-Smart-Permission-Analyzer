package com.yourname.smartpermissionanalyzer.core.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.yourname.smartpermissionanalyzer.domain.usecase.PerformPeriodicScanUseCase
import com.yourname.smartpermissionanalyzer.domain.events.DomainEventBus
import com.yourname.smartpermissionanalyzer.domain.events.DomainEvent

@AndroidEntryPoint
class BackgroundMonitoringService : Service() {

    @Inject
    lateinit var performPeriodicScanUseCase: PerformPeriodicScanUseCase

    @Inject
    lateinit var domainEventBus: DomainEventBus

    private val notificationId = 1
    private val channelId = "permission_analyzer_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service with notification
        startForeground(notificationId, createNotification())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ FIXED: Use emit() instead of publish()
                domainEventBus.emit(DomainEvent.PeriodicScanStarted)

                performPeriodicScanUseCase.execute().fold(
                    onSuccess = { apps ->
                        // ✅ FIXED: Use emit() instead of publish()
                        domainEventBus.emit(DomainEvent.ScanCompleted(apps))
                    },
                    onFailure = { error ->
                        // ✅ FIXED: Use emit() instead of publish()
                        domainEventBus.emit(DomainEvent.ScanFailed(error.message ?: "Scan failed"))
                    }
                )
            } catch (e: Exception) {
                // ✅ FIXED: Use emit() instead of publish()
                domainEventBus.emit(DomainEvent.ScanFailed(e.message ?: "Service error"))
            } finally {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Permission Analyzer Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background permission scanning service"
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Permission Analyzer")
            .setContentText("Scanning apps for security risks...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSound(null)
            .build()
    }
}
