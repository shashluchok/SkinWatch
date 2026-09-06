package com.shashluchok.skinwatch.data.storage.pricesnapshot

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PriceSnapshotDao {
    @Query("SELECT * FROM PriceSnapshot WHERE marketHashName = :marketHashName ORDER BY capturedAt ASC")
    fun observeForItem(marketHashName: String): Flow<List<PriceSnapshotEntity>>

    /**
     * Newest row per item in one query. The bare columns alongside `MAX(capturedAt)` are SQLite's
     * documented "bare columns in an aggregate query" behaviour: they come from the row that
     * produced the maximum, which is exactly the snapshot wanted here.
     */
    @Query(
        """
        SELECT id, marketHashName, currencyId, lowestPriceMinorUnits, medianPriceMinorUnits, volume,
               MAX(capturedAt) AS capturedAt
        FROM PriceSnapshot
        GROUP BY marketHashName
        """,
    )
    fun observeLatestPerItem(): Flow<List<PriceSnapshotEntity>>

    @Insert
    suspend fun insert(entity: PriceSnapshotEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM PriceSnapshot)")
    suspend fun hasAny(): Boolean

    @Query("SELECT * FROM PriceSnapshot")
    suspend fun getAll(): List<PriceSnapshotEntity>

    @Query("SELECT * FROM PriceSnapshot WHERE marketHashName = :marketHashName ORDER BY capturedAt ASC")
    suspend fun getAllForItem(marketHashName: String): List<PriceSnapshotEntity>

    @Update
    suspend fun updateAll(entities: List<PriceSnapshotEntity>)

    @Query("DELETE FROM PriceSnapshot WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
