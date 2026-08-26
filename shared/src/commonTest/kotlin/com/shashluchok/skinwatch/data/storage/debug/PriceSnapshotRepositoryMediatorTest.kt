package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import com.shashluchok.skinwatch.domain.debug.FakeDebugSettingsRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class PriceSnapshotRepositoryMediatorTest {
    @Test
    fun `observeSnapshots delegates to the real repository while mock data is disabled`() = runTest {
        val realRepository = FakePriceSnapshotRepository()
        realRepository.emitSnapshot(
            marketHashName = "AK-47 | Redline",
            lowestPrice = Money(minorUnits = 10_00, currency = SteamCurrency.USD),
            capturedAt = Clock.System.now(),
        )
        val mediator = PriceSnapshotRepositoryMediator(
            realRepository = realRepository,
            debugRepository = FakePriceSnapshotRepository(),
            debugSettingsRepository = FakeDebugSettingsRepository(initialMockDataEnabled = false),
        )

        val snapshots = mediator.observeSnapshots("AK-47 | Redline").first()

        assertEquals(1, snapshots.size)
    }

    @Test
    fun `observeSnapshots delegates to the debug repository while mock data is enabled`() = runTest {
        val debugRepository = FakePriceSnapshotRepository()
        debugRepository.emitSnapshot(
            marketHashName = "${DEBUG_NAME_PREFIX}Breakeven",
            lowestPrice = Money(minorUnits = 50_00, currency = SteamCurrency.RUB),
            capturedAt = Clock.System.now(),
        )
        val mediator = PriceSnapshotRepositoryMediator(
            realRepository = FakePriceSnapshotRepository(),
            debugRepository = debugRepository,
            debugSettingsRepository = FakeDebugSettingsRepository(initialMockDataEnabled = true),
        )

        val snapshots = mediator.observeSnapshots("${DEBUG_NAME_PREFIX}Breakeven").first()

        assertEquals(1, snapshots.size)
    }

    @Test
    fun `record after toggling mock data mid-session goes to the newly active repository`() = runTest {
        val realRepository = FakePriceSnapshotRepository()
        val debugRepository = FakePriceSnapshotRepository()
        val debugSettingsRepository = FakeDebugSettingsRepository(initialMockDataEnabled = false)
        val mediator = PriceSnapshotRepositoryMediator(
            realRepository = realRepository,
            debugRepository = debugRepository,
            debugSettingsRepository = debugSettingsRepository,
        )
        val overview = SteamPriceOverview(
            lowestPrice = Money(minorUnits = 10_00, currency = SteamCurrency.USD),
            medianPrice = null,
            volume = null,
        )
        mediator.record(
            marketHashName = "AK-47 | Redline",
            overview = overview,
            currency = SteamCurrency.USD,
            capturedAt = Clock.System.now(),
        )

        debugSettingsRepository.update(
            DebugSettingsRepository.DebugSettings(mockDataEnabled = true),
        )
        mediator.record(
            marketHashName = "AK-47 | Redline",
            overview = overview,
            currency = SteamCurrency.USD,
            capturedAt = Clock.System.now(),
        )

        assertTrue(realRepository.recorded.size == 1)
        assertTrue(debugRepository.recorded.size == 1)
    }
}
