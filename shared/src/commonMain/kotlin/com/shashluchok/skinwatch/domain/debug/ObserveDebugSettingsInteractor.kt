package com.shashluchok.skinwatch.domain.debug

import kotlinx.coroutines.flow.Flow

internal class ObserveDebugSettingsInteractor(
    private val debugSettingsRepository: DebugSettingsRepository,
) {
    operator fun invoke(): Flow<DebugSettingsRepository.DebugSettings> = debugSettingsRepository.settings
}
