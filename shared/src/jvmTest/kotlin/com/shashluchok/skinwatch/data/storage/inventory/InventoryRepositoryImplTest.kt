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
        )

        val items = repository.observeItems().first()
        val item = items.single()
        assertEquals(id, item.id)
        assertEquals("AK-47 | Redline (Field-Tested)", item.marketHashName)
        assertEquals("https://example.com/ak47-redline.png", item.iconUrl)
        assertEquals(2, item.quantity)
        assertEquals(Money(minorUnits = 1234, currency = SteamCurrency.USD), item.purchasePrice)
    }

    @Test
    fun `updateItem changes the stored quantity`() = runTest {
        val repository = newRepository()
        val id = repository.addItem(
            marketHashName = "AWP | Asiimov (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        val original = repository.observeItems().first().single()

        repository.updateItem(original.copy(quantity = 5))

        val updated = repository.observeItems().first().single()
        assertEquals(5, updated.quantity)
        assertEquals(id, updated.id)
    }

    @Test
    fun `removeItem deletes it from observeItems`() = runTest {
        val repository = newRepository()
        val id = repository.addItem(
            marketHashName = "USP-S | Kill Confirmed",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        assertTrue(repository.observeItems().first().isNotEmpty())

        repository.removeItem(id)

        assertTrue(repository.observeItems().first().isEmpty())
    }

    @Test
    fun `getDistinctMarketHashNames returns each name once`() = runTest {
        val repository = newRepository()
        repository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        repository.addItem(
            marketHashName = "AWP | Asiimov (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )

        val names = repository.getDistinctMarketHashNames()

        assertEquals(setOf("AK-47 | Redline (Field-Tested)", "AWP | Asiimov (Field-Tested)"), names.toSet())
        assertEquals(2, names.size)
    }

    @Test
    fun `getDistinctMarketHashNames returns an empty list when the inventory is empty`() = runTest {
        val repository = newRepository()

        assertEquals(emptyList(), repository.getDistinctMarketHashNames())
    }
}
