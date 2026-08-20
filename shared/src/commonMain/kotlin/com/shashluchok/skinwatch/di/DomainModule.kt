package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.domain.catalog.SearchCatalogItemsInteractor
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsIfStaleInteractor
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsInteractor
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
import org.koin.dsl.module

/**
 * Pure business-logic bindings (use cases/interactors) -- repository contracts are bound to their
 * implementations in [dataModule] instead, since those implementations live in `data.*`.
 */
internal val domainModule = module {
    // Steam / search
    single { ResolveDisplayCurrencyInteractor(settingsRepository = get(), steamMarketRepository = get()) }
    single { GetDefaultCurrencyInteractor(steamMarketRepository = get()) }

    // Inventory
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

    // Settings / currency
    single { ObserveSelectedCurrencyInteractor(settingsRepository = get()) }
    single { SetSelectedCurrencyInteractor(settingsRepository = get()) }
    single { HasConvertiblePricesInteractor(currencyConversionRepository = get()) }
    single { ConvertStoredPricesInteractor(exchangeRateRepository = get(), currencyConversionRepository = get()) }

    // Price sync
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

    // Catalog
    single {
        SyncCatalogItemsInteractor(
            remoteSource = get(),
            catalogRepository = get(),
            catalogSyncStatusRepository = get(),
        )
    }
    single { SyncCatalogItemsIfStaleInteractor(catalogSyncStatusRepository = get(), syncCatalogItems = get()) }
    single { SearchCatalogItemsInteractor(catalogRepository = get()) }
}
