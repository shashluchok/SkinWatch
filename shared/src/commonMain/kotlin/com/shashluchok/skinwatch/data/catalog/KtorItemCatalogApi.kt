package com.shashluchok.skinwatch.data.catalog

import com.shashluchok.skinwatch.data.catalog.dto.CatalogEntryDto
import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogFetchError
import com.shashluchok.skinwatch.domain.catalog.CatalogFetchResult
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRemoteSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

private const val BASE_URL = "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en"

internal class KtorItemCatalogApi(
    private val httpClient: HttpClient,
) : ItemCatalogRemoteSource {
    override suspend fun fetch(category: CatalogCategory): CatalogFetchResult<List<CatalogItem>> = runCatching {
        httpClient.get("$BASE_URL/${category.fileName}.json").body<List<CatalogEntryDto>>()
    }.fold(
        onSuccess = { entries -> CatalogFetchResult.Success(entries.toCatalogItems(category)) },
        onFailure = { CatalogFetchResult.Failure(it.toCatalogFetchError()) },
    )

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
