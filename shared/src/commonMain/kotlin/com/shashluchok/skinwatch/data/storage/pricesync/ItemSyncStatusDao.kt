package com.shashluchok.skinwatch.data.storage.pricesync

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ItemSyncStatusDao {
    /** Every status in one query -- callers need them all and must not read or observe per item. */
    @Query("SELECT * FROM ItemSyncStatus")
    fun observeAll(): Flow<List<ItemSyncStatusEntity>>

    @Query("SELECT * FROM ItemSyncStatus")
    suspend fun getAll(): List<ItemSyncStatusEntity>

    @Query("SELECT * FROM ItemSyncStatus WHERE marketHashName = :marketHashName")
    suspend fun get(marketHashName: String): ItemSyncStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ItemSyncStatusEntity)
}
