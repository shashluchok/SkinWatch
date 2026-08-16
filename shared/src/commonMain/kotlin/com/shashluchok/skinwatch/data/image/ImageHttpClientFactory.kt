package com.shashluchok.skinwatch.data.image

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

/**
 * A standalone [HttpClient] for Coil's [Ktor network fetcher](https://coil-kt.github.io/coil/network/),
 * deliberately not the shared client from `data.steam.HttpClientFactory`. That client's retry policy
 * (`HttpRequestRetry`, exponential backoff on 5xx/429) and `expectSuccess = true` are tuned for the
 * Steam Market API contract, not for best-effort thumbnail loading -- a missing/broken icon should
 * fail once and let Coil show its placeholder, not retry three times or throw on a non-2xx status.
 * CIO is reused as the engine since it already covers every KMP target this project ships
 * (JVM/Android/Native/JS/WasmJs) from a single commonMain dependency, same as `HttpClientFactory`.
 */
internal object ImageHttpClientFactory {
    private const val REQUEST_TIMEOUT_MS = 15_000L

    fun create(): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }
}
