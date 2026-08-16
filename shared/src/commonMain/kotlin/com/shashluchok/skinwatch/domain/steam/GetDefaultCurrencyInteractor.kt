package com.shashluchok.skinwatch.domain.steam

internal class GetDefaultCurrencyInteractor(
    private val steamMarketRepository: SteamMarketRepository,
) {
    operator fun invoke(): SteamCurrency = steamMarketRepository.defaultCurrency
}
