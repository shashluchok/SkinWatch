package com.shashluchok.skinwatch.domain.pricesync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

internal class FakePriceSyncStatusRepository(
    initialLastCompletedAt: Instant? = null,
) : PriceSyncStatusRepository {
    private val lastCompletedAtFlow = MutableStateFlow(initialLastCompletedAt)
    val markCompletedCalls = mutableListOf<Instant>()

    override val lastCompletedAt: Flow<Instant?> = lastCompletedAtFlow

    override suspend fun markCompleted(at: Instant) {
        markCompletedCalls += at
        lastCompletedAtFlow.value = at
    }
}
