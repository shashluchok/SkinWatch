package com.shashluchok.skinwatch.domain.exchangerate

internal sealed interface ConvertStoredPricesResult {
    data object Success : ConvertStoredPricesResult

    data class Failure(
        val error: ExchangeRateError,
    ) : ConvertStoredPricesResult
}
