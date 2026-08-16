package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency

internal class ConvertStoredPricesInteractor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val currencyConversionRepository: CurrencyConversionRepository,
) {
    suspend operator fun invoke(
        targetCurrency: SteamCurrency,
        newSelectedCurrency: SteamCurrency?,
    ): ConvertStoredPricesResult = when (val result = exchangeRateRepository.getRates(base = targetCurrency)) {
        is ExchangeRateResult.Success -> {
            currencyConversionRepository.convertAll(
                rates = result.data,
                targetCurrency = targetCurrency,
                newSelectedCurrency = newSelectedCurrency,
            )
            ConvertStoredPricesResult.Success
        }
        is ExchangeRateResult.Failure -> ConvertStoredPricesResult.Failure(result.error)
    }
}
