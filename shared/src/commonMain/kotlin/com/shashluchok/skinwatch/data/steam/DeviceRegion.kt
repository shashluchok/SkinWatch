package com.shashluchok.skinwatch.data.steam

/**
 * ISO 3166-1 alpha-2 device region code (e.g. "US", "RU", "DE"), or null if unavailable on this
 * platform/target. Feeds `resolveSteamCurrency` in `domain.steam` for default-currency resolution.
 */
internal expect fun currentDeviceRegionCode(): String?
