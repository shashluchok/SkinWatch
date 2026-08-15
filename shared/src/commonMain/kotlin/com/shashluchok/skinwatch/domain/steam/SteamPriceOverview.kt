package com.shashluchok.skinwatch.domain.steam

/**
 * All three fields are independently nullable: Steam's `priceoverview` returns `success: true`
 * with none of them present for items with no current listings -- this is not an error state.
 */
internal data class SteamPriceOverview(
    val lowestPrice: Money?,
    val medianPrice: Money?,
    val volume: Int?,
)
