package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.data.catalog.CatalogHttpClientFactory
import com.shashluchok.skinwatch.data.catalog.KtorItemCatalogApi
import com.shashluchok.skinwatch.data.exchangerate.ExchangeRateApi
import com.shashluchok.skinwatch.data.exchangerate.ExchangeRateHttpClientFactory
import com.shashluchok.skinwatch.data.exchangerate.ExchangeRateRepositoryImpl
import com.shashluchok.skinwatch.data.exchangerate.KtorExchangeRateApi
import com.shashluchok.skinwatch.data.steam.HttpClientFactory
import com.shashluchok.skinwatch.data.steam.KtorSteamMarketApi
import com.shashluchok.skinwatch.data.steam.SteamMarketApi
import com.shashluchok.skinwatch.data.steam.SteamMarketRepositoryImpl
import com.shashluchok.skinwatch.data.steam.SteamRateLimiter
import com.shashluchok.skinwatch.data.steam.currentDeviceRegionCode
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.CurrencyConversionRepositoryImpl
import com.shashluchok.skinwatch.data.storage.catalog.CatalogSyncStatusRepositoryImpl
import com.shashluchok.skinwatch.data.storage.catalog.ItemCatalogRepositoryImpl
import com.shashluchok.skinwatch.data.storage.debug.DebugInventoryRepository
import com.shashluchok.skinwatch.data.storage.debug.DebugPriceSnapshotRepository
import com.shashluchok.skinwatch.data.storage.debug.DebugSettingsRepositoryImpl
import com.shashluchok.skinwatch.data.storage.debug.InventoryRepositoryMediator
import com.shashluchok.skinwatch.data.storage.debug.PriceSnapshotRepositoryMediator
import com.shashluchok.skinwatch.data.storage.inventory.InventoryRepositoryImpl
import com.shashluchok.skinwatch.data.storage.pricesnapshot.PriceSnapshotRepositoryImpl
import com.shashluchok.skinwatch.data.storage.pricesync.PriceSyncStatusRepositoryImpl
import com.shashluchok.skinwatch.data.storage.settings.SettingsRepositoryImpl
import com.shashluchok.skinwatch.data.storage.watchlist.WatchlistRepositoryImpl
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncStatusRepository
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRemoteSource
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRepository
import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import com.shashluchok.skinwatch.domain.exchangerate.CurrencyConversionRepository
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateRepository
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncStatusRepository
import com.shashluchok.skinwatch.domain.settings.SettingsRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.watchlist.WatchlistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val PRIMARY_EXCHANGE_RATE_BASE_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest"
private const val FALLBACK_EXCHANGE_RATE_BASE_URL = "https://latest.currency-api.pages.dev"

internal val dataModule = module {
    // Steam
    single { HttpClientFactory.create() }
    single<SteamMarketApi> { KtorSteamMarketApi(httpClient = get()) }
    single { SteamRateLimiter() }
    single<SteamMarketRepository> {
        SteamMarketRepositoryImpl(api = get(), rateLimiter = get(), deviceRegionCode = ::currentDeviceRegionCode)
    }

    // Exchange rate
    single { ExchangeRateHttpClientFactory.create() }
    single<ExchangeRateApi>(named("exchangeRatePrimary")) {
        KtorExchangeRateApi(httpClient = get(), baseUrl = PRIMARY_EXCHANGE_RATE_BASE_URL)
    }
    single<ExchangeRateApi>(named("exchangeRateFallback")) {
        KtorExchangeRateApi(httpClient = get(), baseUrl = FALLBACK_EXCHANGE_RATE_BASE_URL)
    }
    single<ExchangeRateRepository> {
        ExchangeRateRepositoryImpl(
            primaryApi = get(named("exchangeRatePrimary")),
            fallbackApi = get(named("exchangeRateFallback")),
        )
    }

    // Database
    single { get<AppDatabase>().inventoryDao() }
    single { get<AppDatabase>().watchlistDao() }
    single { get<AppDatabase>().priceSnapshotDao() }
    single { get<AppDatabase>().settingsDao() }
    single { get<AppDatabase>().priceSyncStatusDao() }
    single<InventoryRepository> {
        InventoryRepositoryMediator(
            realRepository = InventoryRepositoryImpl(dao = get()),
            debugRepository = DebugInventoryRepository(),
            debugSettingsRepository = get(),
        )
    }
    single<WatchlistRepository> { WatchlistRepositoryImpl(dao = get()) }
    single<PriceSnapshotRepository> {
        PriceSnapshotRepositoryMediator(
            realRepository = PriceSnapshotRepositoryImpl(dao = get()),
            debugRepository = DebugPriceSnapshotRepository(),
            debugSettingsRepository = get(),
        )
    }
    single<SettingsRepository> { SettingsRepositoryImpl(dao = get()) }
    single<PriceSyncStatusRepository> { PriceSyncStatusRepositoryImpl(dao = get()) }
    single<CurrencyConversionRepository> {
        CurrencyConversionRepositoryImpl(
            database = get(),
            inventoryDao = get(),
            priceSnapshotDao = get(),
            settingsDao = get(),
        )
    }

    // Catalog
    single(named("catalog")) { CatalogHttpClientFactory.create() }
    single<ItemCatalogRemoteSource> { KtorItemCatalogApi(httpClient = get(named("catalog"))) }
    single { get<AppDatabase>().catalogItemDao() }
    single { get<AppDatabase>().catalogSyncStatusDao() }
    single<ItemCatalogRepository> { ItemCatalogRepositoryImpl(dao = get()) }
    single<CatalogSyncStatusRepository> { CatalogSyncStatusRepositoryImpl(dao = get()) }

    // Debug
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { get<AppDatabase>().debugSettingsDao() }
    single<DebugSettingsRepository> { DebugSettingsRepositoryImpl(dao = get(), scope = get()) }
}
