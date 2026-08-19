package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncStatusRepository
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRemoteSource
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRepository
import com.shashluchok.skinwatch.domain.exchangerate.CurrencyConversionRepository
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncStatusRepository
import com.shashluchok.skinwatch.domain.settings.SettingsRepository
import com.shashluchok.skinwatch.domain.watchlist.WatchlistRepository
import org.koin.dsl.module

internal val webModule = module {
    // Price sync
    single<PriceSyncScheduler> { PriceSyncScheduler.EMPTY }
    single<InventoryRepository> { InventoryRepository.EMPTY }
    single<WatchlistRepository> { WatchlistRepository.EMPTY }
    single<PriceSnapshotRepository> { PriceSnapshotRepository.EMPTY }
    single<SettingsRepository> { SettingsRepository.EMPTY }
    single<PriceSyncStatusRepository> { PriceSyncStatusRepository.EMPTY }
    single<CurrencyConversionRepository> { CurrencyConversionRepository.EMPTY }

    // Catalog sync
    single<CatalogSyncScheduler> { CatalogSyncScheduler.EMPTY }
    single<ItemCatalogRemoteSource> { ItemCatalogRemoteSource.EMPTY }
    single<ItemCatalogRepository> { ItemCatalogRepository.EMPTY }
    single<CatalogSyncStatusRepository> { CatalogSyncStatusRepository.EMPTY }
}

object WebModule {
    fun init() = initKoin(platformModule = webModule)
}
