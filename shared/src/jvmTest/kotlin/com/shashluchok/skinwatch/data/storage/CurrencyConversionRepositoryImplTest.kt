package com.shashluchok.skinwatch.data.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.inventory.InventoryItemEntity
import com.shashluchok.skinwatch.data.storage.pricesnapshot.PriceSnapshotEntity
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class CurrencyConversionRepositoryImplTest {
    private fun newDatabase() = Room
        .inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

    private fun newRepository(database: AppDatabase) = CurrencyConversionRepositoryImpl(
        database = database,
        inventoryDao = database.inventoryDao(),
        priceSnapshotDao = database.priceSnapshotDao(),
        settingsDao = database.settingsDao(),
    )

    @Test
    fun `hasConvertibleData is false when both tables are empty`() = runTest {
        val repository = newRepository(newDatabase())

        assertFalse(repository.hasConvertibleData())
    }

    @Test
    fun `hasConvertibleData is true when only PriceSnapshot has rows`() = runTest {
        val database = newDatabase()
        database.priceSnapshotDao().insert(
            PriceSnapshotEntity(
                marketHashName = "AK-47 | Redline (Field-Tested)",
                currencyId = SteamCurrency.USD.id,
                lowestPriceMinorUnits = 5000,
                medianPriceMinorUnits = 5200,
                volume = 10,
                capturedAt = Instant.fromEpochMilliseconds(1_755_000_000_000),
            ),
        )
        val repository = newRepository(database)

        assertTrue(repository.hasConvertibleData())
    }

    @Test
    fun `convertAll rewrites InventoryItem purchasePrice, PriceSnapshot prices, and Settings atomically`() =
        runTest {
            val database = newDatabase()
            val itemId = database.inventoryDao().insert(
                InventoryItemEntity(
                    marketHashName = "AK-47 | Redline (Field-Tested)",
                    iconUrl = "https://example.com/icon.png",
                    addedAt = Instant.fromEpochMilliseconds(1_755_000_000_000),
                    quantity = 1,
                    purchasePriceMinorUnits = 10000,
                    purchasePriceCurrencyId = SteamCurrency.USD.id,
                    note = null,
                ),
            )
            database.priceSnapshotDao().insert(
                PriceSnapshotEntity(
                    marketHashName = "AK-47 | Redline (Field-Tested)",
                    currencyId = SteamCurrency.USD.id,
                    lowestPriceMinorUnits = 10000,
                    medianPriceMinorUnits = 10500,
                    volume = 10,
                    capturedAt = Instant.fromEpochMilliseconds(1_755_000_000_000),
                ),
            )
            val repository = newRepository(database)

            // 1 EUR = 2.0 USD -- 10000 minor USD units / 2.0 = 5000, 10500 / 2.0 = 5250.
            repository.convertAll(
                rates = mapOf(SteamCurrency.USD to 2.0, SteamCurrency.EUR to 1.0),
                targetCurrency = SteamCurrency.EUR,
                newSelectedCurrency = SteamCurrency.EUR,
            )

            val item = database.inventoryDao().getAll().single { it.id == itemId }
            assertEquals(5000, item.purchasePriceMinorUnits)
            assertEquals(SteamCurrency.EUR.id, item.purchasePriceCurrencyId)

            val snapshot = database.priceSnapshotDao().getAll().single()
            assertEquals(5000, snapshot.lowestPriceMinorUnits)
            assertEquals(5250, snapshot.medianPriceMinorUnits)
            assertEquals(SteamCurrency.EUR.id, snapshot.currencyId)

            val settings = database.settingsDao().observe().first()
            assertEquals(SteamCurrency.EUR.id, settings?.selectedCurrencyId)
        }

    @Test
    fun `convertAll leaves a row already in the target currency unchanged`() = runTest {
        val database = newDatabase()
        database.inventoryDao().insert(
            InventoryItemEntity(
                marketHashName = "P250 | Sand Dune",
                iconUrl = "https://example.com/icon.png",
                addedAt = Instant.fromEpochMilliseconds(1_755_000_000_000),
                quantity = 1,
                purchasePriceMinorUnits = 500,
                purchasePriceCurrencyId = SteamCurrency.EUR.id,
                note = null,
            ),
        )
        val repository = newRepository(database)

        repository.convertAll(
            rates = mapOf(SteamCurrency.USD to 2.0, SteamCurrency.EUR to 1.0),
            targetCurrency = SteamCurrency.EUR,
            newSelectedCurrency = SteamCurrency.EUR,
        )

        val item = database.inventoryDao().getAll().single()
        assertEquals(500, item.purchasePriceMinorUnits)
        assertEquals(SteamCurrency.EUR.id, item.purchasePriceCurrencyId)
    }

    @Test
    fun `convertAll leaves stored data untouched when the transaction throws partway through`() = runTest {
        val database = newDatabase()
        val itemId = database.inventoryDao().insert(
            InventoryItemEntity(
                marketHashName = "AK-47 | Redline (Field-Tested)",
                iconUrl = "https://example.com/icon.png",
                addedAt = Instant.fromEpochMilliseconds(1_755_000_000_000),
                quantity = 1,
                purchasePriceMinorUnits = 10000,
                purchasePriceCurrencyId = SteamCurrency.USD.id,
                note = null,
            ),
        )
        val repository = newRepository(database)

        // The rate map is missing SteamCurrency.USD -- convertMoney.getValue throws mid-transaction,
        // after InventoryItem would already have been rewritten if it weren't inside a transaction.
        runCatching {
            repository.convertAll(
                rates = emptyMap(),
                targetCurrency = SteamCurrency.EUR,
                newSelectedCurrency = SteamCurrency.EUR,
            )
        }

        val item = database.inventoryDao().getAll().single { it.id == itemId }
        assertEquals(10000, item.purchasePriceMinorUnits)
        assertEquals(SteamCurrency.USD.id, item.purchasePriceCurrencyId)
        val settings = database.settingsDao().observe().first()
        assertEquals(null, settings?.selectedCurrencyId)
    }
}
