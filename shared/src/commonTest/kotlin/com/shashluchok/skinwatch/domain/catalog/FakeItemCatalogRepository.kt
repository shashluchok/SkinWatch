package com.shashluchok.skinwatch.domain.catalog

internal class FakeItemCatalogRepository : ItemCatalogRepository {
    val clearCategoryCalls = mutableListOf<CatalogCategory>()
    val insertItemsCalls = mutableListOf<Pair<CatalogCategory, List<CatalogItem>>>()
    val searchCalls = mutableListOf<String>()
    var searchResult: List<CatalogItem> = emptyList()
    var emptyResult = true

    override suspend fun search(query: String): List<CatalogItem> {
        searchCalls += query
        return searchResult
    }

    override suspend fun clearCategory(category: CatalogCategory) {
        clearCategoryCalls += category
    }

    override suspend fun insertItems(category: CatalogCategory, items: List<CatalogItem>) {
        insertItemsCalls += category to items
    }

    override suspend fun isEmpty(): Boolean = emptyResult
}
