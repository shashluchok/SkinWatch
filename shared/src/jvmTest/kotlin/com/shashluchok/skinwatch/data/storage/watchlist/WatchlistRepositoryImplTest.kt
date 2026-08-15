package com.shashluchok.skinwatch.data.storage.watchlist

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchlistRepositoryImplTest {
    private fun newRepository(): WatchlistRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return WatchlistRepositoryImpl(dao = database.watchlistDao())
    }

    @Test
    fun `addItem then observeItems emits the added item`() = runTest {
        val repository = newRepository()

        val id = repository.addItem(marketHashName = "Karambit | Doppler (Factory New)")

        val item = repository.observeItems().first().single()
        assertEquals(id, item.id)
        assertEquals("Karambit | Doppler (Factory New)", item.marketHashName)
    }

    @Test
    fun `removeItem deletes it from observeItems`() = runTest {
        val repository = newRepository()
        val id = repository.addItem(marketHashName = "M4A4 | Howl (Field-Tested)")
        assertTrue(repository.observeItems().first().isNotEmpty())

        repository.removeItem(id)

        assertTrue(repository.observeItems().first().isEmpty())
    }
}
