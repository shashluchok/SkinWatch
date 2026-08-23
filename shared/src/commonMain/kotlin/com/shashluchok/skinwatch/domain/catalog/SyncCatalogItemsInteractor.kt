package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock

/**
 * The single implementation of "fetch every catalog category and replace its rows locally" --
 * called by every trigger (the platform periodic scheduler and the app-open staleness check),
 * never duplicated per trigger.
 */
internal class SyncCatalogItemsInteractor(
    private val remoteSource: ItemCatalogRemoteSource,
    private val catalogRepository: ItemCatalogRepository,
    private val catalogSyncStatusRepository: CatalogSyncStatusRepository,
) {
    private val runMutex = Mutex()
    private val mutableIsSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = mutableIsSyncing.asStateFlow()

    suspend operator fun invoke() {
        // A run is already in progress -- a second call is a no-op, not a queued retry: it would
        // just re-sync what the active run is already about to finish syncing.
        if (!runMutex.tryLock()) return
        try {
            mutableIsSyncing.value = true
            var anySucceeded = false
            CatalogCategory.entries.forEach { category ->
                var isFirstChunk = true
                val result = remoteSource.fetch(category) { chunk ->
                    // Old rows are cleared only once the first chunk actually arrives, not before
                    // the fetch starts -- a fetch that fails immediately leaves previously cached
                    // rows for this category untouched instead of wiping them for nothing.
                    if (isFirstChunk) {
                        catalogRepository.clearCategory(category)
                        isFirstChunk = false
                    }
                    catalogRepository.insertItems(category = category, items = chunk)
                }
                if (result is CatalogFetchResult.Success) anySucceeded = true
                // Failure: skip the rest of this category, keep whatever rows it already has
                // (previously cached, or partially inserted from this run), continue with the rest.
            }
            if (anySucceeded) catalogSyncStatusRepository.markCompleted(Clock.System.now())
        } finally {
            mutableIsSyncing.value = false
            runMutex.unlock()
        }
    }
}
