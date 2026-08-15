package com.shashluchok.skinwatch.domain.steam

internal sealed interface SteamMarketError {
    data object Network : SteamMarketError

    data object RateLimited : SteamMarketError

    data object InvalidResponse : SteamMarketError

    data class Unknown(
        val message: String?,
    ) : SteamMarketError
}
