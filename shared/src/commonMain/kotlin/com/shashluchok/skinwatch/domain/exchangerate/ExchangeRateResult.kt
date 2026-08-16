package com.shashluchok.skinwatch.domain.exchangerate

internal sealed interface ExchangeRateResult<
    out T,
> {
    data class Success<T>(
        val data: T,
    ) : ExchangeRateResult<T>

    data class Failure(
        val error: ExchangeRateError,
    ) : ExchangeRateResult<Nothing>
}
