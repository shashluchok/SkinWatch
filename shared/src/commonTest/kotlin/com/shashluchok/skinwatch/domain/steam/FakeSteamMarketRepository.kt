package com.shashluchok.skinwatch.domain.steam

import kotlinx.coroutines.delay
import kotlin.time.Duration

internal class FakeSteamMarketRepository(
    override val defaultCurrency: SteamCurrency = SteamCurrency.USD,
) : SteamMarketRepository {
    val searchCalls = mutableListOf<String>()
    val searchCurrencies = mutableListOf<SteamCurrency>()
    val priceOverviewCalls = mutableListOf<String>()
    val priceOverviewCurrencies = mutableListOf<SteamCurrency>()
    var searchDelay: Duration = Duration.ZERO
    var searchResult: SteamMarketResult<List<SteamMarketItem>> = SteamMarketResult.Success(emptyList())
    var priceOverviewResult: SteamMarketResult<SteamPriceOverview> = SteamMarketResult.Success(
        SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
    )

    /**
     * Per-[marketHashName] overrides, checked before falling back to [priceOverviewResult] -- lets
     * a test make one specific item fail while the rest succeed, without needing every caller of
     * this fake to know about the override map.
     */
    val priceOverviewResultsByHashName = mutableMapOf<String, SteamMarketResult<SteamPriceOverview>>()

    /**
     * Lets a test hold `getPriceOverview` at a real suspension point (via `delay`) to deterministically
     * control interleaving with `kotlinx-coroutines-test`'s `TestCoroutineScheduler`
     * (`runCurrent()`/`advanceUntilIdle()`) -- needed by `SyncPriceSnapshotsInteractorTest`'s
     * concurrency and `isSyncing` tests, which would otherwise have no suspension point to pause at.
     */
    var priceOverviewDelay: Duration = Duration.ZERO

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
        priceOverviewCalls += marketHashName
        priceOverviewCurrencies += currency
        if (priceOverviewDelay > Duration.ZERO) delay(priceOverviewDelay)
        return priceOverviewResultsByHashName[marketHashName] ?: priceOverviewResult
    }
}
