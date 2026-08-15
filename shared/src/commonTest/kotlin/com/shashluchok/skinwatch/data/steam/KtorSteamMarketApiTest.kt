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
    private val searchResponseJson =
        """
        {"success":true,"start":0,"pagesize":10,"total_count":597,
        "results":[{"name":"AK-47 | Inheritance (Field-Tested)",
        "hash_name":"AK-47 | Inheritance (Field-Tested)","sell_listings":835,
        "sell_price":5193,"sell_price_text":"${'$'}51.93",
        "asset_description":{"icon_url":"i0CoZ81Ui0m-9KwlBY1L",
        "market_hash_name":"AK-47 | Inheritance (Field-Tested)"}}]}
        """.trimIndent()

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
        // Matches HttpClientFactory's real config: the fixtures below carry fields
        // (e.g. "start", "pagesize", "sell_price_text") the DTOs deliberately don't model.
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return KtorSteamMarketApi(httpClient)
    }

    @Test
    fun `searchItems decodes a real search-render response`() = runTest {
        val api = apiWithFixedResponse(searchResponseJson)

        val response = api.searchItems(
            query = "AK-47",
            currency = SteamCurrency.USD,
            count = 10,
            start = 0,
        )

        assertEquals(true, response.success)
        assertEquals(1, response.results.size)
        assertEquals("AK-47 | Inheritance (Field-Tested)", response.results.first().hashName)
        assertEquals(5193L, response.results.first().sellPrice)
        assertEquals(
            "i0CoZ81Ui0m-9KwlBY1L",
            response.results
                .first()
                .assetDescription.iconUrl,
        )
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
