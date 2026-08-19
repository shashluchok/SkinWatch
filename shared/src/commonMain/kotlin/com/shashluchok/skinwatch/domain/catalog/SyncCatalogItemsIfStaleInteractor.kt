package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.flow.first
import kotlin.time.Clock

internal class SyncCatalogItemsIfStaleInteractor(
    private val catalogSyncStatusRepository: CatalogSyncStatusRepository,
    private val syncCatalogItems: SyncCatalogItemsInteractor,
) {
    suspend operator fun invoke() {
        val lastCompletedAt = catalogSyncStatusRepository.lastCompletedAt.first()
        val isStale = lastCompletedAt == null || Clock.System.now() - lastCompletedAt >= CATALOG_SYNC_INTERVAL
        if (isStale) syncCatalogItems()
    }
}
