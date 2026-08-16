package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.domain.inventory.AddInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.ObserveInventoryListInteractor
import com.shashluchok.skinwatch.domain.inventory.RemoveInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.UpdateInventoryItemInteractor
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SearchMarketItemsInteractor
import org.koin.dsl.module

/**
 * Pure business-logic bindings (use cases/interactors) -- repository contracts are bound to their
 * implementations in [dataModule] instead, since those implementations live in `data.*`.
 */
internal val domainModule = module {
    single { ResolveDisplayCurrencyInteractor(settingsRepository = get(), steamMarketRepository = get()) }
    single { SearchMarketItemsInteractor(steamMarketRepository = get(), resolveDisplayCurrency = get()) }
    single {
        AddInventoryItemInteractor(
            inventoryRepository = get(),
            steamMarketRepository = get(),
            priceSnapshotRepository = get(),
            resolveDisplayCurrency = get(),
        )
    }
    single { UpdateInventoryItemInteractor(inventoryRepository = get(), resolveDisplayCurrency = get()) }
    single { RemoveInventoryItemInteractor(inventoryRepository = get()) }
    single { ObserveInventoryListInteractor(inventoryRepository = get(), priceSnapshotRepository = get()) }
}
