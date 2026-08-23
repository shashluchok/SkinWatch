package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.delay
import kotlin.time.Duration

internal class FakeItemCatalogRemoteSource : ItemCatalogRemoteSource {
    val fetchCalls = mutableListOf<CatalogCategory>()
    val chunksByCategory = mutableMapOf<CatalogCategory, List<List<CatalogItem>>>()
    val resultsByCategory = mutableMapOf<CatalogCategory, CatalogFetchResult<Unit>>()
    var defaultResult: CatalogFetchResult<Unit> = CatalogFetchResult.Success(Unit)
    var delayDuration: Duration = Duration.ZERO

    override suspend fun fetch(
        category: CatalogCategory,
        onChunk: suspend (List<CatalogItem>) -> Unit,
    ): CatalogFetchResult<Unit> {
        fetchCalls += category
        if (delayDuration > Duration.ZERO) delay(delayDuration)
        chunksByCategory[category]?.forEach { chunk -> onChunk(chunk) }
        return resultsByCategory[category] ?: defaultResult
    }
}
