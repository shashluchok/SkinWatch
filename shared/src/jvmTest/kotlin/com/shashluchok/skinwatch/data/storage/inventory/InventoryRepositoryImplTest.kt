package com.shashluchok.skinwatch.data.storage.inventory

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InventoryRepositoryImplTest {
    private fun newRepository(): InventoryRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return InventoryRepositoryImpl(dao = database.inventoryDao())
    }

    @Test
    fun `addItem then observeItems emits the added item with a real purchase price`() = runTest {
        val repository = newRepository()

        val id = repository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/ak47-redline.png",
            quantity = 2,
            purchasePrice = Money(minorUnits = 1234, currency = SteamCurrency.USD),
            note = "bought on sale",
        )

        val items = repository.observeItems().first()
        val item = items.single()
        assertEquals(id, item.id)
        assertEquals("AK-47 | Redline (Field-Tested)", item.marketHashName)
        assertEquals("https://example.com/ak47-redline.png", item.iconUrl)
        assertEquals(2, item.quantity)
        assertEquals(Money(minorUnits = 1234, currency = SteamCurrency.USD), item.purchasePrice)
        assertEquals("bought on sale", item.note)
    }

    @Test
    fun `addItem with no purchase price stores a null Money`() = runTest {
        val repository = newRepository()

        repository.addItem(
            marketHashName = "P250 | Sand Dune",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
        )

        val item = repository.observeItems().first().single()
        assertEquals(null, item.purchasePrice)
        assertEquals(null, item.note)
    }

    @Test
    fun `updateItem changes the stored quantity and note`() = runTest {
        val repository = newRepository()
        val id = repository.addItem(
            marketHashName = "AWP | Asiimov (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
        )
        val original = repository.observeItems().first().single()

        repository.updateItem(original.copy(quantity = 5, note = "updated"))

        val updated = repository.observeItems().first().single()
        assertEquals(5, updated.quantity)
        assertEquals("updated", updated.note)
        assertEquals(id, updated.id)
    }

    @Test
    fun `removeItem deletes it from observeItems`() = runTest {
        val repository = newRepository()
        val id = repository.addItem(
            marketHashName = "USP-S | Kill Confirmed",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
        )
        assertTrue(repository.observeItems().first().isNotEmpty())

        repository.removeItem(id)

        assertTrue(repository.observeItems().first().isEmpty())
    }
}
