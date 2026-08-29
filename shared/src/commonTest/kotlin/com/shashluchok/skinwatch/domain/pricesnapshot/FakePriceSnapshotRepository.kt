package com.shashluchok.skinwatch.domain.pricesnapshot

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

internal class FakePriceSnapshotRepository : PriceSnapshotRepository {
    private val snapshotFlows = mutableMapOf<String, MutableStateFlow<List<PriceSnapshot>>>()
    val observeCallCounts = mutableMapOf<String, Int>()
    val recorded = mutableListOf<PriceSnapshot>()
    val compactHistoryCalls = mutableListOf<String>()

    fun emitSnapshot(
        marketHashName: String,
        lowestPrice: Money?,
        capturedAt: Instant,
    ) {
        val flow = snapshotFlows.getOrPut(marketHashName) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + PriceSnapshot(
            marketHashName = marketHashName,
            currency = lowestPrice?.currency ?: SteamCurrency.USD,
            lowestPrice = lowestPrice,
            medianPrice = null,
            volume = null,
            capturedAt = capturedAt,
        )
    }

    override suspend fun record(
        marketHashName: String,
        overview: SteamPriceOverview,
        currency: SteamCurrency,
        capturedAt: Instant,
    ) {
        recorded += PriceSnapshot(
            marketHashName = marketHashName,
            currency = currency,
            lowestPrice = overview.lowestPrice,
            medianPrice = overview.medianPrice,
            volume = overview.volume,
            capturedAt = capturedAt,
        )
        val flow = snapshotFlows.getOrPut(marketHashName) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + recorded.last()
    }

    override fun observeSnapshots(marketHashName: String): Flow<List<PriceSnapshot>> {
        observeCallCounts[marketHashName] = (observeCallCounts[marketHashName] ?: 0) + 1
        return snapshotFlows.getOrPut(marketHashName) { MutableStateFlow(emptyList()) }
    }

    override suspend fun compactHistory(marketHashName: String, now: Instant) {
        compactHistoryCalls += marketHashName
    }
}
