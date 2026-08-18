package com.shashluchok.skinwatch.data.storage.pricesync

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PriceSyncStatusDao {
    @Query("SELECT * FROM PriceSyncStatus WHERE id = 0")
    fun observe(): Flow<PriceSyncStatusEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PriceSyncStatusEntity)
}
