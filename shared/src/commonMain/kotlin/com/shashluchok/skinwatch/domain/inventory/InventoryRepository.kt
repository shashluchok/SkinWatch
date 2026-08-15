package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import kotlinx.coroutines.flow.Flow

internal interface InventoryRepository {
    fun observeItems(): Flow<List<InventoryItem>>

    suspend fun addItem(
        marketHashName: String,
        quantity: Int,
        purchasePrice: Money?,
        note: String?,
    ): Long

    suspend fun updateItem(item: InventoryItem)

    suspend fun removeItem(id: Long)
}
