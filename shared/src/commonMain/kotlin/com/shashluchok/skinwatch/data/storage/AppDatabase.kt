package com.shashluchok.skinwatch.data.storage

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.shashluchok.skinwatch.data.storage.inventory.InventoryDao
import com.shashluchok.skinwatch.data.storage.inventory.InventoryItemEntity
import com.shashluchok.skinwatch.data.storage.pricesnapshot.PriceSnapshotDao
import com.shashluchok.skinwatch.data.storage.pricesnapshot.PriceSnapshotEntity
import com.shashluchok.skinwatch.data.storage.settings.SettingsDao
import com.shashluchok.skinwatch.data.storage.settings.SettingsEntity
import com.shashluchok.skinwatch.data.storage.watchlist.WatchlistDao
import com.shashluchok.skinwatch.data.storage.watchlist.WatchlistItemEntity

/** Filename shared by every platform's `createRoomDatabase` bootstrap. */
internal const val DATABASE_FILE_NAME = "skinwatch.db"

/**
 * The only `public` type in `data.storage`. Kotlin `internal` is module-scoped, and
 * `androidApp`/`desktopApp`/`webApp` (separate Gradle modules from `shared`) each construct a
 * `RoomDatabase.Builder<AppDatabase>`/`AppDatabase` of their own -- an `internal` class here would
 * not compile from those modules.
 */
@Database(
    entities = [
        InventoryItemEntity::class,
        WatchlistItemEntity::class,
        PriceSnapshotEntity::class,
        SettingsEntity::class,
    ],
    version = 2,
)
@ColumnTypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun inventoryDao(): InventoryDao

    internal abstract fun watchlistDao(): WatchlistDao

    internal abstract fun priceSnapshotDao(): PriceSnapshotDao

    internal abstract fun settingsDao(): SettingsDao
}

/**
 * Non-JVM/non-Android KMP targets (Native, JS, WasmJs) can't instantiate the Room-generated
 * database implementation via reflection the way the JVM/Android targets can -- Room 3.0 KMP
 * requires this `expect object` so its KSP compiler can generate a matching `actual` per target
 * instead. No `actual` is written by hand anywhere: the Room compiler generates it for every
 * target, including Android and JVM, confirmed against the current official Room KMP setup guide.
 */
@Suppress("KotlinNoActualForExpect")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
