package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import org.koin.dsl.module

internal val desktopModule = module {
    single<AppDatabase> { createRoomDatabase() }
}
