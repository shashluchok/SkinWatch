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
import androidx.work.workDataOf
import com.shashluchok.skinwatch.domain.pricesync.PRICE_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.SyncTrigger
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.domain.synclog.info
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

private val RETRY_BACKOFF_DELAY = 1.minutes

internal const val PRICE_SYNC_TRIGGER_KEY = "trigger"

internal class AndroidPriceSyncScheduler(
    private val context: Context,
    private val syncLog: SyncLogRepository = SyncLogRepository.EMPTY,
) : PriceSyncScheduler {
    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(PRICE_SYNC_INTERVAL.toJavaDuration())
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_DELAY.toJavaDuration())
            .setInputData(workDataOf(PRICE_SYNC_TRIGGER_KEY to SyncTrigger.PERIODIC_WORKER.name))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PRICE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        syncLog.info(
            tag = SyncLogTag.SCHEDULE,
            message = "enqueued periodic '$PRICE_SYNC_WORK_NAME' every $PRICE_SYNC_INTERVAL under KEEP",
        )
        context.logWorkState(workName = PRICE_SYNC_WORK_NAME, syncLog = syncLog)
    }

    override fun scheduleRetrySync() {
        val request = OneTimeWorkRequestBuilder<PriceSyncWorker>()
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_DELAY.toJavaDuration())
            .setInputData(workDataOf(PRICE_SYNC_TRIGGER_KEY to SyncTrigger.RETRY_WORKER.name))
            .build()
        // REPLACE, not KEEP. Every caller of this has just left an item with no price at all, and
        // under KEEP that request loses to whatever is already pending -- including a chain that has
        // already failed repeatedly and backed off toward WorkManager's five-hour ceiling. Replacing
        // restarts the backoff at RETRY_BACKOFF_DELAY, and the connectivity constraint still holds
        // the work until there is a network, so this cannot turn into a request loop.
        WorkManager.getInstance(context).enqueueUniqueWork(
            PRICE_SYNC_RETRY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        syncLog.info(
            tag = SyncLogTag.SCHEDULE,
            message = "enqueued one-time '$PRICE_SYNC_RETRY_WORK_NAME' under REPLACE, " +
                "backoff restarted at $RETRY_BACKOFF_DELAY",
        )
        context.logWorkState(workName = PRICE_SYNC_RETRY_WORK_NAME, syncLog = syncLog)
    }

    /** The constraint that makes WorkManager itself wait for connectivity and run once it returns. */
    private fun connectedConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}
