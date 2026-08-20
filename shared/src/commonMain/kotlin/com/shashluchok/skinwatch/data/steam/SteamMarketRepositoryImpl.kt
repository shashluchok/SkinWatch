package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.data.steam.dto.PriceOverviewResponseDto
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import com.shashluchok.skinwatch.domain.steam.resolveSteamCurrency
import io.ktor.client.engine.cio.FailToConnectException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

internal class SteamMarketRepositoryImpl(
    private val api: SteamMarketApi,
    private val rateLimiter: SteamRateLimiter,
    private val deviceRegionCode: () -> String?,
) : SteamMarketRepository {
    override val defaultCurrency: SteamCurrency
        get() = resolveSteamCurrency(deviceRegionCode())

    override suspend fun getPriceOverview(
        marketHashName: String,
        currency: SteamCurrency,
    ): SteamMarketResult<SteamPriceOverview> = runCatching {
        rateLimiter.awaitTurn(SteamEndpoint.PRICE_OVERVIEW)
        val dto = api.getPriceOverview(marketHashName = marketHashName, currency = currency)
        // toDomain is called here, inside runCatching, so a malformed price string (thrown by
        // SteamPriceParser as IllegalArgumentException) is caught below rather than propagating.
        if (dto.success) dto.toDomain(currency) else null
    }.fold(
        onSuccess = { overview ->
            if (overview != null) {
                SteamMarketResult.Success(overview)
            } else {
                SteamMarketResult.Failure(SteamMarketError.InvalidResponse)
            }
        },
        onFailure = { SteamMarketResult.Failure(it.toSteamMarketError()) },
    )

    private fun PriceOverviewResponseDto.toDomain(currency: SteamCurrency): SteamPriceOverview = SteamPriceOverview(
        lowestPrice = lowestPrice?.let { SteamPriceParser.parse(text = it, currency = currency) },
        medianPrice = medianPrice?.let { SteamPriceParser.parse(text = it, currency = currency) },
        // Steam formats volume with the same thousands separator as prices (e.g. "1,234") once an
        // item trades above 999/day -- strip it before parsing, otherwise it silently reads as null.
        volume = volume?.replace(",", "")?.toIntOrNull(),
    )

    private fun Throwable.toSteamMarketError(): SteamMarketError = when (this) {
        // Must be checked first: runCatching also catches CancellationException, and converting it
        // to a Failure instead of rethrowing would break structured concurrency.
        is CancellationException -> throw this
        is ClientRequestException -> if (response.status == HttpStatusCode.TooManyRequests) {
            SteamMarketError.RateLimited
        } else {
            SteamMarketError.InvalidResponse
        }
        is ServerResponseException,
        is HttpRequestTimeoutException,
        is IOException,
        // CIO's own connection-establishment failure (DNS resolution / retry attempts exhausted) --
        // does not extend IOException, so it needs its own branch, see Endpoint.kt in
        // ktor-client-cio.
        is FailToConnectException,
        -> SteamMarketError.Network
        is SerializationException -> SteamMarketError.InvalidResponse
        // SteamPriceParser.parse throws this for malformed/unexpected price strings.
        is IllegalArgumentException -> SteamMarketError.InvalidResponse
        else -> SteamMarketError.Unknown(message)
    }
}
