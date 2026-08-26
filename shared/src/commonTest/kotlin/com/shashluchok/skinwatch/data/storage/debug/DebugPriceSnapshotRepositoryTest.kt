package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class DebugPriceSnapshotRepositoryTest {
    @Test
    fun `observeSnapshots returns a chronologically ordered history for a known scenario`() = runTest {
        val repository = DebugPriceSnapshotRepository()

        val snapshots = repository.observeSnapshots("${DEBUG_NAME_PREFIX}Many prices, volatile, profit").first()

        assertTrue(snapshots.isNotEmpty())
        assertEquals(snapshots.sortedBy { it.capturedAt }, snapshots)
    }

    @Test
    fun `observeSnapshots is empty for the no-snapshots-yet scenario`() = runTest {
        val repository = DebugPriceSnapshotRepository()

        val snapshots = repository.observeSnapshots("${DEBUG_NAME_PREFIX}No snapshots yet").first()

        assertTrue(snapshots.isEmpty())
    }

    @Test
    fun `observeSnapshots is empty for a market hash name outside the fixed scenarios`() = runTest {
        val repository = DebugPriceSnapshotRepository()

        val snapshots = repository.observeSnapshots("AK-47 | Redline").first()

        assertTrue(snapshots.isEmpty())
    }

    @Test
    fun `record is a no-op that never changes the fixed history`() = runTest {
        val repository = DebugPriceSnapshotRepository()
        val marketHashName = "${DEBUG_NAME_PREFIX}Breakeven"
        val before = repository.observeSnapshots(marketHashName).first()

        repository.record(
            marketHashName = marketHashName,
            overview = SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
            currency = before.first().currency,
            capturedAt = Clock.System.now(),
        )

        assertEquals(before, repository.observeSnapshots(marketHashName).first())
    }
}
