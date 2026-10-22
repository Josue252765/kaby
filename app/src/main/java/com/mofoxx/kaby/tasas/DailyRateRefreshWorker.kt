package com.mofoxx.kaby.tasas

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mofoxx.kaby.KabyApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Performs a daily best-effort check and discards the response; only an interactive proposal can be confirmed. */
class DailyRateRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        (applicationContext as KabyApplication).repository.proposeRate()
        Result.success()
    } catch (cancelled: CancellationException) {
        // WorkManager cancellation is control flow; don't turn it into a retry.
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }
}

object RateRefreshScheduler {
    private const val WORK_NAME = "kaby-daily-rate-proposal"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyRateRefreshWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
