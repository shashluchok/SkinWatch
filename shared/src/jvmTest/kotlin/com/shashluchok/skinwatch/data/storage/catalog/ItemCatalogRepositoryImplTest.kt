package com.shashluchok.skinwatch.data.storage.catalog

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItemCatalogRepositoryImplTest {
    private fun newRepository(): ItemCatalogRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return ItemCatalogRepositoryImpl(dao = database.catalogItemDao())
    }

    private fun item(name: String, category: CatalogCategory) = CatalogItem(
        marketHashName = name,
        displayName = name,
        iconUrl = "https://example.com/$name.png",
        category = category,
    )

    @Test
    fun `isEmpty is true before anything is stored, false after`() = runTest {
        val repository = newRepository()
        assertTrue(repository.isEmpty())

        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline", CatalogCategory.SKIN)),
        )

        assertEquals(false, repository.isEmpty())
    }

    @Test
    fun `search finds a substring match case-insensitively`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline (Field-Tested)", CatalogCategory.SKIN)),
        )

        val results = repository.search("redline")

        assertEquals(1, results.size)
        assertEquals("AK-47 | Redline (Field-Tested)", results.single().marketHashName)
    }

    @Test
    fun `search with no match returns an empty list`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline (Field-Tested)", CatalogCategory.SKIN)),
        )

        assertEquals(emptyList(), repository.search("nonexistent"))
    }

    @Test
    fun `clearCategory only removes rows of that category, others are untouched`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline", CatalogCategory.SKIN)),
        )
        repository.insertItems(
            category = CatalogCategory.STICKER,
            items = listOf(item("Sticker | Shooter", CatalogCategory.STICKER)),
        )

        repository.clearCategory(CatalogCategory.SKIN)

        assertEquals(emptyList(), repository.search("Redline"))
        assertEquals(1, repository.search("Shooter").size)
    }

    @Test
    fun `insertItems accumulates across multiple chunks of the same category`() = runTest {
        val repository = newRepository()

        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline", CatalogCategory.SKIN)),
        )
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AWP | Asiimov", CatalogCategory.SKIN)),
        )

        assertEquals(1, repository.search("Redline").size)
        assertEquals(1, repository.search("Asiimov").size)
    }
}
