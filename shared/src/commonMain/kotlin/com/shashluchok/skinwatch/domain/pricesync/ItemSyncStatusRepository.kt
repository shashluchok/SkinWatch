package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant

/** Keys are `marketHashName`; a missing key means that item has never been attempted. */
internal interface ItemSyncStatusRepository {
    fun observeAll(): Flow<Map<String, ItemSyncStatus>>

    suspend fun getAll(): Map<String, ItemSyncStatus>

    suspend fun markSynced(marketHashName: String, at: Instant)

    /** Returns the status it just wrote, so the caller can see how far along its curve the item is. */
    suspend fun markFailed(marketHashName: String, error: SteamMarketError, at: Instant): ItemSyncStatus.Failed

    /** Called when the last inventory item holding this hash is gone and the status outlives nothing. */
    suspend fun delete(marketHashName: String)

    companion object {
        val EMPTY = object : ItemSyncStatusRepository {
            override fun observeAll(): Flow<Map<String, ItemSyncStatus>> = flowOf(emptyMap())

            override suspend fun getAll(): Map<String, ItemSyncStatus> = emptyMap()

            override suspend fun markSynced(marketHashName: String, at: Instant) = Unit

            override suspend fun markFailed(
                marketHashName: String,
                error: SteamMarketError,
                at: Instant,
            ): ItemSyncStatus.Failed = ItemSyncStatus.Failed(
                attemptedAt = at,
                lastSuccessAt = null,
                error = error,
                consecutiveFailures = 1,
            )

            override suspend fun delete(marketHashName: String) = Unit
        }
    }
}
