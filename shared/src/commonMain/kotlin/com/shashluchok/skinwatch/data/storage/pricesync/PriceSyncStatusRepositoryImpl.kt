package com.shashluchok.skinwatch.data.storage.pricesync

import com.shashluchok.skinwatch.domain.pricesync.PriceSyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal class PriceSyncStatusRepositoryImpl(
    private val dao: PriceSyncStatusDao,
) : PriceSyncStatusRepository {
    override val lastCompletedAt: Flow<Instant?> = dao.observe().map { it?.lastCompletedAt }

    override suspend fun markCompleted(at: Instant) = dao.upsert(PriceSyncStatusEntity(lastCompletedAt = at))
}
