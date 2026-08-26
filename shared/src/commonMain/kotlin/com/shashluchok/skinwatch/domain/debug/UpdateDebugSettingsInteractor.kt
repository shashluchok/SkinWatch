package com.shashluchok.skinwatch.domain.debug

internal class UpdateDebugSettingsInteractor(
    private val debugSettingsRepository: DebugSettingsRepository,
) {
    suspend operator fun invoke(settings: DebugSettingsRepository.DebugSettings) =
        debugSettingsRepository.update(settings)
}
