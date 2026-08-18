package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import org.koin.dsl.module

internal val desktopModule = module {
    single<AppDatabase> { createRoomDatabase() }
    single<PriceSyncScheduler> { PriceSyncScheduler.EMPTY }
}

object DesktopModule {
    fun init() = initKoin(platformModule = desktopModule)
}
