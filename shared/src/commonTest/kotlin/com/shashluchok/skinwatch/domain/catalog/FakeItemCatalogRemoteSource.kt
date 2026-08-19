package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.delay
import kotlin.time.Duration

internal class FakeItemCatalogRemoteSource : ItemCatalogRemoteSource {
    val fetchCalls = mutableListOf<CatalogCategory>()
    val resultsByCategory = mutableMapOf<CatalogCategory, CatalogFetchResult<List<CatalogItem>>>()
    var defaultResult: CatalogFetchResult<List<CatalogItem>> = CatalogFetchResult.Success(emptyList())
    var delayDuration: Duration = Duration.ZERO

    override suspend fun fetch(category: CatalogCategory): CatalogFetchResult<List<CatalogItem>> {
        fetchCalls += category
        if (delayDuration > Duration.ZERO) delay(delayDuration)
        return resultsByCategory[category] ?: defaultResult
    }
}
