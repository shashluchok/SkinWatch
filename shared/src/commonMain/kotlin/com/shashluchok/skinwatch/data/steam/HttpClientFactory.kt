package com.shashluchok.skinwatch.data.steam

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A single shared [HttpClient] for all Steam Market calls. CIO is the only Ktor engine available
 * on JVM/Android/Native/JS/WasmJs from one commonMain dependency (verified against
 * https://ktor.io/docs/client-engines.html, 2026-08-12) -- no per-platform expect/actual needed.
 */
internal object HttpClientFactory {
    private const val REQUEST_TIMEOUT_MS = 10_000L
    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val MAX_RETRIES = 3
    private const val SERVER_ERROR_STATUS_MIN = 500
    private const val SERVER_ERROR_STATUS_MAX = 599

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
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }

        install(HttpRequestRetry) {
            maxRetries = MAX_RETRIES
            retryIf { _, response ->
                response.status.value in SERVER_ERROR_RANGE || response.status == HttpStatusCode.TooManyRequests
            }
            exponentialDelay()
        }
    }

    private val SERVER_ERROR_RANGE = SERVER_ERROR_STATUS_MIN..SERVER_ERROR_STATUS_MAX
}
