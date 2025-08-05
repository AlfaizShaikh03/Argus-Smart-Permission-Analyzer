package com.yourname.smartpermissionanalyzer.domain.monitoring

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.yourname.smartpermissionanalyzer.domain.entities.AppEntity
import com.yourname.smartpermissionanalyzer.domain.entities.AppCategoryEntity
import com.yourname.smartpermissionanalyzer.domain.entities.RiskLevelEntity
import com.yourname.smartpermissionanalyzer.domain.repository.PermissionAnalyzerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@HiltWorker
class RealTimeMonitoringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PermissionAnalyzerRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "real_time_monitoring"

        // This creates the monitoring job
        fun createWorkRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<RealTimeMonitoringWorker>(
                15, TimeUnit.MINUTES // Check every 15 minutes
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
        }
    }

    // This runs every 15 minutes in the background
    override suspend fun doWork(): Result {
        return try {
            // Get current apps on the phone
            val currentApps = getCurrentInstalledApps()

            // Get apps we knew about before
            val lastKnownApps = getLastKnownApps()

            // Find new apps that weren't there before
            val newApps = findNewApps(currentApps, lastKnownApps)

            // Check if new apps are dangerous
            newApps.forEach { app ->
                if (isDangerousApp(app)) {
                    sendDangerAlert(app)
                }
            }

            // Save current apps for next time
            saveCurrentApps(currentApps)

            Result.success()
        } catch (e: Exception) {
            // If something goes wrong, try again later
            Result.retry()
        }
    }

    // Get all apps currently installed on phone
    private fun getCurrentInstalledApps(): List<AppEntity> {
        val packageManager = applicationContext.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps.map { appInfo ->
            val permissions = try {
                packageManager.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                    .requestedPermissions?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            AppEntity(
                appName = appInfo.loadLabel(packageManager).toString(),
                packageName = appInfo.packageName,
                permissions = permissions,
                detailedPermissions = permissions,
                riskLevel = RiskLevelEntity.UNKNOWN,
                riskScore = 0,
                appCategory = AppCategoryEntity.UNKNOWN,
                suspiciousPermissions = emptyList(),
                suspiciousPermissionCount = 0,
                criticalPermissionCount = 0,
                installTime = System.currentTimeMillis(),
                lastUpdateTime = System.currentTimeMillis(),
                lastUpdate = System.currentTimeMillis(),
                lastScannedTime = System.currentTimeMillis(),
                lastScan = System.currentTimeMillis(),
                isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                targetSdkVersion = appInfo.targetSdkVersion,
                versionName = "1.0",
                versionCode = 1,
                minSdkVersion = 0,
                permissionDensity = 0f,
                riskFactors = emptyList(),
                permissionChanges = emptyList(),
                appSize = 0L,
                lastUsedTime = 0L,
                isEnabled = appInfo.enabled,
                hasInternetAccess = permissions.contains("android.permission.INTERNET"),
                signatureHash = "",
                trustScore = 0f
            )
        }
    }

    // Get apps we saved before
    private suspend fun getLastKnownApps(): List<AppEntity> {
        return try {
            repository.getAnalysisResults().getOrElse { emptyList() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Find apps that are new
    private fun findNewApps(currentApps: List<AppEntity>, lastKnownApps: List<AppEntity>): List<AppEntity> {
        return currentApps.filter { current ->
            lastKnownApps.none { known -> known.packageName == current.packageName }
        }
    }

    // Check if app is dangerous
    private fun isDangerousApp(app: AppEntity): Boolean {
        val dangerousPermissions = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.CALL_PHONE"
        )

        val dangerousCount = app.permissions.count { it in dangerousPermissions }

        // If app has 3+ dangerous permissions, it's risky
        return dangerousCount >= 3
    }

    // Send alert about dangerous app
    private fun sendDangerAlert(app: AppEntity) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, "security_alerts_channel")
            .setContentTitle("⚠️ Dangerous App Detected!")
            .setContentText("${app.appName} has suspicious permissions")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(app.packageName.hashCode(), notification)
    }

    // Save current apps for next comparison
    private suspend fun saveCurrentApps(apps: List<AppEntity>) {
        // This would save to your database/repository
        // For now, we'll skip this implementation
    }
}
