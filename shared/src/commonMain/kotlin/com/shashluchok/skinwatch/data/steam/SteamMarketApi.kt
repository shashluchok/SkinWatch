package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.data.steam.dto.PriceOverviewResponseDto
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal interface SteamMarketApi {
    suspend fun getPriceOverview(
        marketHashName: String,
        currency: SteamCurrency,
    ): PriceOverviewResponseDto
}

internal class KtorSteamMarketApi(
    private val httpClient: HttpClient,
) : SteamMarketApi {
    override suspend fun getPriceOverview(
        marketHashName: String,
        currency: SteamCurrency,
    ): PriceOverviewResponseDto = httpClient
        .get("$BASE_URL/market/priceoverview/") {
            parameter(key = "market_hash_name", value = marketHashName)
            parameter(key = "appid", value = CS2_APP_ID)
            parameter(key = "currency", value = currency.id)
        }.body()

    private companion object {
        const val BASE_URL = "https://steamcommunity.com"
        const val CS2_APP_ID = 730
    }
}
