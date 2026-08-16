package com.shashluchok.skinwatch.domain.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeSettingsRepository(
    initialCurrency: SteamCurrency? = null,
) : SettingsRepository {
    private val currencyFlow = MutableStateFlow(initialCurrency)

    override val selectedCurrency: Flow<SteamCurrency?> = currencyFlow

    override suspend fun setSelectedCurrency(currency: SteamCurrency?) {
        currencyFlow.value = currency
    }
}
