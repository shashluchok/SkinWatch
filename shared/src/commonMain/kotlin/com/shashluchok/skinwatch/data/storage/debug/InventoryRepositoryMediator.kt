package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.steam.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Delegates every call to either [realRepository] or [debugRepository] depending on the debug
 * panel's mock-data switch, read live from [debugSettingsRepository] on every call -- so toggling
 * the switch takes effect immediately, with no app restart.
 */
internal class InventoryRepositoryMediator(
    private val realRepository: InventoryRepository,
    private val debugRepository: InventoryRepository,
    private val debugSettingsRepository: DebugSettingsRepository,
) : InventoryRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeItems(): Flow<List<InventoryItem>> = debugSettingsRepository.settings.flatMapLatest {
        activeRepository(it.mockDataEnabled).observeItems()
    }

    override suspend fun addItem(
        marketHashName: String,
        iconUrl: String,
        quantity: Int,
        purchasePrice: Money,
    ): Long = activeRepository().addItem(
        marketHashName = marketHashName,
        iconUrl = iconUrl,
        quantity = quantity,
        purchasePrice = purchasePrice,
    )

    override suspend fun updateItem(item: InventoryItem) = activeRepository().updateItem(item)

    override suspend fun removeItem(id: Long) = activeRepository().removeItem(id)

    override suspend fun getDistinctMarketHashNames(): List<String> = activeRepository().getDistinctMarketHashNames()

    private suspend fun activeRepository(): InventoryRepository =
        activeRepository(debugSettingsRepository.settings.first().mockDataEnabled)

    private fun activeRepository(mockDataEnabled: Boolean): InventoryRepository =
        if (mockDataEnabled) debugRepository else realRepository
}
