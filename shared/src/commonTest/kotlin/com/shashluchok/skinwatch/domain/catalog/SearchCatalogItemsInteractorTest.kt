package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchCatalogItemsInteractorTest {
    private val catalogRepository = FakeItemCatalogRepository()
    private val interactor = SearchCatalogItemsInteractor(catalogRepository = catalogRepository)

    private fun catalogItem(name: String) = CatalogItem(
        marketHashName = name,
        displayName = name,
        iconUrl = "https://example.com/$name.png",
        category = CatalogCategory.SKIN,
    )

    @Test
    fun `forwards the query and returns Loaded with non-empty results`() = runTest {
        catalogRepository.searchResult = listOf(catalogItem("AK-47 | Redline"))
        catalogRepository.emptyResult = false

        val result = interactor(query = "Redline")

        assertEquals(listOf("Redline"), catalogRepository.searchCalls)
        val loaded = assertIs<SearchCatalogItemsInteractor.Result.Loaded>(result)
        assertEquals(listOf(catalogItem("AK-47 | Redline")), loaded.items)
    }

    @Test
    fun `empty results with a non-empty catalog is Loaded with an empty list`() = runTest {
        catalogRepository.searchResult = emptyList()
        catalogRepository.emptyResult = false

        val result = interactor(query = "nonexistent")

        val loaded = assertIs<SearchCatalogItemsInteractor.Result.Loaded>(result)
        assertEquals(emptyList(), loaded.items)
    }

    @Test
    fun `empty results with an empty catalog is CatalogUnavailable`() = runTest {
        catalogRepository.searchResult = emptyList()
        catalogRepository.emptyResult = true

        val result = interactor(query = "anything")

        assertIs<SearchCatalogItemsInteractor.Result.CatalogUnavailable>(result)
    }
}
