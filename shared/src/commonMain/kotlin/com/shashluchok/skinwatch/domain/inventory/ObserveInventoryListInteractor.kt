package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
internal class ObserveInventoryListInteractor(
    private val inventoryRepository: InventoryRepository,
    private val priceSnapshotRepository: PriceSnapshotRepository,
) {
    operator fun invoke(): Flow<List<InventoryListItem>> = inventoryRepository.observeItems().flatMapLatest { items ->
        val distinctNames = items.map { it.marketHashName }.distinct()
        val latestByNameFlow: Flow<Map<String, PriceSnapshot?>> = if (distinctNames.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                distinctNames.map { name ->
                    priceSnapshotRepository
                        .observeSnapshots(name)
                        .map { snapshots -> name to snapshots.maxByOrNull { it.capturedAt } }
                },
            ) { pairs -> pairs.toMap() }
        }
        latestByNameFlow.map { latestByName ->
            items.map { item -> InventoryListItem(item = item, latestSnapshot = latestByName[item.marketHashName]) }
        }
    }
}
