package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import org.koin.dsl.module

internal val iosModule = module {
    single<AppDatabase> { createRoomDatabase() }
    single<PriceSyncScheduler> { PriceSyncScheduler.EMPTY }
}

object IosModule {
    private var isKoinStarted = false

    fun init() {
        if (!isKoinStarted) {
            isKoinStarted = true
            initKoin(platformModule = iosModule)
        }
    }
}
