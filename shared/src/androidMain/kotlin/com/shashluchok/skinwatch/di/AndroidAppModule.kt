package com.shashluchok.skinwatch.di

import android.content.Context
import com.shashluchok.skinwatch.AppModule
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.catalog.AndroidCatalogSyncScheduler
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import com.shashluchok.skinwatch.data.storage.pricesync.AndroidPriceSyncScheduler
import com.shashluchok.skinwatch.data.storage.synclog.AndroidSyncLogExporter
import com.shashluchok.skinwatch.data.storage.synclog.FileSyncLogRepository
import com.shashluchok.skinwatch.data.storage.synclog.WorkManagerSyncStateInspector
import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.synclog.PlatformSyncStateInspector
import com.shashluchok.skinwatch.domain.synclog.SyncLogExporter
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import org.koin.dsl.module

object AndroidAppModule : AppModule() {
    fun init(context: Context, appConfigurationProvider: AppConfigurationProvider) =
        start(
            platformModule = createAndroidModule(context),
            appConfigurationProvider = appConfigurationProvider,
        )

    private fun createAndroidModule(context: Context) = module {
        single<AppDatabase> { createRoomDatabase(context) }
        single<PriceSyncScheduler> { AndroidPriceSyncScheduler(context = context, syncLog = get()) }
        single<CatalogSyncScheduler> { AndroidCatalogSyncScheduler(context = context) }

        // Diagnostics: overrides the no-op bindings the shared data module declares for platforms
        // without a file-backed log.
        single<SyncLogRepository> { FileSyncLogRepository(context = context, scope = get()) }
        single<SyncLogExporter> { AndroidSyncLogExporter(context = context) }
        single<PlatformSyncStateInspector> {
            WorkManagerSyncStateInspector(context = context, syncLog = get())
        }
    }
}
