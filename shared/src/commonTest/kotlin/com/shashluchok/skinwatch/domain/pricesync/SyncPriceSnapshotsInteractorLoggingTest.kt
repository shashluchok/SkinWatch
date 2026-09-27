package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SAMPLE_UNPRICED_OVERVIEW
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.synclog.FakeSyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogLevel
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val HASH_NAME = "AK-47 | Redline (Field-Tested)"

/**
 * The diagnostic log is only useful if it still records the events an investigation relies on, and
 * nothing else in the app breaks when a call site quietly stops logging -- hence these.
 */
class SyncPriceSnapshotsInteractorLoggingTest {
    private val inventoryRepository = FakeInventoryRepository()
    private val steamMarketRepository = FakeSteamMarketRepository()
    private val priceSnapshotRepository = FakePriceSnapshotRepository()
    private val priceSyncStatusRepository = FakePriceSyncStatusRepository()
    private val itemSyncStatusRepository = FakeItemSyncStatusRepository()
    private val syncLog = FakeSyncLogRepository()
    private val resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
        settingsRepository = FakeSettingsRepository(),
        steamMarketRepository = steamMarketRepository,
    )

    private fun newInteractor() = SyncPriceSnapshotsInteractor(
        inventoryRepository = inventoryRepository,
        steamMarketRepository = steamMarketRepository,
        priceSnapshotRepository = priceSnapshotRepository,
        resolveDisplayCurrency = resolveDisplayCurrency,
        priceSyncStatusRepository = priceSyncStatusRepository,
        itemSyncStatusRepository = itemSyncStatusRepository,
        syncLog = syncLog,
    )

    private suspend fun addTrackedItem(marketHashName: String = HASH_NAME) {
        inventoryRepository.addItem(
            marketHashName = marketHashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
    }

    @Test
    fun `a run records the trigger it was started by`() = runTest {
        addTrackedItem()

        newInteractor().invoke(trigger = SyncTrigger.RETRY_WORKER)

        assertTrue(
            syncLog
                .entriesWith(SyncLogTag.TRIGGER)
                .any { SyncTrigger.RETRY_WORKER.name in it.message },
        )
    }

    @Test
    fun `an item failure is recorded at error level against that item`() = runTest {
        addTrackedItem()
        steamMarketRepository.priceOverviewResult =
            SteamMarketResult.Failure(SteamMarketError.InvalidResponse)

        newInteractor().invoke(trigger = SyncTrigger.MANUAL)

        val failures = syncLog.entries.filter { it.level == SyncLogLevel.ERROR }
        assertEquals(HASH_NAME, failures.single().marketHashName)
        assertTrue(SteamMarketError.InvalidResponse.toString() in failures.single().message)
    }

    /** The only trace of an item Steam has no price for: it leaves no snapshot and no failure. */
    @Test
    fun `an answer with no price at all is recorded against the item`() = runTest {
        addTrackedItem()
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(SAMPLE_UNPRICED_OVERVIEW)

        newInteractor().invoke(trigger = SyncTrigger.MANUAL)

        assertTrue(
            syncLog
                .entriesWith(SyncLogTag.ITEM)
                .any { it.marketHashName == HASH_NAME && "no price at all" in it.message },
        )
    }

    @Test
    fun `a staleness decision is recorded per item`() = runTest {
        addTrackedItem()

        newInteractor().invoke(trigger = SyncTrigger.MANUAL)

        assertTrue(syncLog.entriesWith(SyncLogTag.DUE).any { it.marketHashName == HASH_NAME })
    }

    @Test
    fun `a second concurrent call records why it got no run`() = runTest {
        addTrackedItem()
        val interactor = newInteractor()
        interactor.invoke(trigger = SyncTrigger.MANUAL)
        syncLog.clear()

        // Nothing is due any more, so this second call runs to completion rather than being blocked
        // -- the lock is only contended while a run is in flight, which is what the WARN documents.
        val outcome = interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(PriceSyncOutcome.NothingDue, outcome)
        assertTrue(
            syncLog
                .entriesWith(SyncLogTag.TRIGGER)
                .any { SyncTrigger.PERIODIC_WORKER.name in it.message },
        )
    }
}
