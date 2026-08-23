package com.shashluchok.skinwatch.domain.catalog

internal interface ItemCatalogRepository {
    suspend fun search(query: String): List<CatalogItem>

    suspend fun clearCategory(category: CatalogCategory)

    suspend fun insertItems(category: CatalogCategory, items: List<CatalogItem>)

    suspend fun isEmpty(): Boolean

    companion object {
        val EMPTY = object : ItemCatalogRepository {
            override suspend fun search(query: String): List<CatalogItem> = emptyList()

            override suspend fun clearCategory(category: CatalogCategory) = Unit

            override suspend fun insertItems(category: CatalogCategory, items: List<CatalogItem>) = Unit

            override suspend fun isEmpty(): Boolean = true
        }
    }
}
