package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.data.steam.dto.PriceOverviewResponseDto
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import com.shashluchok.skinwatch.domain.steam.resolveSteamCurrency
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.domain.synclog.error
import com.shashluchok.skinwatch.domain.synclog.info
import io.ktor.client.engine.cio.FailToConnectException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.time.TimeSource

internal class SteamMarketRepositoryImpl(
    private val api: SteamMarketApi,
    private val rateLimiter: SteamRateLimiter,
    private val deviceRegionCode: () -> String?,
    private val syncLog: SyncLogRepository = SyncLogRepository.EMPTY,
) : SteamMarketRepository {
    override val defaultCurrency: SteamCurrency
        get() = resolveSteamCurrency(deviceRegionCode())

    override suspend fun getPriceOverview(
        marketHashName: String,
        currency: SteamCurrency,
    ): SteamMarketResult<SteamPriceOverview> = runCatching {
        awaitThrottle(marketHashName)
        val requestMark = TimeSource.Monotonic.markNow()
        val dto = api.getPriceOverview(marketHashName = marketHashName, currency = currency)
        syncLog.info(
            tag = SyncLogTag.HTTP,
            message = "responded in ${requestMark.elapsedNow()}: success=${dto.success}, " +
                "lowest=${dto.lowestPrice}, median=${dto.medianPrice}, volume=${dto.volume}",
            marketHashName = marketHashName,
        )
        // toDomain is called here, inside runCatching, so a malformed price string (thrown by
        // SteamPriceParser as SteamPriceFormatException) is caught below rather than propagating.
        if (dto.success) dto.toDomain(currency) else null
    }.fold(
        onSuccess = { overview ->
            if (overview != null) {
                SteamMarketResult.Success(overview)
            } else {
                syncLog.error(
                    tag = SyncLogTag.HTTP,
                    message = "Steam answered with success=false, mapping to InvalidResponse",
                    marketHashName = marketHashName,
                )
                SteamMarketResult.Failure(SteamMarketError.InvalidResponse)
            }
        },
        onFailure = { throwable ->
            // Recorded before the mapping, which rethrows cancellation rather than returning from it.
            if (throwable is CancellationException) {
                syncLog.error(
                    tag = SyncLogTag.HTTP,
                    message = "request cancelled by its caller's scope: ${throwable.message}",
                    marketHashName = marketHashName,
                )
            }
            val error = throwable.toSteamMarketError()
            syncLog.error(
                tag = SyncLogTag.HTTP,
                message = "${throwable::class.simpleName}: ${throwable.message}" +
                    ((throwable as? ResponseException)?.let { " (HTTP ${it.response.status})" } ?: "") +
                    " -> $error",
                marketHashName = marketHashName,
            )
            SteamMarketResult.Failure(error)
        },
    )

    /**
     * The throttle is shared with every other caller, so a request queued behind a running sync can
     * wait far longer than the request itself takes -- worth telling apart from a slow network.
     */
    private suspend fun awaitThrottle(marketHashName: String) {
        val throttleMark = TimeSource.Monotonic.markNow()
        rateLimiter.awaitTurn(SteamEndpoint.PRICE_OVERVIEW)
        syncLog.info(
            tag = SyncLogTag.LIMITER,
            message = "waited ${throttleMark.elapsedNow()} for a turn on ${SteamEndpoint.PRICE_OVERVIEW}",
            marketHashName = marketHashName,
        )
    }

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
        is SteamPriceFormatException -> SteamMarketError.InvalidResponse
        // Anything unrecognised is reported as unknown rather than guessed at, and unknown is
        // retryable: a transport failure this list has not learned about yet must cost one more
        // attempt, not a verdict. There used to be a blanket `is IllegalArgumentException` branch
        // here for the parser, and it swallowed UnresolvedAddressException -- a plain DNS failure --
        // reporting a whole inventory as permanently unpriceable.
        else -> SteamMarketError.Unknown("${this::class.simpleName}: $message")
    }
}
