package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency

internal interface ExchangeRateRepository {
    suspend fun getRates(base: SteamCurrency): ExchangeRateResult<Map<SteamCurrency, Double>>
}
