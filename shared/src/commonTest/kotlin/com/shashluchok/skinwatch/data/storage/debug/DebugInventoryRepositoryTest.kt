package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugInventoryRepositoryTest {
    @Test
    fun `starts pre-populated with debug-prefixed items`() = runTest {
        val repository = DebugInventoryRepository()

        val items = repository.observeItems().first()

        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.marketHashName.startsWith(DEBUG_NAME_PREFIX) })
        assertTrue(items.all { it.iconUrl == DEBUG_ICON_URL })
    }

    @Test
    fun `getDistinctMarketHashNames matches the pre-populated items`() = runTest {
        val repository = DebugInventoryRepository()

        val distinctNames = repository.getDistinctMarketHashNames()

        assertEquals(
            repository
                .observeItems()
                .first()
                .map { it.marketHashName }
                .distinct(),
            distinctNames,
        )
    }

    @Test
    fun `addItem appends a new item without touching the pre-populated ones`() = runTest {
        val repository = DebugInventoryRepository()
        val itemsBefore = repository.observeItems().first()

        val id = repository.addItem(
            marketHashName = "AK-47 | Redline",
            iconUrl = "icon",
            quantity = 2,
            purchasePrice = Money(minorUnits = 10_00, currency = SteamCurrency.USD),
        )

        val itemsAfter = repository.observeItems().first()
        assertEquals(itemsBefore.size + 1, itemsAfter.size)
        assertTrue(itemsAfter.any { it.id == id && it.marketHashName == "AK-47 | Redline" })
    }

    @Test
    fun `updateItem replaces the matching item`() = runTest {
        val repository = DebugInventoryRepository()
        val target = repository.observeItems().first().first()

        repository.updateItem(target.copy(quantity = target.quantity + 1))

        val updated = repository.observeItems().first().first { it.id == target.id }
        assertEquals(target.quantity + 1, updated.quantity)
    }

    @Test
    fun `removeItem drops the matching item`() = runTest {
        val repository = DebugInventoryRepository()
        val target = repository.observeItems().first().first()

        repository.removeItem(target.id)

        assertFalse(repository.observeItems().first().any { it.id == target.id })
    }
}
