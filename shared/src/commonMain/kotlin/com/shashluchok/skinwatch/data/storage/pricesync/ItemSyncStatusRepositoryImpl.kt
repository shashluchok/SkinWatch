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
            // A success ends whatever streak was running, so the next failure starts the curve over.
            consecutiveFailures = 0,
        ),
    )

    override suspend fun markFailed(
        marketHashName: String,
        error: SteamMarketError,
        at: Instant,
    ): ItemSyncStatus.Failed {
        val existing = dao.get(marketHashName)
        val entity = ItemSyncStatusEntity(
            marketHashName = marketHashName,
            lastAttemptAt = at,
            // Carried over from the existing row: a failure must not erase when this item was last
            // genuinely priced, which is exactly what decides whether it is due again.
            lastSuccessAt = existing?.lastSuccessAt,
            lastErrorType = steamMarketErrorToType(error),
            lastErrorMessage = steamMarketErrorToMessage(error),
            consecutiveFailures = (existing?.consecutiveFailures ?: 0) + 1,
        )
        dao.upsert(entity)

        return entity.toDomain() as ItemSyncStatus.Failed
    }

    override suspend fun delete(marketHashName: String) = dao.deleteByMarketHashName(marketHashName)
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
        // A row written before the counter existed reads as a single failure -- the forgiving end
        // of the curve, which is where an item nobody has counted for should start.
        consecutiveFailures = consecutiveFailures.coerceAtLeast(1),
    )
}
