package com.shashluchok.skinwatch.data.storage.pricesnapshot

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PriceSnapshotDao {
    @Query("SELECT * FROM PriceSnapshot WHERE marketHashName = :marketHashName ORDER BY capturedAt ASC")
    fun observeForItem(marketHashName: String): Flow<List<PriceSnapshotEntity>>

    @Insert
    suspend fun insert(entity: PriceSnapshotEntity)
}
