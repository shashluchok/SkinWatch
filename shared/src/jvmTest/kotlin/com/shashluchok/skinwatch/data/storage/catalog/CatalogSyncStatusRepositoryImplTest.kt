package com.shashluchok.skinwatch.data.storage.catalog

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CatalogSyncStatusRepositoryImplTest {
    private fun newRepository(): CatalogSyncStatusRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return CatalogSyncStatusRepositoryImpl(dao = database.catalogSyncStatusDao())
    }

    @Test
    fun `lastCompletedAt starts as null when nothing was ever synced`() = runTest {
        val repository = newRepository()

        assertEquals(null, repository.lastCompletedAt.first())
    }

    @Test
    fun `markCompleted then lastCompletedAt emits the saved instant`() = runTest {
        val repository = newRepository()
        val completedAt = Instant.fromEpochMilliseconds(123_000)

        repository.markCompleted(completedAt)

        assertEquals(completedAt, repository.lastCompletedAt.first())
    }

    @Test
    fun `a second markCompleted overwrites the previous instant`() = runTest {
        val repository = newRepository()
        repository.markCompleted(Instant.fromEpochMilliseconds(1_000))

        repository.markCompleted(Instant.fromEpochMilliseconds(2_000))

        assertEquals(Instant.fromEpochMilliseconds(2_000), repository.lastCompletedAt.first())
    }
}
