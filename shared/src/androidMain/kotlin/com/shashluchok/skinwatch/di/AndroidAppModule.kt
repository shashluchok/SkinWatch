package com.shashluchok.skinwatch.di

import android.content.Context
import com.shashluchok.skinwatch.AppModule
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.catalog.AndroidCatalogSyncScheduler
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import com.shashluchok.skinwatch.data.storage.pricesync.AndroidPriceSyncScheduler
import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import org.koin.dsl.module

object AndroidAppModule : AppModule() {
    fun init(context: Context, appConfigurationProvider: AppConfigurationProvider) =
        start(
            platformModule = createAndroidModule(context),
            appConfigurationProvider = appConfigurationProvider,
        )

    private fun createAndroidModule(context: Context) = module {
        single<AppDatabase> { createRoomDatabase(context) }
        single<PriceSyncScheduler> { AndroidPriceSyncScheduler(context = context) }
        single<CatalogSyncScheduler> { AndroidCatalogSyncScheduler(context = context) }
    }
}
