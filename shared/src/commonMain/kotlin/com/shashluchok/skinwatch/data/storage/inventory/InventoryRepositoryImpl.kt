package com.shashluchok.skinwatch.data.storage.inventory

import com.shashluchok.skinwatch.data.storage.idToSteamCurrency
import com.shashluchok.skinwatch.data.storage.steamCurrencyToId
import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.steam.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class InventoryRepositoryImpl(
    private val dao: InventoryDao,
) : InventoryRepository {
    override fun observeItems(): Flow<List<InventoryItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addItem(
        marketHashName: String,
        iconUrl: String,
        quantity: Int,
        purchasePrice: Money,
    ): Long = dao.insert(
        InventoryItemEntity(
            marketHashName = marketHashName,
            iconUrl = iconUrl,
            addedAt = Clock.System.now(),
            quantity = quantity,
            purchasePriceMinorUnits = purchasePrice.minorUnits,
            purchasePriceCurrencyId = steamCurrencyToId(purchasePrice.currency),
        ),
    )

    override suspend fun updateItem(item: InventoryItem) = dao.update(
        InventoryItemEntity(
            id = item.id,
            marketHashName = item.marketHashName,
            iconUrl = item.iconUrl,
            addedAt = item.addedAt,
            quantity = item.quantity,
            purchasePriceMinorUnits = item.purchasePrice.minorUnits,
            purchasePriceCurrencyId = steamCurrencyToId(item.purchasePrice.currency),
        ),
    )

    override suspend fun removeItem(id: Long) = dao.deleteById(id)

    override suspend fun getDistinctMarketHashNames(): List<String> = dao.getDistinctMarketHashNames()
}

private fun InventoryItemEntity.toDomain(): InventoryItem = InventoryItem(
    id = id,
    marketHashName = marketHashName,
    iconUrl = iconUrl,
    addedAt = addedAt,
    quantity = quantity,
    purchasePrice = Money(minorUnits = purchasePriceMinorUnits, currency = idToSteamCurrency(purchasePriceCurrencyId)),
)
