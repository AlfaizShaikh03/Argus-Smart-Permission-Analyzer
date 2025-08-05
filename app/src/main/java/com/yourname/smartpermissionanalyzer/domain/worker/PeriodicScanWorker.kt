package com.yourname.smartpermissionanalyzer.domain.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.yourname.smartpermissionanalyzer.domain.usecase.PerformPeriodicScanUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class PeriodicScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val performPeriodicScanUseCase: PerformPeriodicScanUseCase
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "periodic_scan_worker"
        const val WORK_TAG = "security_scan"

        fun createWorkRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            return PeriodicWorkRequestBuilder<PeriodicScanWorker>(
                15, // Minimum interval is 15 minutes for PeriodicWork
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            // Set foreground service for long-running work
            setForeground(createForegroundInfo())

            // Perform the actual scanning
            performPeriodicScanUseCase.execute().fold(
                onSuccess = {
                    Result.success()
                },
                onFailure = { error ->
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            )
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            "security_scan_channel"
        )
            .setContentTitle("Security Analysis")
            .setContentText("Scanning apps for security risks...")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1, notification)
    }
}
