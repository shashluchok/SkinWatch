package com.shashluchok.skinwatch.presentation.navigation

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsIfStaleInteractor
import com.shashluchok.skinwatch.domain.debug.ObserveDebugSettingsInteractor
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsIfStaleInteractor
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppViewModel(
    priceSyncScheduler: PriceSyncScheduler,
    syncPriceSnapshotsIfStale: SyncPriceSnapshotsIfStaleInteractor,
    catalogSyncScheduler: CatalogSyncScheduler,
    syncCatalogItemsIfStale: SyncCatalogItemsIfStaleInteractor,
    private val observeDebugSettings: ObserveDebugSettingsInteractor,
) : BaseViewModel<AppViewModel.State, AppViewModel.Action>() {
    data class State(
        val isSplashVisible: Boolean = true,
    )

    sealed interface Action {
        data object SplashScreenAnimationFinished : Action
    }

    init {
        priceSyncScheduler.schedulePeriodicSync()
        catalogSyncScheduler.schedulePeriodicSync()
        viewModelScope.launch { syncPriceSnapshotsIfStale() }
        viewModelScope.launch { syncCatalogItemsIfStale() }
        checkSplashScreenAvailability()
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    override fun onAction(action: Action) = when (action) {
        Action.SplashScreenAnimationFinished -> onSplashScreenAnimationFinished()
    }

    private fun onSplashScreenAnimationFinished() {
        mutableStateFlow.update { it.copy(isSplashVisible = false) }
    }

    private fun checkSplashScreenAvailability() {
        viewModelScope.launch {
            val settings = observeDebugSettings().first()
            mutableStateFlow.update { it.copy(isSplashVisible = settings.showSplashScreen) }
        }
    }
}
