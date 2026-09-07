package com.shashluchok.skinwatch.data.storage.pricesync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncOutcome
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsInteractor
import com.shashluchok.skinwatch.domain.pricesync.SyncTrigger
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.domain.synclog.error
import com.shashluchok.skinwatch.domain.synclog.info
import com.shashluchok.skinwatch.domain.synclog.warn
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * How many times a run may ask to be retried before the chain is given up on.
 *
 * WorkManager doubles its backoff up to a five-hour ceiling and never stops on its own, so an
 * unbounded chain ends up hours away from running while still occupying the unique work slot. Past
 * this point the periodic run is the better place to pick the work up.
 */
private const val MAX_RETRY_ATTEMPTS = 5

internal class PriceSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    // The attempt count is the number that explains a retry chain that has backed off out of reach,
    // so an unexpected throw has to be recorded alongside it rather than only reaching Logcat.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        val syncLog = get<SyncLogRepository>()
        val trigger = inputData
            .getString(PRICE_SYNC_TRIGGER_KEY)
            ?.let { name -> SyncTrigger.entries.firstOrNull { it.name == name } }
            ?: SyncTrigger.PERIODIC_WORKER
        val attempt = runAttemptCount + 1
        syncLog.info(tag = SyncLogTag.WORKER, message = "$trigger started, attempt #$attempt (id=$id)")

        val outcome = try {
            get<SyncPriceSnapshotsInteractor>().invoke(trigger = trigger)
        } catch (throwable: Throwable) {
            syncLog.error(
                tag = SyncLogTag.WORKER,
                message = "$trigger threw ${throwable::class.simpleName} on attempt " +
                    "#$attempt: ${throwable.message}, isStopped=$isStopped",
            )
            throw throwable
        }

        val result = resultFor(outcome = outcome, attempt = attempt, syncLog = syncLog)
        syncLog.info(
            tag = SyncLogTag.WORKER,
            message = "$trigger got $outcome on attempt #$attempt, " +
                "reporting ${result::class.simpleName}, isStopped=$isStopped",
        )
        return result
    }

    /**
     * [PriceSyncOutcome.AlreadyRunning] asks to come back rather than reporting success: the run
     * holding the lock can still abort partway through, and reporting success here would retire the
     * only pending request that would have collected what it left behind.
     */
    private fun resultFor(outcome: PriceSyncOutcome, attempt: Int, syncLog: SyncLogRepository): Result {
        val wantsRetry = when (outcome) {
            PriceSyncOutcome.HadFailures, PriceSyncOutcome.AlreadyRunning -> true
            PriceSyncOutcome.Completed, PriceSyncOutcome.NothingDue -> false
        }
        val chainExhausted = attempt >= MAX_RETRY_ATTEMPTS
        if (wantsRetry && chainExhausted) {
            syncLog.warn(
                tag = SyncLogTag.WORKER,
                message = "giving up this chain after $attempt attempt(s) rather than backing off " +
                    "further -- the periodic run picks the work up from here",
            )
        }

        return if (wantsRetry && !chainExhausted) Result.retry() else Result.success()
    }
}
