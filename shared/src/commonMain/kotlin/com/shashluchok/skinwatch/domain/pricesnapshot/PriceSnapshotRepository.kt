package com.shashluchok.skinwatch.domain.pricesnapshot

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant

internal interface PriceSnapshotRepository {
    /**
     * `currency` is passed explicitly rather than read off `overview.lowestPrice?.currency` --
     * both price fields on [SteamPriceOverview] can legitimately be null together (no active
     * listings), and the currency the request was made in must still be recorded. `capturedAt` is
     * likewise caller-supplied rather than read from the system clock inside this repository, so a
     * batch of snapshots taken in the same sync run can share one consistent timestamp instead of
     * drifting a few milliseconds apart per row.
     */
    suspend fun record(
        marketHashName: String,
        overview: SteamPriceOverview,
        currency: SteamCurrency,
        capturedAt: Instant,
    )

    fun observeSnapshots(marketHashName: String): Flow<List<PriceSnapshot>>

    /**
     * Thins out history older than 7 days to 1 reading/day and older than 90 days to
     * 1 reading/week, always keeping the last real reading of each collapsed period -- never a
     * synthetic average.
     */
    suspend fun compactHistory(marketHashName: String, now: Instant)

    companion object {
        val EMPTY = object : PriceSnapshotRepository {
            override suspend fun record(
                marketHashName: String,
                overview: SteamPriceOverview,
                currency: SteamCurrency,
                capturedAt: Instant,
            ) = Unit

            override fun observeSnapshots(marketHashName: String): Flow<List<PriceSnapshot>> = flowOf(emptyList())

            override suspend fun compactHistory(marketHashName: String, now: Instant) = Unit
        }
    }
}
