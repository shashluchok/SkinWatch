package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.FakePriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.PRICE_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class AddInventoryItemInteractorTest {
    private val inventoryRepository = FakeInventoryRepository()
    private val priceSnapshotRepository = FakePriceSnapshotRepository()
    private val steamMarketRepository = FakeSteamMarketRepository()
    private val settingsRepository = FakeSettingsRepository(initialCurrency = SteamCurrency.USD)
    private val priceSyncScheduler = FakePriceSyncScheduler()
    private val interactor = AddInventoryItemInteractor(
        inventoryRepository = inventoryRepository,
        steamMarketRepository = steamMarketRepository,
        priceSnapshotRepository = priceSnapshotRepository,
        resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
            settingsRepository = settingsRepository,
            steamMarketRepository = steamMarketRepository,
        ),
        priceSyncScheduler = priceSyncScheduler,
    )

    @Test
    fun `adds the item with a Money built from the purchase price amount`() = runTest {
        interactor(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 2,
            purchasePriceAmount = 12.5,
        )

        val added = inventoryRepository.observeItems().value.single()
        assertEquals("AK-47 | Redline (Field-Tested)", added.marketHashName)
        assertEquals(2, added.quantity)
        assertEquals(Money(minorUnits = 1250, currency = SteamCurrency.USD), added.purchasePrice)
    }

    @Test
    fun `records a price snapshot when the price overview succeeds`() = runTest {
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(
                lowestPrice = Money(minorUnits = 4900, currency = SteamCurrency.USD),
                medianPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
                volume = 42,
            ),
        )

        interactor(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePriceAmount = 5.0,
        )

        val recorded = priceSnapshotRepository.recorded.single()
        assertEquals(Money(minorUnits = 4900, currency = SteamCurrency.USD), recorded.lowestPrice)
    }

    @Test
    fun `records no price snapshot when the price overview fails`() = runTest {
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        interactor(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePriceAmount = 5.0,
        )

        assertEquals(1, inventoryRepository.observeItems().value.size)
        assertEquals(emptyList(), priceSnapshotRepository.recorded)
    }

    @Test
    fun `skips the fetch for a duplicate marketHashName already priced recently`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 4900, currency = SteamCurrency.USD),
            capturedAt = Clock.System.now(),
        )

        interactor(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePriceAmount = 5.0,
        )

        assertEquals(0, steamMarketRepository.priceOverviewCalls.size)
        assertEquals(emptyList(), priceSnapshotRepository.recorded)
    }

    @Test
    fun `still fetches for a duplicate marketHashName whose history is overdue for a sync`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 4900, currency = SteamCurrency.USD),
            capturedAt = Clock.System.now() - PRICE_SYNC_INTERVAL,
        )
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(
                lowestPrice = Money(minorUnits = 5100, currency = SteamCurrency.USD),
                medianPrice = null,
                volume = null,
            ),
        )

        interactor(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePriceAmount = 5.0,
        )

        assertEquals(1, steamMarketRepository.priceOverviewCalls.size)
        assertEquals(1, priceSnapshotRepository.recorded.size)
    }

    @Test
    fun `a failed add-time fetch asks for a retry once there is a connection again`() = runTest {
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)

        interactor(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePriceAmount = 5.0,
        )

        assertEquals(1, priceSyncScheduler.retrySyncScheduledCount)
    }

    @Test
    fun `a successful add-time fetch schedules nothing extra`() = runTest {
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
            SteamPriceOverview(
                lowestPrice = Money(minorUnits = 5100, currency = SteamCurrency.USD),
                medianPrice = null,
                volume = null,
            ),
        )

        interactor(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePriceAmount = 5.0,
        )

        assertEquals(0, priceSyncScheduler.retrySyncScheduledCount)
    }
}
