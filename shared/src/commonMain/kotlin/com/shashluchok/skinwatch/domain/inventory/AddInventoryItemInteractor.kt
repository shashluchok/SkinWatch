package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.ItemSyncStatusRepository
import com.shashluchok.skinwatch.domain.pricesync.PRICE_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.isRetryable
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.domain.synclog.error
import com.shashluchok.skinwatch.domain.synclog.info
import com.shashluchok.skinwatch.domain.synclog.warn
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.TimeSource

internal class AddInventoryItemInteractor(
    private val inventoryRepository: InventoryRepository,
    private val steamMarketRepository: SteamMarketRepository,
    private val priceSnapshotRepository: PriceSnapshotRepository,
    private val resolveDisplayCurrency: ResolveDisplayCurrencyInteractor,
    private val priceSyncScheduler: PriceSyncScheduler,
    private val itemSyncStatusRepository: ItemSyncStatusRepository,
    private val syncLog: SyncLogRepository = SyncLogRepository.EMPTY,
) {
    suspend operator fun invoke(
        marketHashName: String,
        iconUrl: String,
        quantity: Int,
        purchasePriceAmount: Double,
    ) {
        syncLog.info(tag = SyncLogTag.ADD, message = "adding x$quantity", marketHashName = marketHashName)
        val currency = resolveDisplayCurrency()
        val purchasePrice = Money(
            minorUnits = (purchasePriceAmount * MINOR_UNITS_PER_MAJOR_UNIT).roundToLong(),
            currency = currency,
        )
        inventoryRepository.addItem(
            marketHashName = marketHashName,
            iconUrl = iconUrl,
            quantity = quantity,
            purchasePrice = purchasePrice,
        )
        syncLog.info(tag = SyncLogTag.ADD, message = "stored in the inventory", marketHashName = marketHashName)
        if (needsFreshPrice(marketHashName)) {
            fetchInitialPrice(marketHashName = marketHashName, currency = currency)
        }
    }

    /**
     * A failure here leaves the item sitting in the list with no price at all, which is why it asks
     * for a retry instead of waiting out [PRICE_SYNC_INTERVAL] like an ordinary stale price would.
     *
     * The caller's scope dying mid-request is a live suspect for items that never get a price, and
     * it is invisible from the outside -- the cancellation is recorded before being rethrown.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchInitialPrice(marketHashName: String, currency: SteamCurrency) {
        syncLog.info(
            tag = SyncLogTag.ADD,
            message = "fetching an initial price in $currency",
            marketHashName = marketHashName,
        )
        val startMark = TimeSource.Monotonic.markNow()
        val overview = try {
            steamMarketRepository.getPriceOverview(marketHashName = marketHashName, currency = currency)
        } catch (throwable: Throwable) {
            syncLog.error(
                tag = SyncLogTag.ADD,
                message = "initial fetch stopped after ${startMark.elapsedNow()} by " +
                    "${throwable::class.simpleName}: ${throwable.message} -- no price, no retry scheduled",
                marketHashName = marketHashName,
            )
            throw throwable
        }
        when (overview) {
            is SteamMarketResult.Success -> recordInitialPrice(
                marketHashName = marketHashName,
                priceOverview = overview.data,
                currency = currency,
                elapsed = startMark.elapsedNow().toString(),
            )

            is SteamMarketResult.Failure -> recordInitialFailure(
                marketHashName = marketHashName,
                error = overview.error,
                elapsed = startMark.elapsedNow().toString(),
            )
        }
    }

    /**
     * The sync status is written alongside the snapshot, not only the snapshot itself: the scheduled
     * run decides what to re-request purely from that status, so an item priced here without one
     * would count as never synced and be fetched again on the very next run.
     */
    private suspend fun recordInitialPrice(
        marketHashName: String,
        priceOverview: SteamPriceOverview,
        currency: SteamCurrency,
        elapsed: String,
    ) {
        val capturedAt = Clock.System.now()
        priceSnapshotRepository.record(
            marketHashName = marketHashName,
            overview = priceOverview,
            currency = currency,
            capturedAt = capturedAt,
        )
        itemSyncStatusRepository.markSynced(marketHashName = marketHashName, at = capturedAt)
        syncLog.info(
            tag = SyncLogTag.ADD,
            message = "initial price recorded at $capturedAt after $elapsed " +
                "(lowest=${priceOverview.lowestPrice}, median=${priceOverview.medianPrice})",
            marketHashName = marketHashName,
        )
    }

    /**
     * Left without a price: ask for another attempt once there is a connection again, rather than
     * leaving the item blank until the next scheduled run. An error no retry could fix gets no
     * retry -- the run it would start could only reproduce the same failure.
     */
    private suspend fun recordInitialFailure(
        marketHashName: String,
        error: SteamMarketError,
        elapsed: String,
    ) {
        itemSyncStatusRepository.markFailed(
            marketHashName = marketHashName,
            error = error,
            at = Clock.System.now(),
        )
        syncLog.warn(
            tag = SyncLogTag.ADD,
            message = "initial fetch failed after $elapsed with $error" +
                if (error.isRetryable) ", asking for a retry run" else ", not retryable",
            marketHashName = marketHashName,
        )
        if (error.isRetryable) priceSyncScheduler.scheduleRetrySync()
    }

    /**
     * Skips the add-time fetch when this marketHashName was already priced recently -- otherwise
     * adding a duplicate of an already-tracked item would write another near-simultaneous snapshot
     * that every item sharing that hash sees too.
     */
    private suspend fun needsFreshPrice(marketHashName: String): Boolean {
        val latestCapturedAt = priceSnapshotRepository
            .observeSnapshots(marketHashName)
            .first()
            .maxOfOrNull { it.capturedAt }
        val now = Clock.System.now()
        val needsFresh = latestCapturedAt == null || now - latestCapturedAt >= PRICE_SYNC_INTERVAL
        syncLog.info(
            tag = SyncLogTag.ADD,
            message = "newest snapshot=" + (latestCapturedAt?.let { "$it (${now - it} ago)" } ?: "none") +
                ", ${if (needsFresh) "fetching now" else "reusing it, no fetch"}",
            marketHashName = marketHashName,
        )
        return needsFresh
    }

    private companion object {
        const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
    }
}
