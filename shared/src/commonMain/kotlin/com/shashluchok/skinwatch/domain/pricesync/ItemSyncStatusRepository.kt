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

    suspend fun markFailed(marketHashName: String, error: SteamMarketError, at: Instant)

    companion object {
        val EMPTY = object : ItemSyncStatusRepository {
            override fun observeAll(): Flow<Map<String, ItemSyncStatus>> = flowOf(emptyMap())

            override suspend fun getAll(): Map<String, ItemSyncStatus> = emptyMap()

            override suspend fun markSynced(marketHashName: String, at: Instant) = Unit

            override suspend fun markFailed(marketHashName: String, error: SteamMarketError, at: Instant) = Unit
        }
    }
}
