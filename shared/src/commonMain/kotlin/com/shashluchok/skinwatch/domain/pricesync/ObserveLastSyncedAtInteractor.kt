package com.shashluchok.skinwatch.domain.pricesync

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

internal class ObserveLastSyncedAtInteractor(
    private val priceSyncStatusRepository: PriceSyncStatusRepository,
) {
    operator fun invoke(): Flow<Instant?> = priceSyncStatusRepository.lastCompletedAt
}
