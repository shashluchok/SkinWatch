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

        val results = repository.search(listOf("redline"))

        assertEquals(1, results.size)
        assertEquals("AK-47 | Redline (Field-Tested)", results.single().marketHashName)
    }

    @Test
    fun `every word has to appear but they need not be adjacent`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline (Field-Tested)", CatalogCategory.SKIN)),
        )

        assertEquals(1, repository.search(listOf("ak", "redline")).size)
        assertEquals(1, repository.search(listOf("redline", "tested")).size)
    }

    @Test
    fun `the order the words are typed in does not matter`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline (Field-Tested)", CatalogCategory.SKIN)),
        )

        assertEquals(
            repository.search(listOf("ak", "redline")),
            repository.search(listOf("redline", "ak")),
        )
    }

    @Test
    fun `a word absent from the name rules the item out`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline (Field-Tested)", CatalogCategory.SKIN)),
        )

        assertEquals(emptyList(), repository.search(listOf("ak", "asiimov")))
    }

    @Test
    fun `a word written across the name's punctuation still matches`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline (Field-Tested)", CatalogCategory.SKIN)),
        )

        assertEquals(1, repository.search(listOf("ak47")).size)
        assertEquals(1, repository.search(listOf("fieldtested")).size)
    }

    @Test
    fun `no words at all leaves everything in`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(
                item("AK-47 | Redline", CatalogCategory.SKIN),
                item("AWP | Asiimov", CatalogCategory.SKIN),
            ),
        )

        assertEquals(2, repository.search(emptyList()).size)
    }

    @Test
    fun `names starting with the first word come before names merely containing it`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(
                item("AAA Sticker | Redline", CatalogCategory.SKIN),
                item("Redline Rifle", CatalogCategory.SKIN),
            ),
        )

        assertEquals(
            listOf("Redline Rifle", "AAA Sticker | Redline"),
            repository.search(listOf("redline")).map { it.displayName },
        )
    }

    @Test
    fun `search with no match returns an empty list`() = runTest {
        val repository = newRepository()
        repository.insertItems(
            category = CatalogCategory.SKIN,
            items = listOf(item("AK-47 | Redline (Field-Tested)", CatalogCategory.SKIN)),
        )

        assertEquals(emptyList(), repository.search(listOf("nonexistent")))
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

        assertEquals(emptyList(), repository.search(listOf("redline")))
        assertEquals(1, repository.search(listOf("shooter")).size)
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

        assertEquals(1, repository.search(listOf("redline")).size)
        assertEquals(1, repository.search(listOf("asiimov")).size)
    }
}
