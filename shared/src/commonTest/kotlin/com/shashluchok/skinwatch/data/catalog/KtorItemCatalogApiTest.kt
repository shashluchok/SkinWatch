package com.shashluchok.skinwatch.data.catalog

import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogFetchResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorItemCatalogApiTest {
    // Real shape observed live from raw.githubusercontent.com/ByMykel/CSGO-API's per-category
    // files -- market_hash_name is null for non-tradable entries (event items, achievement coins)
    // and must be dropped, not crash decoding.
    private val skinsJson =
        """
        [
          {"id":"skin-1","name":"AK-47 | Redline (Field-Tested)","market_hash_name":"AK-47 | Redline (Field-Tested)",
           "image":"https://community.akamai.steamstatic.com/economy/image/abc123","weapon":{"id":"ak47"}},
          {"id":"skin-2","name":"Not Tradable Thing","market_hash_name":null,
           "image":"https://community.akamai.steamstatic.com/economy/image/def456"}
        ]
        """.trimIndent()

    private fun apiWithFixedResponse(
        json: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): KtorItemCatalogApi {
        val mockEngine = MockEngine { _ ->
            if (status == HttpStatusCode.OK) {
                respond(
                    content = json,
                    status = status,
                    // raw.githubusercontent.com serves .json files as text/plain, not
                    // application/json (confirmed live) -- the mock mirrors that, not the
                    // "obviously correct" content type, so this test actually catches a
                    // ContentNegotiation misconfiguration instead of masking it.
                    headers = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8"),
                )
            } else {
                respondError(status = status)
            }
        }
        val httpClient = HttpClient(mockEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                val catalogJson = Json { ignoreUnknownKeys = true }
                json(catalogJson)
                json(catalogJson, contentType = ContentType.Text.Plain)
            }
            install(HttpTimeout)
            install(HttpRequestRetry) { maxRetries = 0 }
        }
        return KtorItemCatalogApi(httpClient)
    }

    @Test
    fun `fetch decodes entries and drops ones with a null market_hash_name`() = runTest {
        val api = apiWithFixedResponse(skinsJson)

        val result = api.fetch(CatalogCategory.SKIN)

        assertTrue(result is CatalogFetchResult.Success)
        assertEquals(1, result.data.size)
        assertEquals("AK-47 | Redline (Field-Tested)", result.data.single().marketHashName)
        assertEquals(CatalogCategory.SKIN, result.data.single().category)
        assertEquals(
            "https://community.akamai.steamstatic.com/economy/image/abc123",
            result.data.single().iconUrl,
        )
    }

    @Test
    fun `fetch maps a non-2xx response to a Failure`() = runTest {
        val api = apiWithFixedResponse(json = "", status = HttpStatusCode.NotFound)

        val result = api.fetch(CatalogCategory.SKIN)

        assertTrue(result is CatalogFetchResult.Failure)
    }
}
