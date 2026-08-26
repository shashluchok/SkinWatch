package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.AppModule
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import org.koin.dsl.module

object IosAppModule : AppModule() {
    private val platformModule = module {
        single<AppDatabase> { createRoomDatabase() }
        single<PriceSyncScheduler> { PriceSyncScheduler.EMPTY }
        single<CatalogSyncScheduler> { CatalogSyncScheduler.EMPTY }
    }

    private var isKoinStarted = false

    fun init(appConfigurationProvider: AppConfigurationProvider) {
        if (!isKoinStarted) {
            isKoinStarted = true
            start(
                platformModule = platformModule,
                appConfigurationProvider = appConfigurationProvider,
            )
        }
    }
}
