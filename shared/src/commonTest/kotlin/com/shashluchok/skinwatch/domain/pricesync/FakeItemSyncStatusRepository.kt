package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

internal class FakeItemSyncStatusRepository : ItemSyncStatusRepository {
    private val statusesFlow = MutableStateFlow<Map<String, ItemSyncStatus>>(emptyMap())

    val statuses: Map<String, ItemSyncStatus> get() = statusesFlow.value

    override fun observeAll(): Flow<Map<String, ItemSyncStatus>> = statusesFlow

    override suspend fun getAll(): Map<String, ItemSyncStatus> = statusesFlow.value

    override suspend fun markSynced(marketHashName: String, at: Instant) {
        statusesFlow.value += marketHashName to ItemSyncStatus.Synced(attemptedAt = at)
    }

    override suspend fun markFailed(marketHashName: String, error: SteamMarketError, at: Instant) {
        statusesFlow.value += marketHashName to ItemSyncStatus.Failed(
            attemptedAt = at,
            // Mirrors the real store: a failure keeps whatever success time was already recorded.
            lastSuccessAt = statusesFlow.value[marketHashName]?.lastSuccessAt,
            error = error,
        )
    }
}
