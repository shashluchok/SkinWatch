package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.presentation.navigation.AppViewModel
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryViewModel
import com.shashluchok.skinwatch.presentation.screen.main.MainViewModel
import com.shashluchok.skinwatch.presentation.screen.settings.SettingsViewModel
import com.shashluchok.skinwatch.presentation.screen.splash.SplashViewModel
import com.shashluchok.skinwatch.presentation.screen.watchlist.WatchlistViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val viewModelModule = module {
    viewModel {
        AppViewModel(
            priceSyncScheduler = get(),
            syncPriceSnapshotsIfStale = get(),
        )
    }
    viewModel { SplashViewModel() }
    viewModel {
        MainViewModel(
            searchMarketItems = get(),
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
        )
    }
}
