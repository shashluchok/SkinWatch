package com.shashluchok.skinwatch.data.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

private const val APP_DATA_DIR_NAME = ".skinwatch"

fun createRoomDatabase(): AppDatabase {
    val appDataDir = File(System.getProperty("user.home"), APP_DATA_DIR_NAME).apply { mkdirs() }
    return Room
        .databaseBuilder<AppDatabase>(name = File(appDataDir, DATABASE_FILE_NAME).absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
