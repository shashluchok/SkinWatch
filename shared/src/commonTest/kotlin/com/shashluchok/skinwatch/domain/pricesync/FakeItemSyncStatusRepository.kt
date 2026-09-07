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

    override suspend fun markFailed(
        marketHashName: String,
        error: SteamMarketError,
        at: Instant,
    ): ItemSyncStatus.Failed {
        val existing = statusesFlow.value[marketHashName]
        val failed = ItemSyncStatus.Failed(
            attemptedAt = at,
            // Mirrors the real store: a failure keeps whatever success time was already recorded,
            // and adds one to however many failures came before it.
            lastSuccessAt = existing?.lastSuccessAt,
            error = error,
            consecutiveFailures = ((existing as? ItemSyncStatus.Failed)?.consecutiveFailures ?: 0) + 1,
        )
        statusesFlow.value += marketHashName to failed

        return failed
    }

    override suspend fun delete(marketHashName: String) {
        statusesFlow.value -= marketHashName
    }
}
