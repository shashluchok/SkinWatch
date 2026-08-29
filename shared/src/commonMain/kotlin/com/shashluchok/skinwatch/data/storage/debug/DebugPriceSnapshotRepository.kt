package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.Instant

private val DEBUG_CURRENCY = SteamCurrency.RUB

/**
 * In-memory stand-in for [PriceSnapshotRepository], pre-populated with [debugScenarios]
 */
internal class DebugPriceSnapshotRepository : PriceSnapshotRepository {
    private val snapshotsByMarketHashName: Map<String, List<PriceSnapshot>> =
        debugScenarios(Clock.System.now()).associate { scenario ->
            scenario.marketHashName to scenario.snapshots.map { (capturedAt, priceMinorUnits) ->
                priceMinorUnits.toSnapshot(marketHashName = scenario.marketHashName, capturedAt = capturedAt)
            }
        }

    override suspend fun record(
        marketHashName: String,
        overview: SteamPriceOverview,
        currency: SteamCurrency,
        capturedAt: Instant,
    ) = Unit

    override fun observeSnapshots(marketHashName: String): Flow<List<PriceSnapshot>> =
        flowOf(snapshotsByMarketHashName[marketHashName].orEmpty())

    override suspend fun compactHistory(marketHashName: String, now: Instant) = Unit

    private fun Long?.toSnapshot(
        marketHashName: String,
        capturedAt: Instant,
    ): PriceSnapshot {
        val price = this?.let { Money(minorUnits = it, currency = DEBUG_CURRENCY) }
        return PriceSnapshot(
            marketHashName = marketHashName,
            currency = DEBUG_CURRENCY,
            lowestPrice = price,
            medianPrice = price,
            volume = null,
            capturedAt = capturedAt,
        )
    }
}
