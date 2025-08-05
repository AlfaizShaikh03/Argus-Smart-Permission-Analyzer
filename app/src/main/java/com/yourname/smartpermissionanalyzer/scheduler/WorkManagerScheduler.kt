package com.yourname.smartpermissionanalyzer.scheduler

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.yourname.smartpermissionanalyzer.services.BackgroundScanService

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicScanning(intervalHours: Long = 24) {
        schedulePeriodicScan(intervalHours)
    }

    fun schedulePeriodicScan(intervalHours: Long = 24) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            PeriodicScanWorker::class.java,
            intervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_SCAN_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SCAN_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }

    fun cancelPeriodicScanning() {
        cancelPeriodicScan()
    }

    fun cancelPeriodicScan() {
        workManager.cancelUniqueWork(PERIODIC_SCAN_WORK_NAME)
    }

    companion object {
        private const val PERIODIC_SCAN_WORK_NAME = "periodic_scan_work"
        private const val PERIODIC_SCAN_TAG = "periodic_scan"
    }
}

class PeriodicScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // ✅ FIXED: Use new BackgroundScanService instead of BackgroundMonitoringService
            BackgroundScanService.startService(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
