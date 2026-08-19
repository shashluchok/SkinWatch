package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant

/**
 * `null` means "never synced"
 */
internal interface CatalogSyncStatusRepository {
    val lastCompletedAt: Flow<Instant?>

    suspend fun markCompleted(at: Instant)

    companion object {
        val EMPTY = object : CatalogSyncStatusRepository {
            override val lastCompletedAt: Flow<Instant?> = flowOf(null)

            override suspend fun markCompleted(at: Instant) = Unit
        }
    }
}
