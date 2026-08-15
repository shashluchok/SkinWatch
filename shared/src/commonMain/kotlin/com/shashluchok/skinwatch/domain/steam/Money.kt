package com.shashluchok.skinwatch.domain.steam

/** Always minor units (cents/kopecks) -- never a floating-point amount */
internal data class Money(
    val minorUnits: Long,
    val currency: SteamCurrency,
)
