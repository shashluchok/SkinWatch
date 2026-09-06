package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import kotlin.time.Instant

/**
 * How the last price fetch for one `marketHashName` went.
 *
 * Keyed by hash rather than by inventory item id because one fetch serves every owned item sharing
 * that hash -- duplicates of the same skin therefore share a status.
 *
 * [lastSuccessAt] survives failures on purpose: it is what decides whether an item is due for
 * another attempt, and a failed attempt must not make a stale price look freshly synced.
 */
internal sealed interface ItemSyncStatus {
    val attemptedAt: Instant
    val lastSuccessAt: Instant?

    data class Synced(
        override val attemptedAt: Instant,
    ) : ItemSyncStatus {
        override val lastSuccessAt: Instant get() = attemptedAt
    }

    data class Failed(
        override val attemptedAt: Instant,
        override val lastSuccessAt: Instant?,
        val error: SteamMarketError,
    ) : ItemSyncStatus
}
