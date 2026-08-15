package com.shashluchok.skinwatch.data.storage.watchlist

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface WatchlistDao {
    @Query("SELECT * FROM WatchlistItem ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistItemEntity>>

    @Insert
    suspend fun insert(entity: WatchlistItemEntity): Long

    @Query("DELETE FROM WatchlistItem WHERE id = :id")
    suspend fun deleteById(id: Long)
}
