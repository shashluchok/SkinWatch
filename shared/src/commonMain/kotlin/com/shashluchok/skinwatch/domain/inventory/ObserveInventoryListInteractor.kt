package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class ObserveInventoryListInteractor(
    private val inventoryRepository: InventoryRepository,
    private val priceSnapshotRepository: PriceSnapshotRepository,
) {
    operator fun invoke(): Flow<List<InventoryListItem>> = combine(
        inventoryRepository.observeItems(),
        priceSnapshotRepository.observeLatestSnapshots(),
    ) { items, latestByName ->
        items.map { item ->
            InventoryListItem(item = item, latestSnapshot = latestByName[item.marketHashName])
        }
    }
}
