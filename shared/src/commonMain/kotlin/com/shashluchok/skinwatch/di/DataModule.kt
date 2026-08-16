package com.shashluchok.skinwatch.di

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
import com.shashluchok.skinwatch.data.storage.inventory.InventoryRepositoryImpl
import com.shashluchok.skinwatch.data.storage.pricesnapshot.PriceSnapshotRepositoryImpl
import com.shashluchok.skinwatch.data.storage.settings.SettingsRepositoryImpl
import com.shashluchok.skinwatch.data.storage.watchlist.WatchlistRepositoryImpl
import com.shashluchok.skinwatch.domain.exchangerate.CurrencyConversionRepository
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateRepository
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.settings.SettingsRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.watchlist.WatchlistRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val PRIMARY_EXCHANGE_RATE_BASE_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest"
private const val FALLBACK_EXCHANGE_RATE_BASE_URL = "https://latest.currency-api.pages.dev"

/**
 * Wires data sources (HTTP client, DAOs) and binds every domain repository contract to its
 * concrete implementation -- `domainModule` stays free of anything living in `data.*`, reserved
 * for pure business-logic bindings.
 */
internal val dataModule = module {
    single { HttpClientFactory.create() }
    single<SteamMarketApi> { KtorSteamMarketApi(httpClient = get()) }
    single { SteamRateLimiter() }
    single<SteamMarketRepository> {
        SteamMarketRepositoryImpl(api = get(), rateLimiter = get(), deviceRegionCode = ::currentDeviceRegionCode)
    }

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

    single { get<AppDatabase>().inventoryDao() }
    single { get<AppDatabase>().watchlistDao() }
    single { get<AppDatabase>().priceSnapshotDao() }
    single { get<AppDatabase>().settingsDao() }
    single<InventoryRepository> { InventoryRepositoryImpl(dao = get()) }
    single<WatchlistRepository> { WatchlistRepositoryImpl(dao = get()) }
    single<PriceSnapshotRepository> { PriceSnapshotRepositoryImpl(dao = get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(dao = get()) }
    single<CurrencyConversionRepository> {
        CurrencyConversionRepositoryImpl(
            database = get(),
            inventoryDao = get(),
            priceSnapshotDao = get(),
            settingsDao = get(),
        )
    }
}
