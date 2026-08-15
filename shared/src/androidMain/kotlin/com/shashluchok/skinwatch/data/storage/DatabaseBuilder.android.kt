package com.shashluchok.skinwatch.data.storage

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createRoomDatabase(context: Context): AppDatabase {
    val appContext = context.applicationContext
    return Room
        .databaseBuilder<AppDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
