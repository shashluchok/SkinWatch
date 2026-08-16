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
        purchasePrice: Money?,
        note: String?,
    ): Long = dao.insert(
        InventoryItemEntity(
            marketHashName = marketHashName,
            iconUrl = iconUrl,
            addedAt = Clock.System.now(),
            quantity = quantity,
            purchasePriceMinorUnits = purchasePrice?.minorUnits,
            purchasePriceCurrencyId = purchasePrice?.currency?.let(::steamCurrencyToId),
            note = note,
        ),
    )

    override suspend fun updateItem(item: InventoryItem) = dao.update(
        InventoryItemEntity(
            id = item.id,
            marketHashName = item.marketHashName,
            iconUrl = item.iconUrl,
            addedAt = item.addedAt,
            quantity = item.quantity,
            purchasePriceMinorUnits = item.purchasePrice?.minorUnits,
            purchasePriceCurrencyId = item.purchasePrice?.currency?.let(::steamCurrencyToId),
            note = item.note,
        ),
    )

    override suspend fun removeItem(id: Long) = dao.deleteById(id)
}

private fun InventoryItemEntity.toDomain(): InventoryItem {
    val purchasePrice = if (purchasePriceMinorUnits != null && purchasePriceCurrencyId != null) {
        Money(minorUnits = purchasePriceMinorUnits, currency = idToSteamCurrency(purchasePriceCurrencyId))
    } else {
        null
    }
    return InventoryItem(
        id = id,
        marketHashName = marketHashName,
        iconUrl = iconUrl,
        addedAt = addedAt,
        quantity = quantity,
        purchasePrice = purchasePrice,
        note = note,
    )
}
