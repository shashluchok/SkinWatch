package com.shashluchok.skinwatch.data.storage

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds the per-item failure counter the price-sync backoff escalates on.
 *
 * Written as a real migration rather than folded into version 1 because by this point there is a
 * device carrying an inventory and a price history worth keeping -- the schema is no longer only a
 * developer's own scratch data.
 *
 * Existing rows start at zero: whatever they last failed at, the counter is about what happens from
 * here, and starting everyone at the near end of the curve is the forgiving direction to be wrong in.
 */
internal val MIGRATION_1_2 = object : Migration(startVersion = 1, endVersion = 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE ItemSyncStatus ADD COLUMN consecutiveFailures INTEGER NOT NULL DEFAULT 0",
        )
    }
}

/**
 * The catalog is cleared rather than backfilled here: the reduction lives in one Kotlin function
 * that typed words also go through, and repeating it as nested SQL `replace` calls would be a copy
 * free to drift. Clearing the sync status makes the next launch refetch, filling the column through
 * that function. Only the catalog goes; the inventory and its price history stay.
 */
private val MIGRATION_2_3 = object : Migration(startVersion = 2, endVersion = 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE CatalogItem ADD COLUMN searchName TEXT NOT NULL DEFAULT ''")
        connection.execSQL("DELETE FROM CatalogItem")
        connection.execSQL("DELETE FROM CatalogSyncStatus")
    }
}

/** One place for every platform's builder to pick the migrations up from. */
internal fun <T : RoomDatabase> RoomDatabase.Builder<T>.addAppMigrations(): RoomDatabase.Builder<T> =
    addMigrations(MIGRATION_1_2, MIGRATION_2_3)
