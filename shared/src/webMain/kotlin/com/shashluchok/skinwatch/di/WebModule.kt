package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.domain.exchangerate.CurrencyConversionRepository
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncStatusRepository
import com.shashluchok.skinwatch.domain.settings.SettingsRepository
import com.shashluchok.skinwatch.domain.watchlist.WatchlistRepository
import org.koin.dsl.module

internal val webModule = module {
    single<PriceSyncScheduler> { PriceSyncScheduler.EMPTY }
    single<InventoryRepository> { InventoryRepository.EMPTY }
    single<WatchlistRepository> { WatchlistRepository.EMPTY }
    single<PriceSnapshotRepository> { PriceSnapshotRepository.EMPTY }
    single<SettingsRepository> { SettingsRepository.EMPTY }
    single<PriceSyncStatusRepository> { PriceSyncStatusRepository.EMPTY }
    single<CurrencyConversionRepository> { CurrencyConversionRepository.EMPTY }
}

object WebModule {
    fun init() = initKoin(platformModule = webModule)
}
