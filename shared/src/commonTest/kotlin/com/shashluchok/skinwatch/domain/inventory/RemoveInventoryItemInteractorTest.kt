package com.shashluchok.skinwatch.domain.inventory

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
            purchasePrice = null,
            note = null,
        )
        val interactor = RemoveInventoryItemInteractor(inventoryRepository = inventoryRepository)

        interactor(id)

        assertEquals(listOf(id), inventoryRepository.removedIds)
    }
}
