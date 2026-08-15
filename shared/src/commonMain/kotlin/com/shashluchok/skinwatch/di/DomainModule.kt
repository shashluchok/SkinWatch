package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.data.steam.SteamMarketRepositoryImpl
import com.shashluchok.skinwatch.data.steam.currentDeviceRegionCode
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import org.koin.dsl.module

internal val domainModule = module {
    single<SteamMarketRepository> {
        SteamMarketRepositoryImpl(api = get(), rateLimiter = get(), deviceRegionCode = ::currentDeviceRegionCode)
    }
}
