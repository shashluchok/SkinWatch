package com.shashluchok.skinwatch.domain.pricesnapshot

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ObservePriceHistoryInteractorTest {
    private val priceSnapshotRepository = FakePriceSnapshotRepository()
    private val interactor = ObservePriceHistoryInteractor(priceSnapshotRepository = priceSnapshotRepository)

    @Test
    fun `returns the repository's snapshot history for the given marketHashName as-is`() = runTest {
        val hashName = "AK-47 | Redline (Field-Tested)"
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(1_000),
        )
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 5500, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(2_000),
        )

        val history = interactor(hashName).first()

        assertEquals(2, history.size)
        assertEquals(Money(minorUnits = 5000, currency = SteamCurrency.USD), history[0].lowestPrice)
        assertEquals(Money(minorUnits = 5500, currency = SteamCurrency.USD), history[1].lowestPrice)
    }

    @Test
    fun `does not return a different marketHashName's history`() = runTest {
        priceSnapshotRepository.emitSnapshot(
            marketHashName = "AWP | Asiimov (Field-Tested)",
            lowestPrice = Money(minorUnits = 9000, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(1_000),
        )

        val history = interactor("AK-47 | Redline (Field-Tested)").first()

        assertEquals(emptyList(), history)
    }
}
