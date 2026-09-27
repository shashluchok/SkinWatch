package com.shashluchok.skinwatch.domain.steam

import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * A plain priced answer -- the shared "the fetch worked" fixture. An overview carrying no price at
 * all is a distinct case (Steam answers that way for an item with no live listings) and has to be
 * asked for explicitly, so a test never gets it by accident.
 */
internal val SAMPLE_PRICE_OVERVIEW = SteamPriceOverview(
    lowestPrice = Money(minorUnits = 4900, currency = SteamCurrency.USD),
    medianPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
    volume = 42,
)

internal val SAMPLE_UNPRICED_OVERVIEW = SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null)

internal class FakeSteamMarketRepository(
    override val defaultCurrency: SteamCurrency = SteamCurrency.USD,
) : SteamMarketRepository {
    val priceOverviewCalls = mutableListOf<String>()
    val priceOverviewCurrencies = mutableListOf<SteamCurrency>()
    var priceOverviewResult: SteamMarketResult<SteamPriceOverview> =
        SteamMarketResult.Success(SAMPLE_PRICE_OVERVIEW)

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
