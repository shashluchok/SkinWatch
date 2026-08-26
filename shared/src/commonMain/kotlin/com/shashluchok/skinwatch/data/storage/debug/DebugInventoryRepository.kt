package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock

private val DEBUG_CURRENCY = SteamCurrency.RUB

/**
 * In-memory stand-in for [InventoryRepository], pre-populated with [debugScenarios]
 */
internal class DebugInventoryRepository : InventoryRepository {
    private val itemsFlow = MutableStateFlow(
        debugScenarios(Clock.System.now()).mapIndexed { index, scenario ->
            InventoryItem(
                id = index.toLong(),
                marketHashName = scenario.marketHashName,
                iconUrl = DEBUG_ICON_URL,
                addedAt = Clock.System.now(),
                quantity = 1,
                purchasePrice = Money(
                    minorUnits = scenario.purchasePriceMinorUnits,
                    currency = DEBUG_CURRENCY,
                ),
            )
        },
    )

    override fun observeItems(): Flow<List<InventoryItem>> = itemsFlow

    override suspend fun addItem(
        marketHashName: String,
        iconUrl: String,
        quantity: Int,
        purchasePrice: Money,
    ): Long {
        val id = (itemsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1
        itemsFlow.value += InventoryItem(
            id = id,
            marketHashName = marketHashName,
            iconUrl = iconUrl,
            addedAt = Clock.System.now(),
            quantity = quantity,
            purchasePrice = purchasePrice,
        )
        return id
    }

    override suspend fun updateItem(item: InventoryItem) {
        itemsFlow.value = itemsFlow.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun removeItem(id: Long) {
        itemsFlow.value = itemsFlow.value.filterNot { it.id == id }
    }

    override suspend fun getDistinctMarketHashNames(): List<String> =
        itemsFlow.value.map { it.marketHashName }.distinct()
}
