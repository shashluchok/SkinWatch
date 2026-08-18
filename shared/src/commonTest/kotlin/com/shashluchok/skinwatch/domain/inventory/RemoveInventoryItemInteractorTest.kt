package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoveInventoryItemInteractorTest {
    @Test
    fun `removes the item with the given id from the repository`() = runTest {
        val inventoryRepository = FakeInventoryRepository()
        val id = inventoryRepository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        val interactor = RemoveInventoryItemInteractor(inventoryRepository = inventoryRepository)

        interactor(id)

        assertEquals(listOf(id), inventoryRepository.removedIds)
    }
}
