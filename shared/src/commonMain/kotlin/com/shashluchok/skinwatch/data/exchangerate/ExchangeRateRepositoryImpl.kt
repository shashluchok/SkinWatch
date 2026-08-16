package com.shashluchok.skinwatch.data.exchangerate

import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateError
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateRepository
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateResult
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import io.ktor.client.engine.cio.FailToConnectException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tries [primaryApi] first; only calls [fallbackApi] when it fails -- the two-domain fallback the
 * source itself recommends (jsdelivr CDN primary, Cloudflare Pages fallback), applied at the
 * repository level rather than as HTTP-client retries against the same address.
 */
internal class ExchangeRateRepositoryImpl(
    private val primaryApi: ExchangeRateApi,
    private val fallbackApi: ExchangeRateApi,
) : ExchangeRateRepository {
    override suspend fun getRates(base: SteamCurrency): ExchangeRateResult<Map<SteamCurrency, Double>> {
        val jsonResult = runCatching { primaryApi.getRates(base) }
            .recoverCatching { primaryError ->
                if (primaryError is CancellationException) throw primaryError
                fallbackApi.getRates(base)
            }

        return jsonResult.fold(
            onSuccess = { json ->
                json
                    .toRatesMap(base)
                    ?.let { ExchangeRateResult.Success(it) }
                    ?: ExchangeRateResult.Failure(ExchangeRateError.InvalidResponse)
            },
            onFailure = { ExchangeRateResult.Failure(it.toExchangeRateError()) },
        )
    }
}

private fun Throwable.toExchangeRateError(): ExchangeRateError = when (this) {
    // Must be checked first: runCatching also catches CancellationException, and converting it to
    // a Failure instead of rethrowing would break structured concurrency.
    is CancellationException -> throw this
    is HttpRequestTimeoutException,
    is IOException,
    // CIO's own connection-establishment failure (DNS resolution / retry attempts exhausted) --
    // does not extend IOException, so it needs its own branch, see Endpoint.kt in ktor-client-cio.
    is FailToConnectException,
    -> ExchangeRateError.Network
    is SerializationException -> ExchangeRateError.InvalidResponse
    else -> ExchangeRateError.Unknown(message)
}

/**
 * [base]'s own code (e.g. "eur") is the JSON key for the nested rates object, not a fixed field
 * name -- see [ExchangeRateApi.getRates]. Returns null (mapped to [ExchangeRateError.InvalidResponse]
 * by the caller) if the nested object or any of the 4 currencies this project supports is missing.
 */
private fun JsonObject.toRatesMap(base: SteamCurrency): Map<SteamCurrency, Double>? {
    val baseRates = this[base.name.lowercase()]?.jsonObject ?: return null
    val rates = SteamCurrency.entries.mapNotNull { currency ->
        val rate = baseRates[currency.name.lowercase()]?.jsonPrimitive?.doubleOrNull
        rate?.let { currency to it }
    }
    return rates.takeIf { it.size == SteamCurrency.entries.size }?.toMap()
}
