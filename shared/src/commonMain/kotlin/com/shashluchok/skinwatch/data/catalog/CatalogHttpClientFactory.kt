package com.shashluchok.skinwatch.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 60_000L

/**
 * A dedicated client for the catalog dataset. Responses are decoded manually via
 * [kotlinx.serialization.json.io.decodeSourceToSequence] rather than a typed
 * ContentNegotiation body, so no JSON deserializer plugin is installed here.
 */
internal object CatalogHttpClientFactory {
    fun create(): HttpClient = HttpClient(CIO) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }
}
