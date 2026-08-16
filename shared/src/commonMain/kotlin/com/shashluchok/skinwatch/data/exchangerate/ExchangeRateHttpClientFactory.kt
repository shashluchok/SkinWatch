package com.shashluchok.skinwatch.data.exchangerate

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A standalone [HttpClient] for the exchange-rate source, deliberately not the shared client from
 * `data.steam.HttpClientFactory`. That client's `HttpRequestRetry` plugin retries the same URL --
 * resilience here instead comes from `ExchangeRateRepositoryImpl` falling back to a second,
 * independent domain, so a same-URL retry loop on top of that would be redundant. `ignoreUnknownKeys`
 * matters more here than for Steam: a single exchange-rate response carries dozens of currencies this
 * project never reads.
 */
internal object ExchangeRateHttpClientFactory {
    private const val REQUEST_TIMEOUT_MS = 10_000L

    fun create(): HttpClient = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }
}
