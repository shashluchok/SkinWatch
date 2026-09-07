package com.shashluchok.skinwatch.data.storage.catalog

import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRepository
import com.shashluchok.skinwatch.domain.catalog.MAX_SEARCH_TOKENS
import com.shashluchok.skinwatch.domain.catalog.toSearchableName

internal class ItemCatalogRepositoryImpl(
    private val dao: CatalogItemDao,
) : ItemCatalogRepository {
    override suspend fun search(tokens: List<String>): List<CatalogItem> {
        val padded = List(size = MAX_SEARCH_TOKENS) { tokens.getOrNull(it) }

        return dao
            .search(
                first = padded[0],
                second = padded[1],
                third = padded[2],
                fourth = padded[3],
            ).map { it.toDomain() }
    }

    override suspend fun clearCategory(category: CatalogCategory) = dao.deleteByCategory(category.ordinal)

    override suspend fun insertItems(category: CatalogCategory, items: List<CatalogItem>) =
        dao.insertAll(items.map { it.toEntity() })

    override suspend fun isEmpty(): Boolean = dao.count() == 0

    private fun CatalogItemEntity.toDomain() = CatalogItem(
        marketHashName = marketHashName,
        displayName = displayName,
        iconUrl = iconUrl,
        category = CatalogCategory.entries[category],
    )

    private fun CatalogItem.toEntity() = CatalogItemEntity(
        marketHashName = marketHashName,
        displayName = displayName,
        iconUrl = iconUrl,
        category = category.ordinal,
        searchName = displayName.toSearchableName(),
    )
}
