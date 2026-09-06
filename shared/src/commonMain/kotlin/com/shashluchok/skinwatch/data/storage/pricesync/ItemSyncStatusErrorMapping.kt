package com.shashluchok.skinwatch.data.storage.pricesync

import com.shashluchok.skinwatch.domain.steam.SteamMarketError

private const val ERROR_TYPE_NETWORK = "network"
private const val ERROR_TYPE_RATE_LIMITED = "rateLimited"
private const val ERROR_TYPE_INVALID_RESPONSE = "invalidResponse"
private const val ERROR_TYPE_UNKNOWN = "unknown"

/**
 * Stable string codes rather than enum names or ordinals: these outlive renaming and reordering of
 * [SteamMarketError], and a build reading a code it does not know must not crash on it.
 */
internal fun steamMarketErrorToType(error: SteamMarketError): String = when (error) {
    SteamMarketError.Network -> ERROR_TYPE_NETWORK
    SteamMarketError.RateLimited -> ERROR_TYPE_RATE_LIMITED
    SteamMarketError.InvalidResponse -> ERROR_TYPE_INVALID_RESPONSE
    is SteamMarketError.Unknown -> ERROR_TYPE_UNKNOWN
}

internal fun steamMarketErrorToMessage(error: SteamMarketError): String? =
    (error as? SteamMarketError.Unknown)?.message

/** An unrecognised code degrades to [SteamMarketError.Unknown] instead of throwing. */
internal fun typeToSteamMarketError(type: String, message: String?): SteamMarketError = when (type) {
    ERROR_TYPE_NETWORK -> SteamMarketError.Network
    ERROR_TYPE_RATE_LIMITED -> SteamMarketError.RateLimited
    ERROR_TYPE_INVALID_RESPONSE -> SteamMarketError.InvalidResponse
    else -> SteamMarketError.Unknown(message = message)
}
