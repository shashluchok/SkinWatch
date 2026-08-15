package com.shashluchok.skinwatch.data.storage.pricesnapshot

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class PriceSnapshotRepositoryImplTest {
    private fun newRepository(): PriceSnapshotRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return PriceSnapshotRepositoryImpl(dao = database.priceSnapshotDao())
    }

    @Test
    fun `record then observeSnapshots emits it ordered by capturedAt`() = runTest {
        val repository = newRepository()
        val hashName = "AK-47 | Redline (Field-Tested)"
        val earlier = Instant.fromEpochMilliseconds(1_755_000_000_000)
        val later = Instant.fromEpochMilliseconds(1_755_000_100_000)

        repository.record(
            marketHashName = hashName,
            overview = SteamPriceOverview(
                lowestPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
                medianPrice = Money(minorUnits = 5200, currency = SteamCurrency.USD),
                volume = 100,
            ),
            currency = SteamCurrency.USD,
            capturedAt = later,
        )
        repository.record(
            marketHashName = hashName,
            overview = SteamPriceOverview(
                lowestPrice = Money(minorUnits = 4800, currency = SteamCurrency.USD),
                medianPrice = Money(minorUnits = 4900, currency = SteamCurrency.USD),
                volume = 90,
            ),
            currency = SteamCurrency.USD,
            capturedAt = earlier,
        )

        val snapshots = repository.observeSnapshots(hashName).first()
        assertEquals(2, snapshots.size)
        assertEquals(earlier, snapshots.first().capturedAt)
        assertEquals(later, snapshots.last().capturedAt)
        assertEquals(Money(minorUnits = 4800, currency = SteamCurrency.USD), snapshots.first().lowestPrice)
    }

    @Test
    fun `record with no active listings stores null price fields`() = runTest {
        val repository = newRepository()

        repository.record(
            marketHashName = "Rare Skin",
            overview = SteamPriceOverview(lowestPrice = null, medianPrice = null, volume = null),
            currency = SteamCurrency.USD,
            capturedAt = Instant.fromEpochMilliseconds(1_755_000_000_000),
        )

        val snapshot = repository.observeSnapshots("Rare Skin").first().single()
        assertEquals(null, snapshot.lowestPrice)
        assertEquals(null, snapshot.medianPrice)
        assertEquals(null, snapshot.volume)
    }

    @Test
    fun `observeSnapshots only returns snapshots for the requested marketHashName`() = runTest {
        val repository = newRepository()
        val overview = SteamPriceOverview(
            lowestPrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
            medianPrice = Money(minorUnits = 110, currency = SteamCurrency.USD),
            volume = 1,
        )
        val capturedAt = Instant.fromEpochMilliseconds(1_755_000_000_000)
        repository.record(
            marketHashName = "Item A",
            overview = overview,
            currency = SteamCurrency.USD,
            capturedAt = capturedAt,
        )
        repository.record(
            marketHashName = "Item B",
            overview = overview,
            currency = SteamCurrency.USD,
            capturedAt = capturedAt,
        )

        val snapshots = repository.observeSnapshots("Item A").first()

        assertTrue(snapshots.all { it.marketHashName == "Item A" })
        assertEquals(1, snapshots.size)
    }
}
