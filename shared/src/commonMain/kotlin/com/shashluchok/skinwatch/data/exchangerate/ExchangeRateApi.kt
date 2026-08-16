package com.shashluchok.skinwatch.data.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject

internal interface ExchangeRateApi {
    suspend fun getRates(base: SteamCurrency): JsonObject
}

/**
 * [baseUrl] is everything before `/v1/currencies/{base}.json` -- e.g.
 * `https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest` for the primary domain or
 * `https://latest.currency-api.pages.dev` for the fallback one. One class, two DI-registered
 * instances (see `DataModule`'s exchange-rate bindings) rather than two near-identical classes.
 */
internal class KtorExchangeRateApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : ExchangeRateApi {
    override suspend fun getRates(base: SteamCurrency): JsonObject =
        httpClient.get("$baseUrl/v1/currencies/${base.name.lowercase()}.json").body()
}
