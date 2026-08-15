package com.shashluchok.skinwatch.domain.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.Flow

/**
 * `null` means "no override saved" -- callers fall back to [com.shashluchok.skinwatch.domain.steam
 * .SteamMarketRepository.defaultCurrency]'s locale-based resolution, not an error state.
 */
internal interface SettingsRepository {
    val selectedCurrency: Flow<SteamCurrency?>

    suspend fun setSelectedCurrency(currency: SteamCurrency?)
}
