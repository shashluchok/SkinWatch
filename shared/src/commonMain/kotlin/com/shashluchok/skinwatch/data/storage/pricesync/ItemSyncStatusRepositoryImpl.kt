package com.shashluchok.skinwatch.data.storage.pricesync

import com.shashluchok.skinwatch.domain.pricesync.ItemSyncStatus
import com.shashluchok.skinwatch.domain.pricesync.ItemSyncStatusRepository
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal class ItemSyncStatusRepositoryImpl(
    private val dao: ItemSyncStatusDao,
) : ItemSyncStatusRepository {
    override fun observeAll(): Flow<Map<String, ItemSyncStatus>> =
        dao.observeAll().map { entities -> entities.toDomain() }

    override suspend fun getAll(): Map<String, ItemSyncStatus> = dao.getAll().toDomain()

    override suspend fun markSynced(marketHashName: String, at: Instant) = dao.upsert(
        ItemSyncStatusEntity(
            marketHashName = marketHashName,
            lastAttemptAt = at,
            lastSuccessAt = at,
            lastErrorType = null,
            lastErrorMessage = null,
        ),
    )

    override suspend fun markFailed(marketHashName: String, error: SteamMarketError, at: Instant) = dao.upsert(
        ItemSyncStatusEntity(
            marketHashName = marketHashName,
            lastAttemptAt = at,
            // Carried over from the existing row: a failure must not erase when this item was last
            // genuinely priced, which is exactly what decides whether it is due again.
            lastSuccessAt = dao.get(marketHashName)?.lastSuccessAt,
            lastErrorType = steamMarketErrorToType(error),
            lastErrorMessage = steamMarketErrorToMessage(error),
        ),
    )
}

private fun List<ItemSyncStatusEntity>.toDomain(): Map<String, ItemSyncStatus> =
    associate { it.marketHashName to it.toDomain() }

private fun ItemSyncStatusEntity.toDomain(): ItemSyncStatus = if (lastErrorType == null) {
    ItemSyncStatus.Synced(attemptedAt = lastAttemptAt)
} else {
    ItemSyncStatus.Failed(
        attemptedAt = lastAttemptAt,
        lastSuccessAt = lastSuccessAt,
        error = typeToSteamMarketError(type = lastErrorType, message = lastErrorMessage),
    )
}
