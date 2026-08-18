package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.domain.exchangerate.ConvertStoredPricesInteractor
import com.shashluchok.skinwatch.domain.exchangerate.HasConvertiblePricesInteractor
import com.shashluchok.skinwatch.domain.inventory.AddInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.ObserveInventoryListInteractor
import com.shashluchok.skinwatch.domain.inventory.RemoveInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.UpdateInventoryItemInteractor
import com.shashluchok.skinwatch.domain.pricesnapshot.ObservePriceHistoryInteractor
import com.shashluchok.skinwatch.domain.pricesync.ObserveLastSyncedAtInteractor
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsIfStaleInteractor
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsInteractor
import com.shashluchok.skinwatch.domain.settings.ObserveSelectedCurrencyInteractor
import com.shashluchok.skinwatch.domain.settings.SetSelectedCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.GetDefaultCurrencyInteractor
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
    single { UpdateInventoryItemInteractor(inventoryRepository = get()) }
    single { RemoveInventoryItemInteractor(inventoryRepository = get()) }
    single { ObserveInventoryListInteractor(inventoryRepository = get(), priceSnapshotRepository = get()) }
    single { ObservePriceHistoryInteractor(priceSnapshotRepository = get()) }
    single { ObserveSelectedCurrencyInteractor(settingsRepository = get()) }
    single { SetSelectedCurrencyInteractor(settingsRepository = get()) }
    single { GetDefaultCurrencyInteractor(steamMarketRepository = get()) }
    single { HasConvertiblePricesInteractor(currencyConversionRepository = get()) }
    single { ConvertStoredPricesInteractor(exchangeRateRepository = get(), currencyConversionRepository = get()) }
    single {
        SyncPriceSnapshotsInteractor(
            inventoryRepository = get(),
            steamMarketRepository = get(),
            priceSnapshotRepository = get(),
            resolveDisplayCurrency = get(),
            priceSyncStatusRepository = get(),
        )
    }
    single { SyncPriceSnapshotsIfStaleInteractor(priceSyncStatusRepository = get(), syncPriceSnapshots = get()) }
    single { ObserveLastSyncedAtInteractor(priceSyncStatusRepository = get()) }
}
