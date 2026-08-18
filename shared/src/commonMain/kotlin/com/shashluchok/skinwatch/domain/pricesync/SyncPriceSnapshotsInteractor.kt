package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock

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
) {
    private val runMutex = Mutex()
    private val mutableIsSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = mutableIsSyncing.asStateFlow()

    suspend operator fun invoke() {
        // A run is already in progress -- a second call is a no-op, not a queued retry: it would
        // just re-sync what the active run is already about to finish syncing.
        if (!runMutex.tryLock()) return
        try {
            val marketHashNames = inventoryRepository.getDistinctMarketHashNames()
            // Nothing to sync
            if (marketHashNames.isEmpty()) return
            mutableIsSyncing.value = true
            val currency = resolveDisplayCurrency()
            // One shared capturedAt for the whole run, matching PriceSnapshotRepository.record's
            // existing contract for a batch of snapshots taken together.
            val capturedAt = Clock.System.now()
            marketHashNames.forEach { marketHashName ->
                val overview = steamMarketRepository.getPriceOverview(
                    marketHashName = marketHashName,
                    currency = currency,
                )
                if (overview is SteamMarketResult.Success) {
                    priceSnapshotRepository.record(
                        marketHashName = marketHashName,
                        overview = overview.data,
                        currency = currency,
                        capturedAt = capturedAt,
                    )
                }
                // Failure: skip, keep going -- picked up on the next cycle.
            }
            priceSyncStatusRepository.markCompleted(capturedAt)
        } finally {
            mutableIsSyncing.value = false
            runMutex.unlock()
        }
    }
}
