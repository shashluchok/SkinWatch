package com.shashluchok.skinwatch.domain.pricesnapshot

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.time.Instant

/** A single point-in-time price reading for a market item, independent of ownership status. */
internal data class PriceSnapshot(
    val marketHashName: String,
    val currency: SteamCurrency,
    val lowestPrice: Money?,
    val medianPrice: Money?,
    val volume: Int?,
    val capturedAt: Instant,
)
