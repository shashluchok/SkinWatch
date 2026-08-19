package com.shashluchok.skinwatch.domain.catalog

internal interface ItemCatalogRemoteSource {
    suspend fun fetch(category: CatalogCategory): CatalogFetchResult<List<CatalogItem>>

    companion object {
        val EMPTY = object : ItemCatalogRemoteSource {
            override suspend fun fetch(category: CatalogCategory): CatalogFetchResult<List<CatalogItem>> =
                CatalogFetchResult.Success(emptyList())
        }
    }
}
