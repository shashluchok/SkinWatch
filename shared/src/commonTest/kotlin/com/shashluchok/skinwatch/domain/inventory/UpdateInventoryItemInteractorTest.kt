package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class UpdateInventoryItemInteractorTest {
    private val inventoryRepository = FakeInventoryRepository()
    private val settingsRepository = FakeSettingsRepository(initialCurrency = SteamCurrency.EUR)
    private val steamMarketRepository = FakeSteamMarketRepository(defaultCurrency = SteamCurrency.USD)
    private val interactor = UpdateInventoryItemInteractor(
        inventoryRepository = inventoryRepository,
        resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
            settingsRepository = settingsRepository,
            steamMarketRepository = steamMarketRepository,
        ),
    )

    private fun sampleItem(purchasePrice: Money?) = InventoryItem(
        id = 1L,
        marketHashName = "AK-47 | Redline (Field-Tested)",
        iconUrl = "https://example.com/icon.png",
        addedAt = Instant.fromEpochMilliseconds(0),
        quantity = 1,
        purchasePrice = purchasePrice,
        note = null,
    )

    @Test
    fun `keeps the item's existing purchase-price currency instead of re-resolving it`() = runTest {
        val item = sampleItem(purchasePrice = Money(minorUnits = 100_000, currency = SteamCurrency.USD))

        interactor(item = item, quantity = 4, purchasePriceAmount = 500.0, note = "resold once")

        val updated = inventoryRepository.updatedItems.single()
        assertEquals(SteamCurrency.USD, updated.purchasePrice?.currency)
        assertEquals(4, updated.quantity)
        assertEquals("resold once", updated.note)
    }

    @Test
    fun `resolves the display currency for a brand-new purchase price`() = runTest {
        val item = sampleItem(purchasePrice = null)

        interactor(item = item, quantity = 1, purchasePriceAmount = 12.5, note = null)

        val updated = inventoryRepository.updatedItems.single()
        assertEquals(Money(minorUnits = 1250, currency = SteamCurrency.EUR), updated.purchasePrice)
    }

    @Test
    fun `updates to a null purchase price when no amount is given`() = runTest {
        val item = sampleItem(purchasePrice = Money(minorUnits = 100_000, currency = SteamCurrency.USD))

        interactor(item = item, quantity = 1, purchasePriceAmount = null, note = null)

        val updated = inventoryRepository.updatedItems.single()
        assertNull(updated.purchasePrice)
    }
}
