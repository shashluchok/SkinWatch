package com.shashluchok.skinwatch.domain.steam

internal interface SteamMarketRepository {
    /** Derived from the device's locale (via `resolveSteamCurrency`) -- never persisted. */
    val defaultCurrency: SteamCurrency

    suspend fun getPriceOverview(
        marketHashName: String,
        currency: SteamCurrency = defaultCurrency,
    ): SteamMarketResult<SteamPriceOverview>
}
