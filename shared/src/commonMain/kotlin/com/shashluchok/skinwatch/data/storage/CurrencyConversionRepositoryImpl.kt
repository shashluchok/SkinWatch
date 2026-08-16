package com.shashluchok.skinwatch.data.storage

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.shashluchok.skinwatch.data.storage.inventory.InventoryDao
import com.shashluchok.skinwatch.data.storage.inventory.InventoryItemEntity
import com.shashluchok.skinwatch.data.storage.pricesnapshot.PriceSnapshotDao
import com.shashluchok.skinwatch.data.storage.pricesnapshot.PriceSnapshotEntity
import com.shashluchok.skinwatch.data.storage.settings.SettingsDao
import com.shashluchok.skinwatch.data.storage.settings.SettingsEntity
import com.shashluchok.skinwatch.domain.exchangerate.CurrencyConversionRepository
import com.shashluchok.skinwatch.domain.exchangerate.convertMoney
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency

/**
 * The only repository in the project allowed to hold [AppDatabase] directly plus more than one
 * DAO -- every other repository keeps a single-DAO shape. `InventoryItem`/`PriceSnapshot`/
 * `Settings` must be rewritten together, all-or-nothing, so this class bypasses
 * `InventoryRepository`/`PriceSnapshotRepository`/`SettingsRepository` (none of which give a
 * transactional guarantee across each other) and talks to the three DAOs directly inside one
 * Room write transaction.
 */
internal class CurrencyConversionRepositoryImpl(
    private val database: AppDatabase,
    private val inventoryDao: InventoryDao,
    private val priceSnapshotDao: PriceSnapshotDao,
    private val settingsDao: SettingsDao,
) : CurrencyConversionRepository {
    override suspend fun hasConvertibleData(): Boolean = inventoryDao.hasAny() || priceSnapshotDao.hasAny()

    override suspend fun convertAll(
        rates: Map<SteamCurrency, Double>,
        targetCurrency: SteamCurrency,
        newSelectedCurrency: SteamCurrency?,
    ) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val items = inventoryDao.getAll()
                val convertedItems = items.map { it.convert(rates = rates, targetCurrency = targetCurrency) }
                inventoryDao.updateAll(convertedItems)

                val convertedSnapshots =
                    priceSnapshotDao.getAll().map { it.convert(rates = rates, targetCurrency = targetCurrency) }
                priceSnapshotDao.updateAll(convertedSnapshots)

                settingsDao.upsert(
                    SettingsEntity(selectedCurrencyId = newSelectedCurrency?.let(::steamCurrencyToId)),
                )
            }
        }
    }
}

private fun InventoryItemEntity.convert(
    rates: Map<SteamCurrency, Double>,
    targetCurrency: SteamCurrency,
): InventoryItemEntity {
    val minorUnits = purchasePriceMinorUnits
    val currencyId = purchasePriceCurrencyId
    if (minorUnits == null || currencyId == null) return this

    val converted = convertMoney(
        money = Money(minorUnits = minorUnits, currency = idToSteamCurrency(currencyId)),
        targetCurrency = targetCurrency,
        targetPerUnitRates = rates,
    )
    return copy(
        purchasePriceMinorUnits = converted.minorUnits,
        purchasePriceCurrencyId = steamCurrencyToId(converted.currency),
    )
}

private fun PriceSnapshotEntity.convert(
    rates: Map<SteamCurrency, Double>,
    targetCurrency: SteamCurrency,
): PriceSnapshotEntity {
    val currency = idToSteamCurrency(currencyId)
    val convertedLowest = lowestPriceMinorUnits?.let {
        convertMoney(
            money = Money(minorUnits = it, currency = currency),
            targetCurrency = targetCurrency,
            targetPerUnitRates = rates,
        ).minorUnits
    }
    val convertedMedian = medianPriceMinorUnits?.let {
        convertMoney(
            money = Money(minorUnits = it, currency = currency),
            targetCurrency = targetCurrency,
            targetPerUnitRates = rates,
        ).minorUnits
    }
    return copy(
        currencyId = steamCurrencyToId(targetCurrency),
        lowestPriceMinorUnits = convertedLowest,
        medianPriceMinorUnits = convertedMedian,
    )
}
