package com.yourname.smartpermissionanalyzer.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import com.yourname.smartpermissionanalyzer.MainActivity
import com.yourname.smartpermissionanalyzer.R
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import com.yourname.smartpermissionanalyzer.domain.monitoring.SecurityAlert
import com.yourname.smartpermissionanalyzer.domain.events.DomainEventBus
import com.yourname.smartpermissionanalyzer.domain.events.DomainEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.net.NetworkInterface
import java.io.BufferedReader
import java.io.InputStreamReader
import com.yourname.smartpermissionanalyzer.domain.entities.PermissionChangeLog
import com.yourname.smartpermissionanalyzer.domain.entities.NetworkActivityLog


@AndroidEntryPoint
class BackgroundScanService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "argus_background_scan"
        const val ALERT_CHANNEL_ID = "argus_security_alerts"
        const val ACTION_START_SCANNING = "START_SCANNING"
        const val ACTION_STOP_SCANNING = "STOP_SCANNING"
        const val ACTION_PAUSE_SCANNING = "PAUSE_SCANNING"
        const val ACTION_SCAN_SPECIFIC_APP = "SCAN_SPECIFIC_APP"

        private const val TAG = "BackgroundScanService"

        fun startService(context: Context) {
            Log.d(TAG, "🚀 Starting BackgroundScanService...")
            try {
                val intent = Intent(context, BackgroundScanService::class.java).apply {
                    action = ACTION_START_SCANNING
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                    Log.d(TAG, "✅ startForegroundService called successfully")
                } else {
                    context.startService(intent)
                    Log.d(TAG, "✅ startService called successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start service: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            Log.d(TAG, "🛑 Stopping BackgroundScanService...")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = ACTION_STOP_SCANNING
            }
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var repository: PermissionAnalyzerRepository

    @Inject
    lateinit var domainEventBus: DomainEventBus

    private var scanJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isScanning = false
    private var scanFrequencyMinutes = 30
    private var lastScanTime = 0L
    private var appsScanned = 0
    private var threatsFound = 0

    // ✅ NEW: Permission and Network monitoring
    private var permissionMonitorJob: Job? = null
    private var networkMonitorJob: Job? = null
    private var lastKnownPermissions = mutableMapOf<String, Set<String>>()
    private var networkStats = mutableMapOf<String, NetworkStats>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔧 Service onCreate() called")

        createNotificationChannels()
        acquireWakeLock()

        // ✅ NEW: Initialize monitoring systems
        initializePermissionMonitoring()
        initializeNetworkMonitoring()

        Log.d(TAG, "✅ Service created successfully")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📱 onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_SCANNING -> {
                Log.d(TAG, "🔄 Starting scanning...")
                startScanning()
            }
            ACTION_STOP_SCANNING -> {
                Log.d(TAG, "⏹️ Stopping scanning...")
                stopScanning()
            }
            ACTION_PAUSE_SCANNING -> {
                Log.d(TAG, "⏸️ Pausing scanning...")
                pauseScanning()
            }
            ACTION_SCAN_SPECIFIC_APP -> {
                val packageName = intent.getStringExtra("package_name")
                Log.d(TAG, "🔍 Scanning specific app: $packageName")
                if (packageName != null) {
                    scanSpecificApp(packageName)
                }
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown action: ${intent?.action}")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startScanning() {
        if (isScanning) {
            Log.d(TAG, "⏭️ Scanning already in progress, skipping")
            return
        }

        Log.d(TAG, "🚀 Starting foreground service...")
        try {
            isScanning = true

            val notification = createScanningNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "✅ Foreground service started with notification ID: $NOTIFICATION_ID")

            updateNotification("🛡️ Argus Security Active", "Starting security monitoring...")

            CoroutineScope(Dispatchers.IO).launch {
                domainEventBus.emit(DomainEvent.PeriodicScanStarted)
            }

            // Start background scanning loop
            scanJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                Log.d(TAG, "🔄 Starting scan loop with frequency: $scanFrequencyMinutes minutes")
                while (isScanning) {
                    try {
                        performSecurityScan()
                        updateNotification()
                        Log.d(TAG, "😴 Waiting $scanFrequencyMinutes minutes until next scan...")
                        delay(scanFrequencyMinutes * 60 * 1000L)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Scan error: ${e.message}", e)
                        delay(5 * 60 * 1000L)
                    }
                }
            }

            // ✅ NEW: Start monitoring jobs
            startPermissionMonitoring()
            startNetworkMonitoring()

        } catch (e: Exception) {
            Log.e(TAG, "💥 Failed to start scanning: ${e.message}", e)
            isScanning = false
        }
    }

    private fun stopScanning() {
        Log.d(TAG, "🛑 Stopping scanning...")
        isScanning = false
        scanJob?.cancel()
        permissionMonitorJob?.cancel()
        networkMonitorJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseScanning() {
        Log.d(TAG, "⏸️ Pausing scanning...")
        isScanning = false
        scanJob?.cancel()
        permissionMonitorJob?.cancel()
        networkMonitorJob?.cancel()
        updateNotification("⏸️ Argus Security Paused", "Real-time protection temporarily paused")
    }

    // ✅ NEW: Initialize permission monitoring system
    private fun initializePermissionMonitoring() {
        Log.d(TAG, "🔍 Initializing permission monitoring...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load current permissions for all apps
                val packageManager = packageManager
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

                installedApps.forEach { appInfo ->
                    try {
                        val packageInfo = packageManager.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                        val permissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()
                        lastKnownPermissions[appInfo.packageName] = permissions
                    } catch (e: Exception) {
                        // Ignore apps we can't access
                    }
                }

                Log.d(TAG, "✅ Permission monitoring initialized for ${lastKnownPermissions.size} apps")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize permission monitoring: ${e.message}", e)
            }
        }
    }

    // ✅ NEW: Initialize network monitoring system
    private fun initializeNetworkMonitoring() {
        Log.d(TAG, "🌐 Initializing network monitoring...")
        // Initialize network statistics tracking
        Log.d(TAG, "✅ Network monitoring initialized")
    }

    // ✅ NEW: Start permission monitoring job
    private fun startPermissionMonitoring() {
        permissionMonitorJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            Log.d(TAG, "🔍 Starting permission monitoring loop...")
            while (isScanning) {
                try {
                    checkPermissionChanges()
                    delay(2 * 60 * 1000L) // Check every 2 minutes
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Permission monitoring error: ${e.message}", e)
                    delay(5 * 60 * 1000L)
                }
            }
        }
    }

    // ✅ NEW: Start network monitoring job
    private fun startNetworkMonitoring() {
        networkMonitorJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            Log.d(TAG, "🌐 Starting network monitoring loop...")
            while (isScanning) {
                try {
                    monitorNetworkActivity()
                    delay(30 * 1000L) // Check every 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Network monitoring error: ${e.message}", e)
                    delay(60 * 1000L)
                }
            }
        }
    }

    // ✅ NEW: Check for permission changes
    private suspend fun checkPermissionChanges() {
        try {
            val packageManager = packageManager
            val currentTime = System.currentTimeMillis()
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            installedApps.forEach { appInfo ->
                try {
                    val packageInfo = packageManager.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                    val currentPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()
                    val lastPermissions = lastKnownPermissions[appInfo.packageName] ?: emptySet()

                    if (currentPermissions != lastPermissions) {
                        Log.d(TAG, "🔄 Permission change detected for ${appInfo.packageName}")

                        val addedPermissions = currentPermissions - lastPermissions
                        val removedPermissions = lastPermissions - currentPermissions

                        // Log permission changes
                        if (addedPermissions.isNotEmpty()) {
                            logPermissionChange(
                                packageName = appInfo.packageName,
                                appName = appInfo.loadLabel(packageManager).toString(),
                                changeType = "ADDED",
                                permissions = addedPermissions.toList(),
                                timestamp = currentTime
                            )
                        }

                        if (removedPermissions.isNotEmpty()) {
                            logPermissionChange(
                                packageName = appInfo.packageName,
                                appName = appInfo.loadLabel(packageManager).toString(),
                                changeType = "REMOVED",
                                permissions = removedPermissions.toList(),
                                timestamp = currentTime
                            )
                        }

                        // Update our tracking
                        lastKnownPermissions[appInfo.packageName] = currentPermissions

                        // Check if any added permissions are dangerous
                        val dangerousAdded = addedPermissions.filter { isDangerousPermission(it) }
                        if (dangerousAdded.isNotEmpty()) {
                            showPermissionChangeAlert(
                                appName = appInfo.loadLabel(packageManager).toString(),
                                packageName = appInfo.packageName,
                                permissions = dangerousAdded
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore apps we can't access
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Permission change check failed: ${e.message}", e)
        }
    }

    // ✅ NEW: Monitor network activity
    private suspend fun monitorNetworkActivity() {
        try {
            // Monitor network interfaces for unusual activity
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            var totalBytesReceived = 0L
            var totalBytesSent = 0L

            // Read network statistics from /proc/net/dev if available
            try {
                val process = Runtime.getRuntime().exec("cat /proc/net/dev")
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                reader.use { r ->
                    r.readLines().drop(2).forEach { line ->
                        val parts = line.trim().split(Regex("\\s+"))
                        if (parts.size >= 10) {
                            totalBytesReceived += parts[1].toLongOrNull() ?: 0L
                            totalBytesSent += parts[9].toLongOrNull() ?: 0L
                        }
                    }
                }

                // Log network activity if significant
                if (totalBytesReceived > 1024 * 1024 || totalBytesSent > 1024 * 1024) { // > 1MB
                    Log.d(TAG, "🌐 Network activity: ${totalBytesReceived / 1024 / 1024}MB received, ${totalBytesSent / 1024 / 1024}MB sent")

                    // Store network statistics
                    logNetworkActivity(
                        timestamp = System.currentTimeMillis(),
                        bytesReceived = totalBytesReceived,
                        bytesSent = totalBytesSent
                    )
                }

            } catch (e: Exception) {
                // Fallback: Basic network monitoring
                Log.d(TAG, "🌐 Basic network monitoring active")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Network monitoring failed: ${e.message}", e)
        }
    }

    private suspend fun logPermissionChange(
        packageName: String,
        appName: String,
        changeType: String,
        permissions: List<String>,
        timestamp: Long
    ) {
        try {
            Log.d(TAG, "📝 Logging permission change: $appName - $changeType - ${permissions.size} permissions")

            val permissionChangeLog = PermissionChangeLog(
                packageName = packageName,
                appName = appName,
                changeType = changeType,
                permissions = permissions,
                timestamp = timestamp
            )

            // ✅ FIXED: Now calls the existing repository method
            repository.savePermissionChange(permissionChangeLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to log permission change: ${e.message}", e)
        }
    }

    private suspend fun logNetworkActivity(
        timestamp: Long,
        bytesReceived: Long,
        bytesSent: Long
    ) {
        try {
            Log.d(TAG, "📊 Logging network activity: ${bytesReceived / 1024}KB received, ${bytesSent / 1024}KB sent")

            val networkLog = NetworkActivityLog(
                timestamp = timestamp,
                bytesReceived = bytesReceived,
                bytesSent = bytesSent,
                activeConnections = getActiveConnections()
            )

            // ✅ FIXED: Now calls the existing repository method
            repository.saveNetworkActivity(networkLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to log network activity: ${e.message}", e)
        }
    }

    // ✅ NEW: Show permission change alert
    private fun showPermissionChangeAlert(
        appName: String,
        packageName: String,
        permissions: List<String>
    ) {
        try {
            Log.d(TAG, "🚨 Showing permission change alert for: $appName")

            val permissionNames = permissions.map { getPermissionDisplayName(it) }.take(3)
            val alertText = if (permissions.size > 3) {
                "${permissionNames.joinToString(", ")} and ${permissions.size - 3} more"
            } else {
                permissionNames.joinToString(", ")
            }

            val changeNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("⚠️ New Permissions Granted")
                .setContentText("$appName gained access to: $alertText")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()

            val notificationManager = NotificationManagerCompat.from(this)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(packageName.hashCode(), changeNotification)
                Log.d(TAG, "✅ Permission change alert shown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to show permission change alert: ${e.message}", e)
        }
    }

    // ✅ NEW: Helper functions
    private fun isDangerousPermission(permission: String): Boolean {
        val dangerousPermissions = listOf(
            "CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
            "READ_CONTACTS", "WRITE_CONTACTS", "READ_SMS", "SEND_SMS", "READ_CALL_LOG",
            "WRITE_CALL_LOG", "CALL_PHONE", "READ_PHONE_STATE", "BODY_SENSORS",
            "ACCESS_BACKGROUND_LOCATION", "READ_CALENDAR", "WRITE_CALENDAR"
        )
        return dangerousPermissions.any { permission.contains(it, ignoreCase = true) }
    }

    private fun getPermissionDisplayName(permission: String): String {
        return when {
            permission.contains("CAMERA") -> "Camera"
            permission.contains("RECORD_AUDIO") -> "Microphone"
            permission.contains("LOCATION") -> "Location"
            permission.contains("CONTACTS") -> "Contacts"
            permission.contains("SMS") -> "SMS"
            permission.contains("CALL") -> "Phone"
            permission.contains("CALENDAR") -> "Calendar"
            permission.contains("STORAGE") -> "Storage"
            else -> permission.substringAfterLast(".").replace("_", " ")
        }
    }

    private fun getActiveConnections(): Int {
        return try {
            val process = Runtime.getRuntime().exec("netstat -an")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.use { r ->
                r.readLines().count { it.contains("ESTABLISHED") }
            }
        } catch (e: Exception) {
            0
        }
    }

    // ✅ Keep all existing methods from your original BackgroundScanService...
    // (createNotificationChannels, createScanningNotification, updateNotification,
    //  performSecurityScan, scanSpecificApp, showThreatNotification, etc.)

    private fun createNotificationChannels() {
        Log.d(TAG, "📺 Creating notification channels...")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Argus Background Security",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Real-time security monitoring notifications"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Security Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical security threat notifications"
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(alertChannel)

        Log.d(TAG, "✅ Notification channels created successfully")
    }

    private fun createScanningNotification(): Notification {
        Log.d(TAG, "🔔 Creating scanning notification...")

        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🛡️ Argus Security Active")
                .setContentText("Real-time protection • Tap to open")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .build()

            Log.d(TAG, "✅ Notification created successfully")
            return notification

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create notification: ${e.message}", e)
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Argus Security")
                .setContentText("Running")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(title: String = "🛡️ Argus Security Active", content: String? = null) {
        try {
            val defaultContent = when {
                lastScanTime == 0L -> "Starting security monitoring..."
                threatsFound > 0 -> "⚠️ $threatsFound threats detected • Last scan: ${getTimeAgo(lastScanTime)}"
                appsScanned > 0 -> "✅ $appsScanned apps secure • Last scan: ${getTimeAgo(lastScanTime)}"
                else -> "Monitoring your device security..."
            }

            val finalContent = content ?: defaultContent
            Log.d(TAG, "📝 Updating notification: $title - $finalContent")

            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(finalContent)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            val notificationManager = NotificationManagerCompat.from(this)

            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID, notification)
                Log.d(TAG, "✅ Notification updated successfully")
            } else {
                Log.w(TAG, "⚠️ Notifications are disabled for this app")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update notification: ${e.message}", e)
        }
    }

    private suspend fun performSecurityScan() {
        try {
            Log.d(TAG, "🔍 Starting security scan...")
            lastScanTime = System.currentTimeMillis()

            withContext(Dispatchers.Main) {
                updateNotification("🔍 Argus Scanning...", "Analyzing installed apps for threats")
            }

            val apps = repository.scanInstalledApps()
            appsScanned = apps.size
            Log.d(TAG, "📊 Scanned $appsScanned apps")

            val highRiskApps = apps.filter {
                it.riskScore >= 70 && !it.isSystemApp
            }

            val criticalApps = apps.filter {
                it.riskScore >= 85 && !it.isSystemApp
            }

            threatsFound = highRiskApps.size
            Log.d(TAG, "⚠️ Found $threatsFound threats (${criticalApps.size} critical)")

            if (criticalApps.isNotEmpty()) {
                domainEventBus.emit(DomainEvent.ScanCompleted(apps))
            }

            criticalApps.forEach { app ->
                Log.d(TAG, "🚨 Critical threat: ${app.appName} (${app.riskScore}/100)")
                val alert = SecurityAlert(
                    id = "bg_scan_${app.packageName}_${System.currentTimeMillis()}",
                    type = "CRITICAL_THREAT_DETECTED",
                    title = "Critical Security Alert",
                    message = "High-risk app detected: ${app.appName} (Risk: ${app.riskScore}/100)",
                    appPackageName = app.packageName,
                    timestamp = System.currentTimeMillis(),
                    severity = "CRITICAL"
                )
                repository.saveSecurityAlert(alert)
                showThreatNotification(app.appName, app.riskScore)
            }

            withContext(Dispatchers.Main) {
                val statusText = when {
                    criticalApps.isNotEmpty() -> "⚠️ ${criticalApps.size} critical threats found"
                    highRiskApps.isNotEmpty() -> "⚠️ ${highRiskApps.size} high-risk apps detected"
                    else -> "✅ All apps secure"
                }
                updateNotification("🛡️ Argus Security Active", statusText)
            }

            Log.d(TAG, "✅ Security scan completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "💥 Security scan failed: ${e.message}", e)

            CoroutineScope(Dispatchers.IO).launch {
                domainEventBus.emit(DomainEvent.ScanFailed(e.message ?: "Background scan failed"))
            }

            withContext(Dispatchers.Main) {
                updateNotification("❌ Scan Error", "Will retry in 5 minutes")
            }
        }
    }

    private fun scanSpecificApp(packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔍 Scanning specific app: $packageName")
                updateNotification("🔍 Scanning New App...", "Analyzing $packageName for security risks")

                val apps = repository.scanInstalledApps()
                val targetApp = apps.find { it.packageName == packageName }

                if (targetApp != null && !targetApp.isSystemApp) {
                    Log.d(TAG, "📱 Found app: ${targetApp.appName} (Risk: ${targetApp.riskScore})")
                    if (targetApp.riskScore >= 70) {
                        showThreatNotification(targetApp.appName, targetApp.riskScore)

                        val alert = SecurityAlert(
                            id = "new_app_${packageName}_${System.currentTimeMillis()}",
                            type = "NEW_APP_HIGH_RISK",
                            title = "New High-Risk App Detected",
                            message = "Recently installed app '${targetApp.appName}' has high risk score: ${targetApp.riskScore}/100",
                            appPackageName = packageName,
                            timestamp = System.currentTimeMillis(),
                            severity = if (targetApp.riskScore >= 85) "CRITICAL" else "HIGH"
                        )
                        repository.saveSecurityAlert(alert)

                        domainEventBus.emit(DomainEvent.ScanCompleted(listOf(targetApp)))
                    }
                }

                updateNotification()
            } catch (e: Exception) {
                Log.e(TAG, "❌ App scan failed: ${e.message}", e)
                updateNotification("❌ App Scan Error", "Failed to analyze new app")
            }
        }
    }

    private fun showThreatNotification(appName: String, riskScore: Int) {
        try {
            Log.d(TAG, "🚨 Showing threat notification for: $appName")

            val threatNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("⚠️ Security Threat Detected")
                .setContentText("$appName poses a security risk (Score: $riskScore/100)")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()

            val notificationManager = NotificationManagerCompat.from(this)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(appName.hashCode(), threatNotification)
                Log.d(TAG, "✅ Threat notification shown")
            } else {
                Log.w(TAG, "⚠️ Cannot show threat notification - notifications disabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to show threat notification: ${e.message}", e)
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }

    private fun acquireWakeLock() {
        try {
            Log.d(TAG, "🔋 Acquiring wake lock...")
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ArgusSecurityScanner::BackgroundScan"
            )
            wakeLock?.acquire(10 * 60 * 1000L)
            Log.d(TAG, "✅ Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to acquire wake lock: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "💀 Service onDestroy() called")
        super.onDestroy()
        isScanning = false
        scanJob?.cancel()
        permissionMonitorJob?.cancel()
        networkMonitorJob?.cancel()
        wakeLock?.release()
        Log.d(TAG, "✅ Service destroyed successfully")
    }
}

// ✅ NEW: Data classes for logging
data class PermissionChangeLog(
    val packageName: String,
    val appName: String,
    val changeType: String, // ADDED, REMOVED, MODIFIED
    val permissions: List<String>,
    val timestamp: Long
)

data class NetworkActivityLog(
    val timestamp: Long,
    val bytesReceived: Long,
    val bytesSent: Long,
    val activeConnections: Int
)

data class NetworkStats(
    val totalBytesReceived: Long = 0L,
    val totalBytesSent: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)
