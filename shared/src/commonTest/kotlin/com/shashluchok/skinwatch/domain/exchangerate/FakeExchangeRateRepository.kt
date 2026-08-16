package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency

internal class FakeExchangeRateRepository(
    private val result: ExchangeRateResult<Map<SteamCurrency, Double>>,
) : ExchangeRateRepository {
    var lastRequestedBase: SteamCurrency? = null
        private set

    override suspend fun getRates(base: SteamCurrency): ExchangeRateResult<Map<SteamCurrency, Double>> {
        lastRequestedBase = base
        return result
    }
}
