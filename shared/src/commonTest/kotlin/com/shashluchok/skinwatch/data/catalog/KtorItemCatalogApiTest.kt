package com.shashluchok.skinwatch.data.catalog

import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogFetchResult
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
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

    private fun manyEntriesJson(count: Int): String {
        val entries = (1..count).joinToString(separator = ",") { index ->
            """{"id":"skin-$index","name":"Skin $index","market_hash_name":"Skin $index",""" +
                """"image":"https://example.com/$index.png"}"""
        }
        return "[$entries]"
    }

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
                    // "obviously correct" content type. Manual decoding no longer relies on
                    // ContentNegotiation's content-type matching, so this just documents the
                    // real-world response shape rather than exercising a code path.
                    headers = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8"),
                )
            } else {
                respondError(status = status)
            }
        }
        val httpClient = HttpClient(mockEngine) {
            expectSuccess = true
            install(HttpTimeout)
            install(HttpRequestRetry) { maxRetries = 0 }
        }
        return KtorItemCatalogApi(httpClient)
    }

    @Test
    fun `fetch decodes entries and drops ones with a null market_hash_name`() = runTest {
        val api = apiWithFixedResponse(skinsJson)
        val chunks = mutableListOf<List<CatalogItem>>()

        val result = api.fetch(CatalogCategory.SKIN) { chunk -> chunks += chunk }

        assertTrue(result is CatalogFetchResult.Success)
        val items = chunks.flatten()
        assertEquals(1, items.size)
        assertEquals("AK-47 | Redline (Field-Tested)", items.single().marketHashName)
        assertEquals(CatalogCategory.SKIN, items.single().category)
        assertEquals(
            "https://community.akamai.steamstatic.com/economy/image/abc123",
            items.single().iconUrl,
        )
    }

    @Test
    fun `fetch maps a non-2xx response to a Failure`() = runTest {
        val api = apiWithFixedResponse(json = "", status = HttpStatusCode.NotFound)

        val result = api.fetch(CatalogCategory.SKIN) { }

        assertTrue(result is CatalogFetchResult.Failure)
    }

    @Test
    fun `fetch streams entries across more than one chunk instead of one giant chunk`() = runTest {
        val entryCount = 1200
        val api = apiWithFixedResponse(manyEntriesJson(entryCount))
        val chunks = mutableListOf<List<CatalogItem>>()

        val result = api.fetch(CatalogCategory.SKIN) { chunk -> chunks += chunk }

        assertTrue(result is CatalogFetchResult.Success)
        assertTrue(chunks.size > 1, "expected more than one chunk for $entryCount entries, got ${chunks.size}")
        assertTrue(chunks.all { it.size <= 500 }, "no single chunk should hold the whole response in memory")
        assertEquals(entryCount, chunks.sumOf { it.size })
    }
}
