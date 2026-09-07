package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.domain.catalog.SearchCatalogItemsInteractor
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsIfStaleInteractor
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsInteractor
import com.shashluchok.skinwatch.domain.debug.ObserveDebugSettingsInteractor
import com.shashluchok.skinwatch.domain.debug.UpdateDebugSettingsInteractor
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
import com.shashluchok.skinwatch.domain.synclog.InspectSyncStateInteractor
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
            priceSyncScheduler = get(),
            itemSyncStatusRepository = get(),
            syncLog = get(),
        )
    }
    single { UpdateInventoryItemInteractor(inventoryRepository = get()) }
    single {
        RemoveInventoryItemInteractor(
            inventoryRepository = get(),
            itemSyncStatusRepository = get(),
        )
    }
    single {
        ObserveInventoryListInteractor(
            inventoryRepository = get(),
            priceSnapshotRepository = get(),
        )
    }
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
            itemSyncStatusRepository = get(),
            syncLog = get(),
        )
    }
    single {
        SyncPriceSnapshotsIfStaleInteractor(
            priceSyncStatusRepository = get(),
            syncPriceSnapshots = get(),
            syncLog = get(),
        )
    }
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

    // Diagnostics
    single {
        InspectSyncStateInteractor(
            inventoryRepository = get(),
            itemSyncStatusRepository = get(),
            priceSyncStatusRepository = get(),
            platformInspector = get(),
            syncLog = get(),
        )
    }

    // Debug (temporary -- see the `debug` package doc comment on DebugSettingsRepository)
    single { ObserveDebugSettingsInteractor(debugSettingsRepository = get()) }
    single { UpdateDebugSettingsInteractor(debugSettingsRepository = get()) }
}
