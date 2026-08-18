package com.shashluchok.skinwatch.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsIfStaleInteractor
import kotlinx.coroutines.launch

/**
 * No UI state of its own -- exists only to own the app-startup price sync trigger, which is not
 * tied to any particular screen and must run once per app launch regardless of navigation.
 */
internal class AppViewModel(
    priceSyncScheduler: PriceSyncScheduler,
    syncPriceSnapshotsIfStale: SyncPriceSnapshotsIfStaleInteractor,
) : ViewModel() {
    init {
        priceSyncScheduler.schedulePeriodicSync()
        viewModelScope.launch {
            syncPriceSnapshotsIfStale()
        }
    }
}
