package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock
import kotlin.time.Instant

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
) {
    private val runMutex = Mutex()
    private val mutableIsSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = mutableIsSyncing.asStateFlow()

    suspend operator fun invoke(): PriceSyncOutcome {
        // A run is already in progress -- a second call is a no-op, not a queued retry: it would
        // just re-sync what the active run is already about to finish syncing.
        if (!runMutex.tryLock()) return PriceSyncOutcome.Skipped
        return try {
            runSync()
        } finally {
            mutableIsSyncing.value = false
            runMutex.unlock()
        }
    }

    private suspend fun runSync(): PriceSyncOutcome {
        val marketHashNames = inventoryRepository.getDistinctMarketHashNames()
        // Nothing tracked at all
        if (marketHashNames.isEmpty()) return PriceSyncOutcome.Skipped
        // One shared capturedAt for the whole run, matching PriceSnapshotRepository.record's
        // existing contract for a batch of snapshots taken together.
        val capturedAt = Clock.System.now()
        val due = dueForSync(marketHashNames = marketHashNames, now = capturedAt)
        val allSucceeded = syncAll(due = due, capturedAt = capturedAt)

        // Only a clean run advances this: marking a failed run completed would make the staleness
        // check treat the missing prices as fresh for another whole interval.
        if (allSucceeded) priceSyncStatusRepository.markCompleted(capturedAt)

        return if (allSucceeded) PriceSyncOutcome.Completed else PriceSyncOutcome.HadFailures
    }

    /** Nothing due succeeds trivially: every price is already fresh, so the pass is a finished one. */
    private suspend fun syncAll(due: List<String>, capturedAt: Instant): Boolean {
        if (due.isEmpty()) return true
        mutableIsSyncing.value = true
        val currency = resolveDisplayCurrency()
        var allSucceeded = true

        for (marketHashName in due) {
            val result = syncItem(marketHashName = marketHashName, currency = currency, capturedAt = capturedAt)
            if (result != ItemSyncResult.Synced) allSucceeded = false
            // Once Steam is refusing on request volume, every further request this run is refused
            // too. The untried items record no status and stay due, so the worker's backed-off
            // retry collects them instead of this run spending the quota proving the point.
            if (result == ItemSyncResult.RateLimited) break
        }

        return allSucceeded
    }

    /**
     * Items whose price is actually old enough to be worth a request.
     *
     * Without this, one failing item turning the run into a `Result.retry()` would re-request every
     * other item too, and repeating that under backoff is a direct route to
     * [com.shashluchok.skinwatch.domain.steam.SteamMarketError.RateLimited] -- whose failures would
     * schedule yet another retry.
     */
    private suspend fun dueForSync(marketHashNames: List<String>, now: Instant): List<String> {
        val statuses = itemSyncStatusRepository.getAll()

        return marketHashNames.filter { marketHashName ->
            val lastSuccessAt = statuses[marketHashName]?.lastSuccessAt
            lastSuccessAt == null || now - lastSuccessAt >= PRICE_SYNC_INTERVAL
        }
    }

    /** One dead item must not abort the run -- only a refusal that would apply to all of them does. */
    private suspend fun syncItem(
        marketHashName: String,
        currency: SteamCurrency,
        capturedAt: Instant,
    ): ItemSyncResult {
        val overview = steamMarketRepository.getPriceOverview(
            marketHashName = marketHashName,
            currency = currency,
        )
        val priceOverview = (overview as? SteamMarketResult.Success)?.data
        if (priceOverview == null) {
            itemSyncStatusRepository.markFailed(
                marketHashName = marketHashName,
                error = (overview as SteamMarketResult.Failure).error,
                at = capturedAt,
            )
        } else {
            priceSnapshotRepository.record(
                marketHashName = marketHashName,
                overview = priceOverview,
                currency = currency,
                capturedAt = capturedAt,
            )
            itemSyncStatusRepository.markSynced(marketHashName = marketHashName, at = capturedAt)
        }
        priceSnapshotRepository.compactHistory(marketHashName = marketHashName, now = capturedAt)

        return when {
            priceOverview != null -> ItemSyncResult.Synced
            (overview as SteamMarketResult.Failure).error == SteamMarketError.RateLimited ->
                ItemSyncResult.RateLimited

            else -> ItemSyncResult.Failed
        }
    }
}

private enum class ItemSyncResult {
    Synced,

    /** Failed for a reason of its own, leaving the rest of the run unaffected. */
    Failed,

    /** Steam is rejecting on request volume, so nothing else in this run could succeed either. */
    RateLimited,
}
