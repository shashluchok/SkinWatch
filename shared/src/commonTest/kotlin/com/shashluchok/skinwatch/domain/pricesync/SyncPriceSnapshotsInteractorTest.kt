package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class SyncPriceSnapshotsInteractorTest {
    private val inventoryRepository = FakeInventoryRepository()
    private val steamMarketRepository = FakeSteamMarketRepository()
    private val priceSnapshotRepository = FakePriceSnapshotRepository()
    private val priceSyncStatusRepository = FakePriceSyncStatusRepository()
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
    )

    @Test
    fun `records one snapshot per distinct marketHashName, not one per inventory row`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 200, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        newInteractor().invoke()

        assertEquals(1, priceSnapshotRepository.recorded.size)
        assertEquals(1, steamMarketRepository.priceOverviewCalls.count { it == hashName })
    }

    @Test
    fun `a failed item is skipped and does not stop the rest of the run`() = runTest {
        inventoryRepository.addItem(
            marketHashName = "fails",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        inventoryRepository.addItem(
            marketHashName = "succeeds",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResultsByHashName["fails"] =
            SteamMarketResult.Failure(SteamMarketError.Network)
        steamMarketRepository.priceOverviewResultsByHashName["succeeds"] = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        newInteractor().invoke()

        assertEquals(1, priceSnapshotRepository.recorded.size)
        assertEquals("succeeds", priceSnapshotRepository.recorded.single().marketHashName)
    }

    @Test
    fun `marks the run completed even when some items failed`() = runTest {
        inventoryRepository.addItem(
            marketHashName = "fails",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        newInteractor().invoke()

        assertEquals(1, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `compacts history for every item, including ones whose fetch failed`() = runTest {
        inventoryRepository.addItem(
            marketHashName = "fails",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        inventoryRepository.addItem(
            marketHashName = "succeeds",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResultsByHashName["fails"] =
            SteamMarketResult.Failure(SteamMarketError.Network)
        steamMarketRepository.priceOverviewResultsByHashName["succeeds"] = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        newInteractor().invoke()

        assertEquals(setOf("fails", "succeeds"), priceSnapshotRepository.compactHistoryCalls.toSet())
    }

    @Test
    fun `an empty inventory is a no-op -- no fetch, no snapshot, no completed run`() = runTest {
        newInteractor().invoke()

        assertEquals(0, steamMarketRepository.priceOverviewCalls.size)
        assertEquals(0, priceSnapshotRepository.recorded.size)
        assertEquals(0, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `a concurrent invoke while a run is in progress is a no-op`() = runTest {
        // A positive delay gives the first run a real suspension point to be paused at -- without
        // one, none of these fakes ever suspend, so there would be no way to deterministically get
        // a second invoke() to observe the first one as "in progress" under
        // kotlinx-coroutines-test's cooperative scheduler.
        steamMarketRepository.priceOverviewDelay = 1.hours
        inventoryRepository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        val interactor = newInteractor()

        val firstRun = launch { interactor.invoke() }
        testScheduler.runCurrent() // let firstRun start and reach the delay, then pause there
        interactor.invoke() // runMutex.tryLock() fails -- returns immediately, no second pass
        testScheduler.advanceUntilIdle() // let firstRun's delay elapse and the run finish

        assertEquals(1, priceSyncStatusRepository.markCompletedCalls.size)
        firstRun.join()
    }

    @Test
    fun `isSyncing is true while a run is suspended mid-flight, false once it completes`() = runTest {
        steamMarketRepository.priceOverviewDelay = 1.hours
        inventoryRepository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        val interactor = newInteractor()
        assertTrue(!interactor.isSyncing.value)

        val firstRun = launch { interactor.invoke() }
        testScheduler.runCurrent() // run reaches the delay and pauses -- still "in flight" here
        assertTrue(interactor.isSyncing.value)

        testScheduler.advanceUntilIdle() // let the delay elapse and the run finish
        assertTrue(!interactor.isSyncing.value)
        firstRun.join()
    }
}
