package com.shashluchok.skinwatch.domain.catalog

internal class SearchCatalogItemsInteractor(
    private val catalogRepository: ItemCatalogRepository,
) {
    sealed interface Result {
        data class Loaded(
            val items: List<CatalogItem>,
        ) : Result

        data object CatalogUnavailable : Result
    }

    suspend operator fun invoke(query: String): Result {
        val items = catalogRepository.search(query)
        return if (items.isEmpty() && catalogRepository.isEmpty()) {
            Result.CatalogUnavailable
        } else {
            Result.Loaded(items)
        }
    }
}
