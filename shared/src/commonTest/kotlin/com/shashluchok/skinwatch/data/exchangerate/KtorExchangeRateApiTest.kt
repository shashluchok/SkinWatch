package com.shashluchok.skinwatch.data.exchangerate

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
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorExchangeRateApiTest {
    private val responseJson =
        """
        {"date":"2026-08-16","eur":{"usd":1.08,"gbp":0.85,"eur":1,"rub":95.3}}
        """.trimIndent()

    @Test
    fun `getRates decodes the base currency's nested rate object`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val api = KtorExchangeRateApi(httpClient = httpClient, baseUrl = "https://example.com")

        val response = api.getRates(SteamCurrency.EUR)

        val eurRates = response.getValue("eur").jsonObject
        assertEquals(1.08, eurRates.getValue("usd").jsonPrimitive.double)
        assertEquals(0.85, eurRates.getValue("gbp").jsonPrimitive.double)
        assertEquals(95.3, eurRates.getValue("rub").jsonPrimitive.double)
    }
}
