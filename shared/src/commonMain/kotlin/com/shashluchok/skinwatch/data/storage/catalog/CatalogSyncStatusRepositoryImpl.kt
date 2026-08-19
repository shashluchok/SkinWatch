package com.shashluchok.skinwatch.data.storage.catalog

import com.shashluchok.skinwatch.domain.catalog.CatalogSyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal class CatalogSyncStatusRepositoryImpl(
    private val dao: CatalogSyncStatusDao,
) : CatalogSyncStatusRepository {
    override val lastCompletedAt: Flow<Instant?> = dao.observe().map { it?.lastCompletedAt }

    override suspend fun markCompleted(at: Instant) = dao.upsert(CatalogSyncStatusEntity(lastCompletedAt = at))
}
