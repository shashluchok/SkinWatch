package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency

internal class FakeCurrencyConversionRepository(
    private var hasData: Boolean = false,
) : CurrencyConversionRepository {
    data class ConvertAllCall(
        val rates: Map<SteamCurrency, Double>,
        val targetCurrency: SteamCurrency,
        val newSelectedCurrency: SteamCurrency?,
    )

    val convertAllCalls = mutableListOf<ConvertAllCall>()

    override suspend fun hasConvertibleData(): Boolean = hasData

    override suspend fun convertAll(
        rates: Map<SteamCurrency, Double>,
        targetCurrency: SteamCurrency,
        newSelectedCurrency: SteamCurrency?,
    ) {
        convertAllCalls += ConvertAllCall(
            rates = rates,
            targetCurrency = targetCurrency,
            newSelectedCurrency = newSelectedCurrency,
        )
    }
}
