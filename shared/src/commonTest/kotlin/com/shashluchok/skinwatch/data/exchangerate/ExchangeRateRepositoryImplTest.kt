package com.shashluchok.skinwatch.data.exchangerate

import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateError
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateResult
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExchangeRateRepositoryImplTest {
    private val fullRatesJson = buildJsonObject {
        put(
            "eur",
            buildJsonObject {
                put("usd", 1.08)
                put("gbp", 0.85)
                put("eur", 1.0)
                put("rub", 95.3)
            },
        )
    }

    private val incompleteRatesJson = buildJsonObject {
        put("eur", buildJsonObject { put("usd", 1.08) })
    }

    @Test
    fun `returns Success from the primary domain without calling the fallback`() = runTest {
        val primary = FakeExchangeRateApi(result = Result.success(fullRatesJson))
        val fallback = FakeExchangeRateApi(result = Result.failure(IOException("unused")))
        val repository = ExchangeRateRepositoryImpl(primaryApi = primary, fallbackApi = fallback)

        val result = repository.getRates(SteamCurrency.EUR)

        assertIs<ExchangeRateResult.Success<Map<SteamCurrency, Double>>>(result)
        assertEquals(1.08, result.data.getValue(SteamCurrency.USD))
        assertEquals(0.85, result.data.getValue(SteamCurrency.GBP))
        assertEquals(1.0, result.data.getValue(SteamCurrency.EUR))
        assertEquals(95.3, result.data.getValue(SteamCurrency.RUB))
        assertEquals(0, fallback.callCount)
    }

    @Test
    fun `falls back to the second domain when the primary one fails`() = runTest {
        val primary = FakeExchangeRateApi(result = Result.failure(IOException("primary down")))
        val fallback = FakeExchangeRateApi(result = Result.success(fullRatesJson))
        val repository = ExchangeRateRepositoryImpl(primaryApi = primary, fallbackApi = fallback)

        val result = repository.getRates(SteamCurrency.EUR)

        assertIs<ExchangeRateResult.Success<Map<SteamCurrency, Double>>>(result)
        assertEquals(1, primary.callCount)
        assertEquals(1, fallback.callCount)
    }

    @Test
    fun `returns Failure Network when both domains fail`() = runTest {
        val primary = FakeExchangeRateApi(result = Result.failure(IOException("primary down")))
        val fallback = FakeExchangeRateApi(result = Result.failure(IOException("fallback down")))
        val repository = ExchangeRateRepositoryImpl(primaryApi = primary, fallbackApi = fallback)

        val result = repository.getRates(SteamCurrency.EUR)

        assertEquals(ExchangeRateResult.Failure(ExchangeRateError.Network), result)
    }

    @Test
    fun `returns Failure InvalidResponse when a required currency is missing from the response`() = runTest {
        val primary = FakeExchangeRateApi(result = Result.success(incompleteRatesJson))
        val fallback = FakeExchangeRateApi(result = Result.failure(IOException("unused")))
        val repository = ExchangeRateRepositoryImpl(primaryApi = primary, fallbackApi = fallback)

        val result = repository.getRates(SteamCurrency.EUR)

        assertEquals(ExchangeRateResult.Failure(ExchangeRateError.InvalidResponse), result)
    }
}
