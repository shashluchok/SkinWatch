package com.shashluchok.skinwatch.data.storage.catalog

import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import com.shashluchok.skinwatch.domain.catalog.ItemCatalogRepository

internal class ItemCatalogRepositoryImpl(
    private val dao: CatalogItemDao,
) : ItemCatalogRepository {
    override suspend fun search(query: String): List<CatalogItem> = dao.search(query).map { it.toDomain() }

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
    )
}
