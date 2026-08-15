package com.shashluchok.skinwatch.domain.watchlist

import kotlin.time.Instant

/** A row the user is tracking but does not own -- no quantity, no purchase price. */
internal data class WatchlistItem(
    val id: Long,
    val marketHashName: String,
    val addedAt: Instant,
)
