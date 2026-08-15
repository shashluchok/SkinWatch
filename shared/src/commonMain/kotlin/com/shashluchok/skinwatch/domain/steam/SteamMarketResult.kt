package com.shashluchok.skinwatch.domain.steam

internal sealed interface SteamMarketResult<
    out T,
> {
    data class Success<T>(
        val data: T,
    ) : SteamMarketResult<T>

    data class Failure(
        val error: SteamMarketError,
    ) : SteamMarketResult<Nothing>
}
