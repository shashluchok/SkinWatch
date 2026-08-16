package com.shashluchok.skinwatch.data.storage.inventory

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface InventoryDao {
    @Query("SELECT * FROM InventoryItem ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Insert
    suspend fun insert(entity: InventoryItemEntity): Long

    @Update
    suspend fun update(entity: InventoryItemEntity)

    @Query("DELETE FROM InventoryItem WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM InventoryItem)")
    suspend fun hasAny(): Boolean

    @Query("SELECT * FROM InventoryItem")
    suspend fun getAll(): List<InventoryItemEntity>

    @Update
    suspend fun updateAll(entities: List<InventoryItemEntity>)
}
