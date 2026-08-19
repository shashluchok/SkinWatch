package com.shashluchok.skinwatch.data.storage.catalog

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CatalogSyncStatusDao {
    @Query("SELECT * FROM CatalogSyncStatus WHERE id = 0")
    fun observe(): Flow<CatalogSyncStatusEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CatalogSyncStatusEntity)
}
