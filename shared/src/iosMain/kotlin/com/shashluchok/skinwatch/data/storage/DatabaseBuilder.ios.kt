package com.shashluchok.skinwatch.data.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createRoomDatabase(): AppDatabase {
    val documentsDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val databasePath = requireNotNull(documentsDirectory?.path) {
        "Could not resolve the iOS documents directory"
    } + "/$DATABASE_FILE_NAME"

    return Room
        .databaseBuilder<AppDatabase>(name = databasePath)
        .setDriver(BundledSQLiteDriver())
        // `Dispatchers.IO` is JVM-only public API -- it exists on Kotlin/Native but is `internal`
        // to kotlinx.coroutines there, so iOS uses `Dispatchers.Default` instead (same choice the
        // web bootstrap makes, for the same reason).
        .setQueryCoroutineContext(Dispatchers.Default)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
