package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DebugSettingsRepositoryImpl(
    private val dao: DebugSettingsDao,
) : DebugSettingsRepository {
    override val settings: Flow<DebugSettingsRepository.DebugSettings> = dao.observe().map { entity ->
        entity?.toDomain() ?: DebugSettingsRepository.DebugSettings()
    }

    override suspend fun update(settings: DebugSettingsRepository.DebugSettings) {
        dao.upsert(
            DebugSettingsEntity(
                showSplashScreen = settings.showSplashScreen,
                mockDataEnabled = settings.mockDataEnabled,
            ),
        )
    }

    private fun DebugSettingsEntity.toDomain() = DebugSettingsRepository.DebugSettings(
        showSplashScreen = showSplashScreen,
        mockDataEnabled = mockDataEnabled,
    )
}
