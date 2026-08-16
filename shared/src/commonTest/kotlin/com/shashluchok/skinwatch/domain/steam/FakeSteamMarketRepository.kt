package com.shashluchok.skinwatch.domain.steam

import kotlinx.coroutines.delay
import kotlin.time.Duration

internal class FakeSteamMarketRepository(
    override val defaultCurrency: SteamCurrency = SteamCurrency.USD,
) : SteamMarketRepository {
    val searchCalls = mutableListOf<String>()
    val searchCurrencies = mutableListOf<SteamCurrency>()
    val priceOverviewCurrencies = mutableListOf<SteamCurrency>()
    var searchDelay: Duration = Duration.ZERO
    var searchResult: SteamMarketResult<List<SteamMarketItem>> = SteamMarketResult.Success(emptyList())
    var priceOverviewResult: SteamMarketResult<SteamPriceOverview> = SteamMarketResult.Success(
        SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
    )

    override suspend fun searchItems(
        query: String,
        currency: SteamCurrency,
    ): SteamMarketResult<List<SteamMarketItem>> {
        searchCalls += query
        searchCurrencies += currency
        if (searchDelay > Duration.ZERO) delay(searchDelay)
        return searchResult
    }

    override suspend fun getPriceOverview(
        marketHashName: String,
        currency: SteamCurrency,
    ): SteamMarketResult<SteamPriceOverview> {
        priceOverviewCurrencies += currency
        return priceOverviewResult
    }
}
