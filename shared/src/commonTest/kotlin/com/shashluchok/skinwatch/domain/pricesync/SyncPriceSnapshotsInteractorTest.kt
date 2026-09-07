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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val HASH_NAME = "AK-47 | Redline (Field-Tested)"

class SyncPriceSnapshotsInteractorTest {
    private suspend fun addTrackedItem(marketHashName: String = HASH_NAME) {
        inventoryRepository.addItem(
            marketHashName = marketHashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
    }

    private val inventoryRepository = FakeInventoryRepository()
    private val steamMarketRepository = FakeSteamMarketRepository()
    private val priceSnapshotRepository = FakePriceSnapshotRepository()
    private val priceSyncStatusRepository = FakePriceSyncStatusRepository()
    private val itemSyncStatusRepository = FakeItemSyncStatusRepository()
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

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

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

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(1, priceSnapshotRepository.recorded.size)
        assertEquals("succeeds", priceSnapshotRepository.recorded.single().marketHashName)
    }

    @Test
    fun `a run with any failure is not marked completed, so staleness still triggers a retry`() = runTest {
        inventoryRepository.addItem(
            marketHashName = "fails",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        val outcome = newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(PriceSyncOutcome.HadFailures, outcome)
        assertEquals(0, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `one failure among many is enough to withhold completion`() = runTest {
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

        val outcome = newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(PriceSyncOutcome.HadFailures, outcome)
        assertEquals(0, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `a clean run reports completion and advances the last completed timestamp`() = runTest {
        inventoryRepository.addItem(
            marketHashName = "succeeds",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        val outcome = newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(PriceSyncOutcome.Completed, outcome)
        assertEquals(1, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `an empty inventory reports NothingDue rather than a completed run`() = runTest {
        assertEquals(PriceSyncOutcome.NothingDue, newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER))
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

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(setOf("fails", "succeeds"), priceSnapshotRepository.compactHistoryCalls.toSet())
    }

    @Test
    fun `an empty inventory is a no-op -- no fetch, no snapshot, no completed run`() = runTest {
        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

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

        val firstRun = launch { interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER) }
        testScheduler.runCurrent() // let firstRun start and reach the delay, then pause there
        // runMutex.tryLock() fails -- returns immediately, no second pass
        interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER)
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

        val firstRun = launch { interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER) }
        testScheduler.runCurrent() // run reaches the delay and pauses -- still "in flight" here
        assertTrue(interactor.isSyncing.value)

        testScheduler.advanceUntilIdle() // let the delay elapse and the run finish
        assertTrue(!interactor.isSyncing.value)
        firstRun.join()
    }

    @Test
    fun `a run moments after a successful one requests nothing again`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        repeat(3) {
            inventoryRepository.addItem(
                marketHashName = hashName,
                iconUrl = "https://example.com/icon.png",
                quantity = 1,
                purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
            )
        }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )
        val interactor = newInteractor()

        interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER)
        val outcome = interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(1, steamMarketRepository.priceOverviewCalls.size)
        assertEquals(1, priceSnapshotRepository.recorded.size)
        // Nothing was due -- still a finished pass, so it advances the timestamp like any other.
        assertEquals(PriceSyncOutcome.NothingDue, outcome)
        assertEquals(2, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `an item last priced longer ago than the interval is fetched again`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        itemSyncStatusRepository.markSynced(
            marketHashName = hashName,
            at = Clock.System.now() - PRICE_SYNC_INTERVAL - 1.hours,
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(1, steamMarketRepository.priceOverviewCalls.size)
    }

    @Test
    fun `an item that failed carries no success time and records the error it failed with`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        val status = itemSyncStatusRepository.statuses.getValue(hashName)
        assertTrue(status is ItemSyncStatus.Failed)
        assertEquals(SteamMarketError.Network, status.error)
        assertNull(status.lastSuccessAt)
    }

    /** The point of the backoff curve: one bad attempt costs one more attempt, not a wait. */
    @Test
    fun `an item that has failed once is tried again on the very next run`() = runTest {
        addTrackedItem()
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)
        val interactor = newInteractor()

        interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER)
        interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(2, steamMarketRepository.priceOverviewCalls.size)
    }

    @Test
    fun `an item that keeps failing is left alone between runs`() = runTest {
        addTrackedItem()
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)
        val interactor = newInteractor()

        // The first two attempts are spent walking the curve; by the third the wait is real.
        repeat(times = 4) { interactor.invoke(trigger = SyncTrigger.PERIODIC_WORKER) }

        assertEquals(2, steamMarketRepository.priceOverviewCalls.size)
    }

    @Test
    fun `an item whose backoff has aged past its wait is tried again`() = runTest {
        addTrackedItem()
        repeat(times = 3) {
            itemSyncStatusRepository.markFailed(
                marketHashName = HASH_NAME,
                error = SteamMarketError.Network,
                at = Clock.System.now() - 2.hours,
            )
        }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(1, steamMarketRepository.priceOverviewCalls.size)
    }

    @Test
    fun `an unusable answer earns a long wait sooner than a network failure does`() = runTest {
        addTrackedItem()
        itemSyncStatusRepository.markFailed(
            marketHashName = HASH_NAME,
            error = SteamMarketError.InvalidResponse,
            at = Clock.System.now() - 2.minutes,
        )

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        // Two minutes clears a first network failure, but not a first unusable answer.
        assertEquals(0, steamMarketRepository.priceOverviewCalls.size)
    }

    /** Waiting on an item that has failed its way to the end would freeze the timestamp forever. */
    @Test
    fun `an item that has been failing for a while does not hold back the completed timestamp`() = runTest {
        addTrackedItem(marketHashName = "written-off")
        addTrackedItem(marketHashName = "priceable")
        repeat(times = 4) {
            itemSyncStatusRepository.markFailed(
                marketHashName = "written-off",
                error = SteamMarketError.InvalidResponse,
                at = Clock.System.now() - 2.days,
            )
        }
        steamMarketRepository.priceOverviewResultsByHashName["written-off"] =
            SteamMarketResult.Failure(SteamMarketError.InvalidResponse)
        steamMarketRepository.priceOverviewResultsByHashName["priceable"] = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        val outcome = newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(PriceSyncOutcome.Completed, outcome)
        assertEquals(1, priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `a manual run spends a request on an item every backoff would have withheld`() = runTest {
        addTrackedItem()
        repeat(times = 9) {
            itemSyncStatusRepository.markFailed(
                marketHashName = HASH_NAME,
                error = SteamMarketError.InvalidResponse,
                at = Clock.System.now(),
            )
        }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        newInteractor().invoke(trigger = SyncTrigger.MANUAL)

        assertEquals(1, steamMarketRepository.priceOverviewCalls.size)
    }

    /**
     * The whole inventory failing identically is the shape of a run with no working network, not of
     * an inventory full of dead items -- and marching on records a failure against every one of them.
     */
    @Test
    fun `a run whose first items all fail the same way stops instead of walking the rest`() = runTest {
        repeat(times = 8) { addTrackedItem(marketHashName = "item-$it") }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        val outcome = newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(3, steamMarketRepository.priceOverviewCalls.size)
        assertEquals(PriceSyncOutcome.HadFailures, outcome)
        assertEquals(emptyList(), priceSyncStatusRepository.markCompletedCalls)
    }

    @Test
    fun `a run that is getting somewhere keeps going despite failures`() = runTest {
        repeat(times = 8) { addTrackedItem(marketHashName = "item-$it") }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)
        steamMarketRepository.priceOverviewResultsByHashName["item-0"] = SteamMarketResult.Success(
            SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
        )

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(8, steamMarketRepository.priceOverviewCalls.size)
    }

    @Test
    fun `a retryable failure does hold back the completed timestamp`() = runTest {
        inventoryRepository.addItem(
            marketHashName = "offline",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        val outcome = newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(PriceSyncOutcome.HadFailures, outcome)
        assertEquals(emptyList(), priceSyncStatusRepository.markCompletedCalls)
    }

    @Test
    fun `a failure after a success keeps the earlier success time`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        val syncedAt = Clock.System.now() - PRICE_SYNC_INTERVAL - 1.hours
        itemSyncStatusRepository.markSynced(marketHashName = hashName, at = syncedAt)
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        val status = itemSyncStatusRepository.statuses.getValue(hashName)
        assertTrue(status is ItemSyncStatus.Failed)
        assertEquals(syncedAt, status.lastSuccessAt)
    }

    @Test
    fun `a rate limit ends the run instead of spending the quota on requests that cannot succeed`() = runTest {
        listOf("first", "second", "third").forEach { hashName ->
            inventoryRepository.addItem(
                marketHashName = hashName,
                iconUrl = "https://example.com/icon.png",
                quantity = 1,
                purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
            )
        }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.RateLimited)

        val outcome = newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(PriceSyncOutcome.HadFailures, outcome)
        assertEquals(1, steamMarketRepository.priceOverviewCalls.size)
    }

    @Test
    fun `items left untried by a rate limit keep no status, so the next run still owes them`() = runTest {
        listOf("first", "second").forEach { hashName ->
            inventoryRepository.addItem(
                marketHashName = hashName,
                iconUrl = "https://example.com/icon.png",
                quantity = 1,
                purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
            )
        }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.RateLimited)

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(1, itemSyncStatusRepository.statuses.size)
    }

    @Test
    fun `an ordinary failure does not end the run`() = runTest {
        listOf("first", "second").forEach { hashName ->
            inventoryRepository.addItem(
                marketHashName = hashName,
                iconUrl = "https://example.com/icon.png",
                quantity = 1,
                purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
            )
        }
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.InvalidResponse)

        newInteractor().invoke(trigger = SyncTrigger.PERIODIC_WORKER)

        assertEquals(2, steamMarketRepository.priceOverviewCalls.size)
    }
}
