package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertStoredPricesInteractorTest {
    @Test
    fun `on a successful rate lookup, converts all data and returns Success`() = runTest {
        val rates = mapOf(SteamCurrency.USD to 1.08)
        val exchangeRateRepository = FakeExchangeRateRepository(result = ExchangeRateResult.Success(rates))
        val currencyConversionRepository = FakeCurrencyConversionRepository()
        val interactor = ConvertStoredPricesInteractor(
            exchangeRateRepository = exchangeRateRepository,
            currencyConversionRepository = currencyConversionRepository,
        )

        val result = interactor(targetCurrency = SteamCurrency.EUR, newSelectedCurrency = SteamCurrency.EUR)

        assertEquals(ConvertStoredPricesResult.Success, result)
        assertEquals(SteamCurrency.EUR, exchangeRateRepository.lastRequestedBase)
        val call = currencyConversionRepository.convertAllCalls.single()
        assertEquals(rates, call.rates)
        assertEquals(SteamCurrency.EUR, call.targetCurrency)
        assertEquals(SteamCurrency.EUR, call.newSelectedCurrency)
    }

    @Test
    fun `on a failed rate lookup, does not touch stored data and returns the same Failure`() = runTest {
        val exchangeRateRepository = FakeExchangeRateRepository(
            result = ExchangeRateResult.Failure(ExchangeRateError.Network),
        )
        val currencyConversionRepository = FakeCurrencyConversionRepository()
        val interactor = ConvertStoredPricesInteractor(
            exchangeRateRepository = exchangeRateRepository,
            currencyConversionRepository = currencyConversionRepository,
        )

        val result = interactor(targetCurrency = SteamCurrency.EUR, newSelectedCurrency = null)

        assertEquals(ConvertStoredPricesResult.Failure(ExchangeRateError.Network), result)
        assertTrue(currencyConversionRepository.convertAllCalls.isEmpty())
    }
}
