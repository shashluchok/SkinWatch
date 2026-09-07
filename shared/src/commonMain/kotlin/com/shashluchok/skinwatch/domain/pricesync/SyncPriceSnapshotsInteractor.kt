package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.domain.synclog.error
import com.shashluchok.skinwatch.domain.synclog.info
import com.shashluchok.skinwatch.domain.synclog.warn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * How many failures in a row, with nothing succeeding anywhere in the run, mean the problem belongs
 * to the run's own conditions rather than to the items it is walking.
 */
private const val SYSTEMIC_FAILURE_THRESHOLD = 3

/**
 * The single implementation of "fetch a fresh price for every distinct marketHashName in the
 * inventory and record a snapshot" -- called by every trigger (platform schedulers, the app-open
 * staleness check, and the manual "sync now" action), never duplicated per trigger.
 */
internal class SyncPriceSnapshotsInteractor(
    private val inventoryRepository: InventoryRepository,
    private val steamMarketRepository: SteamMarketRepository,
    private val priceSnapshotRepository: PriceSnapshotRepository,
    private val resolveDisplayCurrency: ResolveDisplayCurrencyInteractor,
    private val priceSyncStatusRepository: PriceSyncStatusRepository,
    private val itemSyncStatusRepository: ItemSyncStatusRepository,
    private val syncLog: SyncLogRepository = SyncLogRepository.EMPTY,
) {
    private val runMutex = Mutex()
    private val mutableIsSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = mutableIsSyncing.asStateFlow()

    // Every run is wrapped so an unexpected throw is recorded rather than only surfacing as a
    // missing price -- diagnosing that from the outside is precisely what this log exists to avoid.
    @Suppress("TooGenericExceptionCaught")
    // No default for [trigger] on purpose: MANUAL now waives the per-item backoff, so a caller that
    // forgot to say who it is would quietly get the one behaviour reserved for a person asking.
    suspend operator fun invoke(trigger: SyncTrigger): PriceSyncOutcome {
        syncLog.info(tag = SyncLogTag.TRIGGER, message = "$trigger asked for a run")
        // A run is already in progress -- a second call is a no-op, not a queued retry: it would
        // just re-sync what the active run is already about to finish syncing.
        if (!runMutex.tryLock()) {
            syncLog.warn(
                tag = SyncLogTag.TRIGGER,
                message = "$trigger got no run: another run already holds the lock, reporting AlreadyRunning",
            )
            return PriceSyncOutcome.AlreadyRunning
        }
        return try {
            runSync(trigger = trigger)
        } catch (throwable: Throwable) {
            syncLog.error(
                tag = SyncLogTag.RUN,
                message = "$trigger threw ${throwable::class.simpleName}: ${throwable.message}",
            )
            throw throwable
        } finally {
            mutableIsSyncing.value = false
            runMutex.unlock()
        }
    }

    private suspend fun runSync(trigger: SyncTrigger): PriceSyncOutcome {
        val marketHashNames = inventoryRepository.getDistinctMarketHashNames()
        syncLog.info(
            tag = SyncLogTag.RUN,
            message = "$trigger started, inventory holds ${marketHashNames.size} distinct name(s)",
        )
        // Nothing tracked at all
        if (marketHashNames.isEmpty()) {
            syncLog.info(tag = SyncLogTag.RUN, message = "$trigger finished: nothing tracked, NothingDue")
            return PriceSyncOutcome.NothingDue
        }
        // One shared capturedAt for the whole run, matching PriceSnapshotRepository.record's
        // existing contract for a batch of snapshots taken together.
        val capturedAt = Clock.System.now()
        // Someone watching the screen and asking for a sync is a better reason to spend a request
        // than any backoff is to withhold one -- it is the only way to force a written-off item.
        val due = dueForSync(
            marketHashNames = marketHashNames,
            now = capturedAt,
            ignoreFailureBackoff = trigger == SyncTrigger.MANUAL,
        )
        val tally = syncAll(due = due, capturedAt = capturedAt)

        // Only a run with nothing left to fix advances this. Items Steam cannot price are excluded
        // on purpose: they fail identically on every attempt, so counting them would freeze the
        // timestamp forever and keep the staleness check firing on every single app open.
        if (tally.isComplete) {
            priceSyncStatusRepository.markCompleted(capturedAt)
            syncLog.info(
                tag = SyncLogTag.RUN,
                message = "$trigger marked the whole sync completed at $capturedAt" +
                    if (tally.writtenOffFailures > 0) {
                        ", despite ${tally.writtenOffFailures} item(s) that have been failing for a while"
                    } else {
                        ""
                    },
            )
        } else {
            syncLog.warn(
                tag = SyncLogTag.RUN,
                message = "$trigger left the sync incomplete: ${tally.blockingFailures} item(s) failed " +
                    "recently enough to be worth another attempt, lastCompletedAt not advanced",
            )
        }

        val outcome = when {
            !tally.isComplete -> PriceSyncOutcome.HadFailures
            due.isEmpty() -> PriceSyncOutcome.NothingDue
            else -> PriceSyncOutcome.Completed
        }
        syncLog.info(tag = SyncLogTag.RUN, message = "$trigger finished with $outcome")
        return outcome
    }

    /** Nothing due succeeds trivially: every price is already fresh, so the pass is a finished one. */
    private suspend fun syncAll(due: List<String>, capturedAt: Instant): RunTally {
        if (due.isEmpty()) {
            syncLog.info(tag = SyncLogTag.RUN, message = "nothing was due, treating the pass as complete")
            return RunTally()
        }
        mutableIsSyncing.value = true
        val currency = resolveDisplayCurrency()
        syncLog.info(tag = SyncLogTag.RUN, message = "fetching ${due.size} item(s) in $currency")
        var tally = RunTally()

        for ((index, marketHashName) in due.withIndex()) {
            syncLog.info(
                tag = SyncLogTag.ITEM,
                message = "requesting item ${index + 1} of ${due.size}",
                marketHashName = marketHashName,
            )
            val result = syncItem(marketHashName = marketHashName, currency = currency, capturedAt = capturedAt)
            tally = tally.plus(result)

            val abortReason = tally.abortReason()
            if (abortReason != null) {
                syncLog.warn(
                    tag = SyncLogTag.RUN,
                    message = "aborting after ${index + 1} of ${due.size}: $abortReason, " +
                        "${due.size - index - 1} item(s) left untried and still due",
                )
                break
            }
        }

        return tally
    }

    /**
     * Items whose price is actually old enough to be worth a request, per [isDueAt].
     *
     * Without this, one failing item turning the run into a `Result.retry()` would re-request every
     * other item too, and repeating that under backoff is a direct route to
     * [com.shashluchok.skinwatch.domain.steam.SteamMarketError.RateLimited] -- whose failures would
     * schedule yet another retry.
     */
    private suspend fun dueForSync(
        marketHashNames: List<String>,
        now: Instant,
        ignoreFailureBackoff: Boolean,
    ): List<String> {
        val statuses = itemSyncStatusRepository.getAll()

        val due = marketHashNames.filter { marketHashName ->
            val status = statuses[marketHashName]
            val isDue = status.isDueAt(now = now, ignoreFailureBackoff = ignoreFailureBackoff)
            syncLog.info(
                tag = SyncLogTag.DUE,
                message = buildString {
                    append(if (isDue) "due" else "skipped")
                    append(": lastSuccessAt=")
                    append(status?.lastSuccessAt?.let { "$it (${now - it} ago)" } ?: "never")
                    append(", lastAttemptAt=")
                    append(status?.attemptedAt?.toString() ?: "never")
                    val failure = status as? ItemSyncStatus.Failed
                    if (failure != null) {
                        append(", lastError=${failure.error}")
                        append(", failuresInARow=${failure.consecutiveFailures}")
                        append(", retryAfter=${failure.retryDelay}")
                    }
                    if (ignoreFailureBackoff) append(", manual run ignoring any backoff")
                },
                marketHashName = marketHashName,
            )
            isDue
        }
        syncLog.info(
            tag = SyncLogTag.DUE,
            message = "${due.size} of ${marketHashNames.size} item(s) are due against a $PRICE_SYNC_INTERVAL interval",
        )

        return due
    }

    /** One dead item must not abort the run -- only a refusal that would apply to all of them does. */
    private suspend fun syncItem(
        marketHashName: String,
        currency: SteamCurrency,
        capturedAt: Instant,
    ): ItemSyncResult {
        val startMark = TimeSource.Monotonic.markNow()
        val overview = steamMarketRepository.getPriceOverview(
            marketHashName = marketHashName,
            currency = currency,
        )
        val elapsed = startMark.elapsedNow()
        val priceOverview = (overview as? SteamMarketResult.Success)?.data

        val result = if (priceOverview == null) {
            recordItemFailure(
                marketHashName = marketHashName,
                error = (overview as SteamMarketResult.Failure).error,
                elapsed = elapsed.toString(),
            )
        } else {
            priceSnapshotRepository.record(
                marketHashName = marketHashName,
                overview = priceOverview,
                currency = currency,
                capturedAt = capturedAt,
            )
            itemSyncStatusRepository.markSynced(marketHashName = marketHashName, at = capturedAt)
            syncLog.info(
                tag = SyncLogTag.ITEM,
                message = "fetched in $elapsed, snapshot recorded at $capturedAt " +
                    "(lowest=${priceOverview.lowestPrice}, median=${priceOverview.medianPrice}, " +
                    "volume=${priceOverview.volume})",
                marketHashName = marketHashName,
            )
            ItemSyncResult.Synced
        }
        priceSnapshotRepository.compactHistory(marketHashName = marketHashName, now = capturedAt)

        return result
    }

    private suspend fun recordItemFailure(
        marketHashName: String,
        error: SteamMarketError,
        elapsed: String,
    ): ItemSyncResult {
        // The real moment of this attempt, not the run's shared capturedAt: a run walks its items
        // minutes apart, and the backoff is measured from when the item actually failed.
        val failed = itemSyncStatusRepository.markFailed(
            marketHashName = marketHashName,
            error = error,
            at = Clock.System.now(),
        )
        syncLog.error(
            tag = SyncLogTag.ITEM,
            message = "fetch failed after $elapsed with $error (${failed.consecutiveFailures} in a row), " +
                "next attempt no sooner than ${failed.retryDelay} from now",
            marketHashName = marketHashName,
        )

        return when {
            error == SteamMarketError.RateLimited -> ItemSyncResult.RateLimited
            failed.isWrittenOff -> ItemSyncResult.WrittenOff(error)
            else -> ItemSyncResult.NeedsAnotherAttempt(error)
        }
    }
}

private sealed interface ItemSyncResult {
    data object Synced : ItemSyncResult

    sealed interface Failure : ItemSyncResult {
        val error: SteamMarketError
    }

    /** Failed recently enough that another attempt is still owed -- the run is not finished. */
    data class NeedsAnotherAttempt(
        override val error: SteamMarketError,
    ) : Failure

    /**
     * Failed its way to the end of its backoff curve. Still retried, just rarely, and no longer
     * holding the run back: waiting on it would freeze the last-completed timestamp forever.
     */
    data class WrittenOff(
        override val error: SteamMarketError,
    ) : Failure

    /** Steam is rejecting on request volume, so nothing else in this run could succeed either. */
    data object RateLimited : Failure {
        override val error: SteamMarketError = SteamMarketError.RateLimited
    }
}

/** What a run has to show for itself -- see [PriceSyncOutcome.Completed] for what counts as done. */
private data class RunTally(
    val succeeded: Int = 0,
    val blockingFailures: Int = 0,
    val writtenOffFailures: Int = 0,
    val rateLimited: Boolean = false,
    val repeatedError: SteamMarketError? = null,
    val repeatedErrorRun: Int = 0,
) {
    val isComplete: Boolean get() = blockingFailures == 0

    fun plus(result: ItemSyncResult): RunTally {
        val counted = when (result) {
            ItemSyncResult.Synced -> copy(succeeded = succeeded + 1)
            ItemSyncResult.RateLimited -> copy(blockingFailures = blockingFailures + 1, rateLimited = true)
            is ItemSyncResult.WrittenOff -> copy(writtenOffFailures = writtenOffFailures + 1)
            is ItemSyncResult.NeedsAnotherAttempt -> copy(blockingFailures = blockingFailures + 1)
        }
        val error = (result as? ItemSyncResult.Failure)?.error

        return when {
            error == null -> counted.copy(repeatedError = null, repeatedErrorRun = 0)
            error == repeatedError -> counted.copy(repeatedErrorRun = repeatedErrorRun + 1)
            else -> counted.copy(repeatedError = error, repeatedErrorRun = 1)
        }
    }

    /**
     * Why this run should stop early, or `null` to carry on.
     *
     * Beyond Steam refusing on volume, the same error landing on item after item with nothing
     * succeeding is the shape of a problem that belongs to the run rather than to the items it is
     * walking -- no working network, or something Steam has started returning for everything.
     * Marching through the rest only records a failure against every item in the inventory for
     * something none of them did.
     */
    fun abortReason(): String? = when {
        rateLimited -> "Steam is refusing on request volume"
        succeeded == 0 && repeatedErrorRun >= SYSTEMIC_FAILURE_THRESHOLD ->
            "$repeatedErrorRun item(s) in a row failed with $repeatedError and nothing has succeeded, " +
                "so this looks like the run's own conditions rather than the items"

        else -> null
    }
}
