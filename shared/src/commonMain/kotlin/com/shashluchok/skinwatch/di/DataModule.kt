package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.data.steam.HttpClientFactory
import com.shashluchok.skinwatch.data.steam.KtorSteamMarketApi
import com.shashluchok.skinwatch.data.steam.SteamMarketApi
import com.shashluchok.skinwatch.data.steam.SteamRateLimiter
import org.koin.dsl.module

internal val dataModule = module {
    single { HttpClientFactory.create() }
    single<SteamMarketApi> { KtorSteamMarketApi(httpClient = get()) }
    single { SteamRateLimiter() }
}
