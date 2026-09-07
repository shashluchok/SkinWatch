package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesync.ItemSyncStatusRepository

internal class RemoveInventoryItemInteractor(
    private val inventoryRepository: InventoryRepository,
    private val itemSyncStatusRepository: ItemSyncStatusRepository,
) {
    /**
     * The sync status belongs to the `marketHashName`, not to the row: several rows can hold the
     * same skin and share one status, so it only goes when the last of them does -- which is why the
     * check reads the inventory back rather than assuming this row was the only one.
     */
    suspend operator fun invoke(item: InventoryItem) {
        inventoryRepository.removeItem(item.id)
        if (item.marketHashName !in inventoryRepository.getDistinctMarketHashNames()) {
            itemSyncStatusRepository.delete(item.marketHashName)
        }
    }
}
