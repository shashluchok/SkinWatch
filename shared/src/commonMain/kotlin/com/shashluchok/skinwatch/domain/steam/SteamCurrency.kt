package com.shashluchok.skinwatch.domain.steam

// Numeric ids verified live against https://steamcommunity.com/market/priceoverview/ on
// 2026-08-12  -- Valve does
// not publish these ids, they were confirmed by observing real responses per currency.
internal enum class SteamCurrency(
    val id: Int,
) {
    USD(id = 1),
    GBP(id = 2),
    EUR(id = 3),
    RUB(id = 5),
}
