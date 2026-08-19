package com.shashluchok.skinwatch.domain.catalog

internal interface ItemCatalogRepository {
    suspend fun search(query: String): List<CatalogItem>

    suspend fun replaceCategory(category: CatalogCategory, items: List<CatalogItem>)

    suspend fun isEmpty(): Boolean

    companion object {
        val EMPTY = object : ItemCatalogRepository {
            override suspend fun search(query: String): List<CatalogItem> = emptyList()

            override suspend fun replaceCategory(category: CatalogCategory, items: List<CatalogItem>) = Unit

            override suspend fun isEmpty(): Boolean = true
        }
    }
}
