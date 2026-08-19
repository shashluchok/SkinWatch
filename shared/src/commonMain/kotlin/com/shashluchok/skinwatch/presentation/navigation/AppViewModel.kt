package com.shashluchok.skinwatch.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsIfStaleInteractor
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsIfStaleInteractor
import kotlinx.coroutines.launch

/**
 * No UI state of its own -- exists only to own the app-startup price/catalog sync triggers, which
 * are not tied to any particular screen and must run once per app launch regardless of navigation.
 */
internal class AppViewModel(
    priceSyncScheduler: PriceSyncScheduler,
    syncPriceSnapshotsIfStale: SyncPriceSnapshotsIfStaleInteractor,
    catalogSyncScheduler: CatalogSyncScheduler,
    syncCatalogItemsIfStale: SyncCatalogItemsIfStaleInteractor,
) : ViewModel() {
    init {
        priceSyncScheduler.schedulePeriodicSync()
        catalogSyncScheduler.schedulePeriodicSync()
        viewModelScope.launch { syncPriceSnapshotsIfStale() }
        viewModelScope.launch { syncCatalogItemsIfStale() }
    }
}
