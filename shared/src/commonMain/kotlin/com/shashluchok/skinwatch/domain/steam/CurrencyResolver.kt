package com.shashluchok.skinwatch.domain.steam

// 21 eurozone members as of 2026-08-12 (Bulgaria joined 2026-01-01) -- verified live via web
// search. This is a plain currency-union fact, not Steam-specific data, kept as a constant rather
// than a pulled-in library.
private val EUROZONE_REGION_CODES = setOf(
    "AT",
    "BE",
    "BG",
    "CY",
    "DE",
    "EE",
    "ES",
    "FI",
    "FR",
    "GR",
    "HR",
    "IE",
    "IT",
    "LT",
    "LU",
    "LV",
    "MT",
    "NL",
    "PT",
    "SI",
    "SK",
)

/**
 * Maps an ISO 3166-1 alpha-2 device region code to the closest supported [SteamCurrency]. `USD`
 * is the deliberate fallback both for `null` (locale unavailable on this platform/target)
 */
internal fun resolveSteamCurrency(regionCode: String?): SteamCurrency = when {
    regionCode == null -> SteamCurrency.USD
    regionCode.equals("RU", ignoreCase = true) -> SteamCurrency.RUB
    regionCode.equals("GB", ignoreCase = true) -> SteamCurrency.GBP
    regionCode.uppercase() in EUROZONE_REGION_CODES -> SteamCurrency.EUR
    else -> SteamCurrency.USD
}
