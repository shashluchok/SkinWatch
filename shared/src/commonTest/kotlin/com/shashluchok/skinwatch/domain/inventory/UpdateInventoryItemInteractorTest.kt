package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class UpdateInventoryItemInteractorTest {
    private val inventoryRepository = FakeInventoryRepository()
    private val interactor = UpdateInventoryItemInteractor(inventoryRepository = inventoryRepository)

    private fun sampleItem(purchasePrice: Money) = InventoryItem(
        id = 1L,
        marketHashName = "AK-47 | Redline (Field-Tested)",
        iconUrl = "https://example.com/icon.png",
        addedAt = Instant.fromEpochMilliseconds(0),
        quantity = 1,
        purchasePrice = purchasePrice,
    )

    @Test
    fun `keeps the item's existing purchase-price currency instead of re-resolving it`() = runTest {
        val item = sampleItem(purchasePrice = Money(minorUnits = 100_000, currency = SteamCurrency.USD))

        interactor(item = item, quantity = 4, purchasePriceAmount = 500.0)

        val updated = inventoryRepository.updatedItems.single()
        assertEquals(SteamCurrency.USD, updated.purchasePrice.currency)
        assertEquals(4, updated.quantity)
    }

    @Test
    fun `builds the purchase price minor units from the given amount`() = runTest {
        val item = sampleItem(purchasePrice = Money(minorUnits = 100_000, currency = SteamCurrency.USD))

        interactor(item = item, quantity = 1, purchasePriceAmount = 12.5)

        val updated = inventoryRepository.updatedItems.single()
        assertEquals(Money(minorUnits = 1250, currency = SteamCurrency.USD), updated.purchasePrice)
    }
}
