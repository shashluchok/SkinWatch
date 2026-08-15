package com.shashluchok.skinwatch.domain.steam

/**
 * One search result from `market/search/render`. `sellPrice` is null when Steam reports zero
 * active listings for the item (observed for low-volume items).
 */
internal data class SteamMarketItem(
    val marketHashName: String,
    val displayName: String,
    val iconUrl: String,
    val sellListingsCount: Int,
    val sellPrice: Money?,
)
