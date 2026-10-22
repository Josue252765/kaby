package com.mofoxx.kaby.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val DAILY_NAME = "kaby-private-daily-backup"
    private const val OPEN_NAME = "kaby-private-open-backup"
    private const val BACKGROUND_NAME = "kaby-private-background-backup"

    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork(
            DAILY_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS).build(),
        )
        manager.enqueueUniqueWork(
            OPEN_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DailyBackupWorker>().build(),
        )
    }

    fun scheduleAfterBackground(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            BACKGROUND_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DailyBackupWorker>().build(),
        )
    }
}
