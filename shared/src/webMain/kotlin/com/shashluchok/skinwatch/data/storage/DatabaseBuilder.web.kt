package com.shashluchok.skinwatch.data.storage

import androidx.room3.Room
import com.shashluchok.skinwatch.sqliteweb.createSqliteWebWorkerDriver
import kotlinx.coroutines.Dispatchers

fun createRoomDatabase(): AppDatabase = Room
    .databaseBuilder<AppDatabase>(name = DATABASE_FILE_NAME)
    .setDriver(createSqliteWebWorkerDriver())
    .setQueryCoroutineContext(Dispatchers.Default)
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
