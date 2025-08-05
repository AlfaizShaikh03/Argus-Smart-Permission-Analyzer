package com.yourname.smartpermissionanalyzer.domain.scheduler

import android.content.Context
import androidx.work.*
import com.yourname.smartpermissionanalyzer.domain.worker.PeriodicScanWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicScan() {
        val workRequest = PeriodicScanWorker.createWorkRequest()

        workManager.enqueueUniquePeriodicWork(
            PeriodicScanWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelPeriodicScan() {
        workManager.cancelUniqueWork(PeriodicScanWorker.WORK_NAME)
    }

    fun getWorkInfo() = workManager.getWorkInfosForUniqueWorkLiveData(
        PeriodicScanWorker.WORK_NAME
    )
}
