package com.shashluchok.skinwatch.data.storage.pricesync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shashluchok.skinwatch.domain.pricesync.PRICE_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

private const val PRICE_SYNC_WORK_NAME = "price-sync"
private const val PRICE_SYNC_RETRY_WORK_NAME = "price-sync-retry"
private val RETRY_BACKOFF_DELAY = 1.minutes

internal class AndroidPriceSyncScheduler(
    private val context: Context,
) : PriceSyncScheduler {
    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(PRICE_SYNC_INTERVAL.toJavaDuration())
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_DELAY.toJavaDuration())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PRICE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun scheduleRetrySync() {
        val request = OneTimeWorkRequestBuilder<PriceSyncWorker>()
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_DELAY.toJavaDuration())
            .build()
        // KEEP, not REPLACE: several items failing in a row should coalesce into the one pending
        // retry rather than each restarting the backoff clock.
        WorkManager.getInstance(context).enqueueUniqueWork(
            PRICE_SYNC_RETRY_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** The constraint that makes WorkManager itself wait for connectivity and run once it returns. */
    private fun connectedConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}
