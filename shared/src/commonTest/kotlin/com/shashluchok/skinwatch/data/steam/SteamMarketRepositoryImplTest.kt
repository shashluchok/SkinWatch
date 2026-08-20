package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class SteamMarketRepositoryImplTest {
    private fun repositoryWithEngine(
        mockEngine: MockEngine,
        deviceRegionCode: () -> String? = { "US" },
    ): SteamMarketRepositoryImpl {
        val httpClient = HttpClient(mockEngine) {
            // Matches HttpClientFactory's real config: expectSuccess = true is what makes a
            // non-2xx status (after retries are exhausted) surface as ClientRequestException
            // instead of ContentNegotiation trying and failing to decode an empty error body.
            // ignoreUnknownKeys/isLenient mirror the same Json settings so responses that add
            // fields Steam doesn't strictly need for this DTO shape don't fail decoding.
            expectSuccess = true
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { _, response -> response.status == HttpStatusCode.TooManyRequests }
                constantDelay(1L)
            }
        }
        val api = KtorSteamMarketApi(httpClient)
        val rateLimiter = SteamRateLimiter(minInterval = { 0.seconds })
        return SteamMarketRepositoryImpl(api = api, rateLimiter = rateLimiter, deviceRegionCode = deviceRegionCode)
    }

    @Test
    fun `defaultCurrency resolves from the injected device region, not the real platform locale`() {
        val repository = repositoryWithEngine(
            mockEngine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.OK) },
            deviceRegionCode = { "RU" },
        )

        assertEquals(SteamCurrency.RUB, repository.defaultCurrency)
    }

    @Test
    fun `defaultCurrency falls back to USD when the device region is unavailable`() {
        val repository = repositoryWithEngine(
            mockEngine = MockEngine { _ -> respond(content = "", status = HttpStatusCode.OK) },
            deviceRegionCode = { null },
        )

        assertEquals(SteamCurrency.USD, repository.defaultCurrency)
    }

    @Test
    fun `getPriceOverview maps a full response to domain Money`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"success":true,"lowest_price":"$51.93","volume":"182","median_price":"$52.98"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = repositoryWithEngine(mockEngine)

        val result = repository.getPriceOverview(
            marketHashName = "AK-47 | Inheritance (Field-Tested)",
            currency = SteamCurrency.USD,
        )

        val success = assertIs<SteamMarketResult.Success<SteamPriceOverview>>(result)
        assertEquals(Money(minorUnits = 5193, currency = SteamCurrency.USD), success.data.lowestPrice)
        assertEquals(Money(minorUnits = 5298, currency = SteamCurrency.USD), success.data.medianPrice)
        assertEquals(182, success.data.volume)
    }

    @Test
    fun `getPriceOverview with no active listings returns Success with null fields`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = repositoryWithEngine(mockEngine)

        val result = repository.getPriceOverview(marketHashName = "Nonexistent Item", currency = SteamCurrency.USD)

        val success = assertIs<SteamMarketResult.Success<SteamPriceOverview>>(result)
        assertEquals(null, success.data.lowestPrice)
        assertEquals(null, success.data.medianPrice)
        assertEquals(null, success.data.volume)
    }

    @Test
    fun `getPriceOverview maps a malformed price string to InvalidResponse`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"success":true,"lowest_price":"not a price"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = repositoryWithEngine(mockEngine)

        val result = repository.getPriceOverview(marketHashName = "Anything", currency = SteamCurrency.USD)

        val failure = assertIs<SteamMarketResult.Failure>(result)
        assertEquals(SteamMarketError.InvalidResponse, failure.error)
    }

    @Test
    fun `getPriceOverview parses a comma-formatted volume for high-traffic items`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"success":true,"lowest_price":"$51.93","volume":"1,234","median_price":"$52.98"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = repositoryWithEngine(mockEngine)

        val result = repository.getPriceOverview(marketHashName = "Anything", currency = SteamCurrency.USD)

        val success = assertIs<SteamMarketResult.Success<SteamPriceOverview>>(result)
        assertEquals(1234, success.data.volume)
    }

    @Test
    fun `getPriceOverview rethrows cancellation instead of turning it into a Failure`() = runTest {
        // Simulates what a cancelled calling scope looks like from inside the suspending HTTP
        // call: the underlying transport throws CancellationException. runCatching would
        // otherwise swallow it and hand it to toSteamMarketError like any other failure.
        val mockEngine = MockEngine { _ -> throw CancellationException("scope cancelled") }
        val repository = repositoryWithEngine(mockEngine)

        assertFailsWith<CancellationException> {
            repository.getPriceOverview(marketHashName = "Anything", currency = SteamCurrency.USD)
        }
    }

    @Test
    fun `getPriceOverview maps success false to InvalidResponse`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"success":false}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = repositoryWithEngine(mockEngine)

        val result = repository.getPriceOverview(marketHashName = "Anything", currency = SteamCurrency.USD)

        val failure = assertIs<SteamMarketResult.Failure>(result)
        assertEquals(SteamMarketError.InvalidResponse, failure.error)
    }

    @Test
    fun `getPriceOverview maps exhausted 429 retries to RateLimited`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.TooManyRequests)
        }
        val repository = repositoryWithEngine(mockEngine)

        val result = repository.getPriceOverview(marketHashName = "Anything", currency = SteamCurrency.USD)

        val failure = assertIs<SteamMarketResult.Failure>(result)
        assertEquals(SteamMarketError.RateLimited, failure.error)
    }
}
