package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.SteamCurrency

internal interface CurrencyConversionRepository {
    /** true if at least one InventoryItem or PriceSnapshot row exists. */
    suspend fun hasConvertibleData(): Boolean

    /**
     * Atomically converts purchasePrice on every InventoryItem and both prices on every
     * PriceSnapshot into targetCurrency using rates, and saves newSelectedCurrency as the new
     * SettingsRepository override -- all three tables in one transaction, all or nothing.
     */
    suspend fun convertAll(
        rates: Map<SteamCurrency, Double>,
        targetCurrency: SteamCurrency,
        newSelectedCurrency: SteamCurrency?,
    )
}
