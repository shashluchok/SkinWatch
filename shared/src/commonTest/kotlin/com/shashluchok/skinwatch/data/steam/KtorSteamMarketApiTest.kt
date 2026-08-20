package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorSteamMarketApiTest {
    private val priceOverviewJson =
        """
        {"success":true,"lowest_price":"${'$'}51.93","volume":"182","median_price":"${'$'}52.98"}
        """.trimIndent()

    private fun apiWithFixedResponse(json: String): SteamMarketApi {
        val mockEngine = MockEngine { _ ->
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        // Matches HttpClientFactory's real config: Steam adds new response fields over time,
        // and unknown ones should be ignored rather than fail decoding.
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return KtorSteamMarketApi(httpClient)
    }

    @Test
    fun `getPriceOverview decodes a real priceoverview response`() = runTest {
        val api = apiWithFixedResponse(priceOverviewJson)

        val response = api.getPriceOverview(
            marketHashName = "AK-47 | Inheritance (Field-Tested)",
            currency = SteamCurrency.USD,
        )

        assertEquals(true, response.success)
        assertEquals("$51.93", response.lowestPrice)
        assertEquals("182", response.volume)
        assertEquals("$52.98", response.medianPrice)
    }
}
