package com.shashluchok.skinwatch.domain.exchangerate

internal sealed interface ExchangeRateError {
    data object Network : ExchangeRateError

    data object InvalidResponse : ExchangeRateError

    data class Unknown(
        val message: String?,
    ) : ExchangeRateError
}
