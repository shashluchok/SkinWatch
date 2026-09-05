package com.shashluchok.skinwatch.data.catalog

import com.shashluchok.skinwatch.data.catalog.dto.CatalogEntryDto
import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogFetchError
import com.shashluchok.skinwatch.domain.catalog.CatalogFetchResult
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRemoteSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeSourceToSequence

private const val BASE_URL = "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en"

// Kept small so at most this many parsed entries (plus one small IO buffer) are ever live in
// memory at once, regardless of how large a category's source file is.
private const val CHUNK_SIZE = 500

private val catalogJson = Json { ignoreUnknownKeys = true }

internal class KtorItemCatalogApi(
    private val httpClient: HttpClient,
) : ItemCatalogRemoteSource {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun fetch(
        category: CatalogCategory,
        onChunk: suspend (List<CatalogItem>) -> Unit,
    ): CatalogFetchResult<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            httpClient.prepareGet("$BASE_URL/${category.fileName}.json").execute { response: HttpResponse ->
                val source = response.bodyAsChannel().toJsonSource()
                catalogJson
                    .decodeSourceToSequence<CatalogEntryDto>(source)
                    .chunked(CHUNK_SIZE)
                    .forEach { entries -> onChunk(entries.toCatalogItems(category)) }
            }
        }.fold(
            onSuccess = { CatalogFetchResult.Success(Unit) },
            onFailure = { CatalogFetchResult.Failure(it.toCatalogFetchError()) },
        )
    }

    private fun List<CatalogEntryDto>.toCatalogItems(category: CatalogCategory): List<CatalogItem> =
        mapNotNull { entry ->
            val marketHashName = entry.marketHashName ?: return@mapNotNull null
            CatalogItem(
                marketHashName = marketHashName,
                displayName = entry.name,
                iconUrl = entry.image,
                category = category,
            )
        }

    private fun Throwable.toCatalogFetchError(): CatalogFetchError = when (this) {
        is CancellationException -> throw this
        is ClientRequestException, is ServerResponseException, is HttpRequestTimeoutException, is IOException ->
            CatalogFetchError.Network
        is SerializationException -> CatalogFetchError.InvalidResponse
        else -> CatalogFetchError.Unknown(message)
    }

    private val CatalogCategory.fileName: String
        get() = when (this) {
            CatalogCategory.SKIN -> "skins_not_grouped"
            CatalogCategory.STICKER -> "stickers"
            CatalogCategory.STICKER_SLAB -> "sticker_slabs"
            CatalogCategory.KEYCHAIN -> "keychains"
            CatalogCategory.CASE -> "crates"
            CatalogCategory.KEY -> "keys"
            CatalogCategory.AGENT -> "agents"
            CatalogCategory.PATCH -> "patches"
            CatalogCategory.GRAFFITI -> "graffiti"
            CatalogCategory.MUSIC_KIT -> "music_kits"
            CatalogCategory.COLLECTIBLE -> "collectibles"
        }
}
