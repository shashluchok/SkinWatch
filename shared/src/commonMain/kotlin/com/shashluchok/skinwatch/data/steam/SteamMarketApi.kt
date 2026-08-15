package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.data.steam.dto.PriceOverviewResponseDto
import com.shashluchok.skinwatch.data.steam.dto.SearchRenderResponseDto
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal interface SteamMarketApi {
    suspend fun searchItems(
        query: String,
        currency: SteamCurrency,
        count: Int,
        start: Int,
    ): SearchRenderResponseDto

    suspend fun getPriceOverview(
        marketHashName: String,
        currency: SteamCurrency,
    ): PriceOverviewResponseDto
}

internal class KtorSteamMarketApi(
    private val httpClient: HttpClient,
) : SteamMarketApi {
    override suspend fun searchItems(
        query: String,
        currency: SteamCurrency,
        count: Int,
        start: Int,
    ): SearchRenderResponseDto = httpClient
        .get("$BASE_URL/market/search/render/") {
            parameter(key = "query", value = query)
            parameter(key = "appid", value = CS2_APP_ID)
            parameter(key = "norender", value = 1)
            parameter(key = "count", value = count)
            parameter(key = "start", value = start)
            parameter(key = "currency", value = currency.id)
        }.body()

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
