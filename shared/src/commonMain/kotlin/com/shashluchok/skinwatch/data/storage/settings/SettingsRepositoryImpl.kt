package com.shashluchok.skinwatch.data.storage.settings

import com.shashluchok.skinwatch.data.storage.idToSteamCurrency
import com.shashluchok.skinwatch.data.storage.steamCurrencyToId
import com.shashluchok.skinwatch.domain.settings.SettingsRepository
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class SettingsRepositoryImpl(
    private val dao: SettingsDao,
) : SettingsRepository {
    override val selectedCurrency: Flow<SteamCurrency?> =
        dao.observe().map { entity -> entity?.selectedCurrencyId?.let(::idToSteamCurrency) }

    override suspend fun setSelectedCurrency(currency: SteamCurrency?) =
        dao.upsert(SettingsEntity(selectedCurrencyId = currency?.let(::steamCurrencyToId)))
}
