package com.shashluchok.skinwatch.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 60_000L

/**
 * A dedicated client for the catalog dataset
 */
internal object CatalogHttpClientFactory {
    fun create(): HttpClient = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            val catalogJson = Json { ignoreUnknownKeys = true }
            json(catalogJson)
            json(catalogJson, contentType = ContentType.Text.Plain)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }
}
