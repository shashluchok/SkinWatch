package com.shashluchok.skinwatch.data.storage.pricesnapshot

import com.shashluchok.skinwatch.data.storage.idToSteamCurrency
import com.shashluchok.skinwatch.data.storage.steamCurrencyToId
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.idsToDeleteForRetention
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal class PriceSnapshotRepositoryImpl(
    private val dao: PriceSnapshotDao,
) : PriceSnapshotRepository {
    override suspend fun record(
        marketHashName: String,
        overview: SteamPriceOverview,
        currency: SteamCurrency,
        capturedAt: Instant,
    ) = dao.insert(
        PriceSnapshotEntity(
            marketHashName = marketHashName,
            currencyId = steamCurrencyToId(currency),
            lowestPriceMinorUnits = overview.lowestPrice?.minorUnits,
            medianPriceMinorUnits = overview.medianPrice?.minorUnits,
            volume = overview.volume,
            capturedAt = capturedAt,
        ),
    )

    override fun observeSnapshots(marketHashName: String): Flow<List<PriceSnapshot>> =
        dao.observeForItem(marketHashName).map { entities -> entities.map { it.toDomain() } }

    override suspend fun compactHistory(marketHashName: String, now: Instant) {
        val entities = dao.getAllForItem(marketHashName)
        val idsToDelete = idsToDeleteForRetention(
            snapshots = entities.map { it.id to it.capturedAt },
            now = now,
        )
        if (idsToDelete.isNotEmpty()) dao.deleteByIds(idsToDelete)
    }
}

private fun PriceSnapshotEntity.toDomain(): PriceSnapshot {
    val currency = idToSteamCurrency(currencyId)
    return PriceSnapshot(
        marketHashName = marketHashName,
        currency = currency,
        lowestPrice = lowestPriceMinorUnits?.let { Money(minorUnits = it, currency = currency) },
        medianPrice = medianPriceMinorUnits?.let { Money(minorUnits = it, currency = currency) },
        volume = volume,
        capturedAt = capturedAt,
    )
}
