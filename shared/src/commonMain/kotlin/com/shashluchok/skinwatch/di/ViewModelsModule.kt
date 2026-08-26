package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.presentation.navigation.AppViewModel
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryViewModel
import com.shashluchok.skinwatch.presentation.screen.main.MainViewModel
import com.shashluchok.skinwatch.presentation.screen.settings.SettingsViewModel
import com.shashluchok.skinwatch.presentation.screen.settings.component.DebugPanelViewModel
import com.shashluchok.skinwatch.presentation.screen.watchlist.WatchlistViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val viewModelModule = module {
    // App-level (not tied to a screen)
    viewModel {
        AppViewModel(
            priceSyncScheduler = get(),
            syncPriceSnapshotsIfStale = get(),
            catalogSyncScheduler = get(),
            syncCatalogItemsIfStale = get(),
            observeDebugSettings = get(),
        )
    }

    // Screens
    viewModel {
        MainViewModel(
            searchCatalogItems = get(),
            addInventoryItem = get(),
        )
    }
    viewModel {
        InventoryViewModel(
            observeInventoryList = get(),
            updateInventoryItem = get(),
            removeInventoryItem = get(),
            observePriceHistory = get(),
            syncPriceSnapshots = get(),
            observeLastSyncedAt = get(),
        )
    }
    viewModel { WatchlistViewModel() }
    viewModel {
        SettingsViewModel(
            observeSelectedCurrency = get(),
            setSelectedCurrency = get(),
            getDefaultCurrency = get(),
            hasConvertiblePrices = get(),
            convertStoredPrices = get(),
            appConfigurationProvider = get(),
        )
    }

    // Debug (temporary -- see the `debug` package doc comment on DebugSettingsRepository)
    viewModel {
        DebugPanelViewModel(
            observeDebugSettings = get(),
            updateDebugSettings = get(),
        )
    }
}
