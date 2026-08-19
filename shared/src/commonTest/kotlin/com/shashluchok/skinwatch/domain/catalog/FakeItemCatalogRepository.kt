package com.shashluchok.skinwatch.domain.catalog

internal class FakeItemCatalogRepository : ItemCatalogRepository {
    val replaceCategoryCalls = mutableListOf<Pair<CatalogCategory, List<CatalogItem>>>()
    var searchResult: List<CatalogItem> = emptyList()
    var emptyResult = true

    override suspend fun search(query: String): List<CatalogItem> = searchResult

    override suspend fun replaceCategory(category: CatalogCategory, items: List<CatalogItem>) {
        replaceCategoryCalls += category to items
    }

    override suspend fun isEmpty(): Boolean = emptyResult
}
