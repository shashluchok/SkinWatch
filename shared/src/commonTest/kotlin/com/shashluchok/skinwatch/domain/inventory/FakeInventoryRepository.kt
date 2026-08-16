package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

internal class FakeInventoryRepository : InventoryRepository {
    private val itemsFlow = MutableStateFlow<List<InventoryItem>>(emptyList())
    private var nextId = 1L
    val updatedItems = mutableListOf<InventoryItem>()
    val removedIds = mutableListOf<Long>()

    override fun observeItems(): StateFlow<List<InventoryItem>> = itemsFlow

    override suspend fun addItem(
        marketHashName: String,
        iconUrl: String,
        quantity: Int,
        purchasePrice: Money?,
        note: String?,
    ): Long {
        val id = nextId++
        itemsFlow.value = itemsFlow.value + InventoryItem(
            id = id,
            marketHashName = marketHashName,
            iconUrl = iconUrl,
            addedAt = Instant.fromEpochMilliseconds(0),
            quantity = quantity,
            purchasePrice = purchasePrice,
            note = note,
        )
        return id
    }

    override suspend fun updateItem(item: InventoryItem) {
        updatedItems += item
        itemsFlow.value = itemsFlow.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun removeItem(id: Long) {
        removedIds += id
        itemsFlow.value = itemsFlow.value.filterNot { it.id == id }
    }
}
