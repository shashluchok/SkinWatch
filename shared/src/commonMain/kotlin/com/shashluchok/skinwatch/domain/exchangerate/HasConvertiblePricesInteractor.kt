package com.shashluchok.skinwatch.domain.exchangerate

internal class HasConvertiblePricesInteractor(
    private val currencyConversionRepository: CurrencyConversionRepository,
) {
    suspend operator fun invoke(): Boolean = currencyConversionRepository.hasConvertibleData()
}
