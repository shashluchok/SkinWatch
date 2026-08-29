package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.time.Instant

/**
 * Delegates every call to either [realRepository] or [debugRepository] depending on the debug
 * panel's mock-data switch, read live from [debugSettingsRepository] on every call -- same
 * reasoning as [InventoryRepositoryMediator].
 */
internal class PriceSnapshotRepositoryMediator(
    private val realRepository: PriceSnapshotRepository,
    private val debugRepository: PriceSnapshotRepository,
    private val debugSettingsRepository: DebugSettingsRepository,
) : PriceSnapshotRepository {
    override suspend fun record(
        marketHashName: String,
        overview: SteamPriceOverview,
        currency: SteamCurrency,
        capturedAt: Instant,
    ) = activeRepository().record(
        marketHashName = marketHashName,
        overview = overview,
        currency = currency,
        capturedAt = capturedAt,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSnapshots(marketHashName: String): Flow<List<PriceSnapshot>> =
        debugSettingsRepository.settings.flatMapLatest {
            activeRepository(it.mockDataEnabled).observeSnapshots(marketHashName)
        }

    override suspend fun compactHistory(marketHashName: String, now: Instant) =
        activeRepository().compactHistory(marketHashName = marketHashName, now = now)

    private suspend fun activeRepository(): PriceSnapshotRepository =
        activeRepository(debugSettingsRepository.settings.first().mockDataEnabled)

    private fun activeRepository(mockDataEnabled: Boolean): PriceSnapshotRepository =
        if (mockDataEnabled) debugRepository else realRepository
}
