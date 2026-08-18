package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface InventoryRepository {
    fun observeItems(): Flow<List<InventoryItem>>

    suspend fun addItem(
        marketHashName: String,
        iconUrl: String,
        quantity: Int,
        purchasePrice: Money,
    ): Long

    suspend fun updateItem(item: InventoryItem)

    suspend fun removeItem(id: Long)

    suspend fun getDistinctMarketHashNames(): List<String>

    companion object {
        val EMPTY = object : InventoryRepository {
            override fun observeItems(): Flow<List<InventoryItem>> = flowOf(emptyList())

            override suspend fun addItem(
                marketHashName: String,
                iconUrl: String,
                quantity: Int,
                purchasePrice: Money,
            ): Long = 0L

            override suspend fun updateItem(item: InventoryItem) = Unit

            override suspend fun removeItem(id: Long) = Unit

            override suspend fun getDistinctMarketHashNames(): List<String> = emptyList()
        }
    }
}
