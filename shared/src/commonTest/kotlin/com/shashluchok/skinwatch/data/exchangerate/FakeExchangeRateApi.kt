package com.shashluchok.skinwatch.data.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.serialization.json.JsonObject

internal class FakeExchangeRateApi(
    private val result: Result<JsonObject>,
) : ExchangeRateApi {
    var callCount: Int = 0
        private set

    override suspend fun getRates(base: SteamCurrency): JsonObject {
        callCount++
        return result.getOrThrow()
    }
}
