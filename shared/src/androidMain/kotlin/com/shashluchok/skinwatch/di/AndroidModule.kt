package com.shashluchok.skinwatch.di

import android.content.Context
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import com.shashluchok.skinwatch.data.storage.pricesync.AndroidPriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import org.koin.dsl.module

internal fun androidModule(context: Context) = module {
    single<AppDatabase> { createRoomDatabase(context) }
    single<PriceSyncScheduler> { AndroidPriceSyncScheduler(context = context) }
}

object AndroidModule {
    fun init(context: Context) = initKoin(platformModule = androidModule(context))
}
