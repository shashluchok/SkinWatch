package com.shashluchok.skinwatch.data.storage.debug

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DebugSettingsDao {
    @Query("SELECT * FROM DebugSettings WHERE id = 0")
    fun observe(): Flow<DebugSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DebugSettingsEntity)
}
