package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class SyncPriceSnapshotsIfStaleInteractorTest {
    private val priceSyncStatusRepository = FakePriceSyncStatusRepository()
    private val inventoryRepository = FakeInventoryRepository()
    private val syncPriceSnapshots = SyncPriceSnapshotsInteractor(
        inventoryRepository = inventoryRepository,
        steamMarketRepository = FakeSteamMarketRepository(),
        priceSnapshotRepository = FakePriceSnapshotRepository(),
        resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
            settingsRepository = FakeSettingsRepository(),
            steamMarketRepository = FakeSteamMarketRepository(),
        ),
        priceSyncStatusRepository = priceSyncStatusRepository,
    )
    private val interactor = SyncPriceSnapshotsIfStaleInteractor(
        priceSyncStatusRepository = priceSyncStatusRepository,
        syncPriceSnapshots = syncPriceSnapshots,
    )

    // A run with nothing to sync is itself a no-op (see SyncPriceSnapshotsInteractorTest) -- every
    // test here needs at least one item so "is a sync attempted" is actually observable.
    private suspend fun seedOneItem() = inventoryRepository.addItem(
        marketHashName = "AK-47 | Redline (Field-Tested)",
        iconUrl = "https://example.com/icon.png",
        quantity = 1,
        purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
    )

    @Test
    fun `syncs when nothing has ever been synced`() = runTest {
        seedOneItem()

        interactor()

        assertEquals(1, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `does not sync when the last sync is younger than the interval`() = runTest {
        seedOneItem()
        priceSyncStatusRepository.markCompleted(Clock.System.now())

        interactor()

        assertEquals(1, priceSyncStatusRepository.markCompletedCalls.size) // only the setup call above
    }

    @Test
    fun `syncs when the last sync is at least as old as the interval`() = runTest {
        seedOneItem()
        priceSyncStatusRepository.markCompleted(Clock.System.now() - PRICE_SYNC_INTERVAL)

        interactor()

        assertEquals(2, priceSyncStatusRepository.markCompletedCalls.size) // setup call + this one
    }
}
