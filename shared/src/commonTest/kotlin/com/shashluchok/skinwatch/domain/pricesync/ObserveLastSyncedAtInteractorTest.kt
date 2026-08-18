package com.shashluchok.skinwatch.domain.pricesync

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ObserveLastSyncedAtInteractorTest {
    @Test
    fun `returns the repository's lastCompletedAt as-is`() = runTest {
        val repository = FakePriceSyncStatusRepository(initialLastCompletedAt = Instant.fromEpochMilliseconds(1_000))
        val interactor = ObserveLastSyncedAtInteractor(priceSyncStatusRepository = repository)

        assertEquals(Instant.fromEpochMilliseconds(1_000), interactor().first())
    }
}
