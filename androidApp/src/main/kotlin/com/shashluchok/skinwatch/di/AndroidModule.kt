package com.shashluchok.skinwatch.di

import android.content.Context
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.data.storage.createRoomDatabase
import org.koin.dsl.module

internal fun androidModule(context: Context) = module {
    single<AppDatabase> { createRoomDatabase(context) }
}
