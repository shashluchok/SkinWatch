package com.shashluchok.skinwatch.domain.catalog

internal interface ItemCatalogRemoteSource {
    /**
     * Streams [category]'s catalog entries in bounded-size chunks via [onChunk] instead of
     * materializing the whole response in memory -- skins_not_grouped.json alone is ~37MB with
     * 20k+ entries and OOMs a normal Android heap if decoded/held as one List.
     */
    suspend fun fetch(category: CatalogCategory, onChunk: suspend (List<CatalogItem>) -> Unit): CatalogFetchResult<Unit>

    companion object {
        val EMPTY = object : ItemCatalogRemoteSource {
            override suspend fun fetch(
                category: CatalogCategory,
                onChunk: suspend (List<CatalogItem>) -> Unit,
            ): CatalogFetchResult<Unit> = CatalogFetchResult.Success(Unit)
        }
    }
}
