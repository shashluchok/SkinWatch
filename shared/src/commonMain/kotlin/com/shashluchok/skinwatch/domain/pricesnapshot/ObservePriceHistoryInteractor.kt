package com.shashluchok.skinwatch.domain.pricesnapshot

import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper over [PriceSnapshotRepository.observeSnapshots] -- exists purely so presentation
 * code depends on an interactor rather than a repository directly, matching this project's "one
 * verb, one interactor" convention.
 */
internal class ObservePriceHistoryInteractor(
    private val priceSnapshotRepository: PriceSnapshotRepository,
) {
    operator fun invoke(marketHashName: String): Flow<List<PriceSnapshot>> =
        priceSnapshotRepository.observeSnapshots(marketHashName)
}
