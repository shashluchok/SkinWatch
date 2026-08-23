package com.shashluchok.skinwatch.data.storage.catalog

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
internal interface CatalogItemDao {
    @Query(
        "SELECT * FROM CatalogItem WHERE displayName LIKE '%' || :query || '%' COLLATE NOCASE " +
            "ORDER BY displayName LIMIT 50",
    )
    suspend fun search(query: String): List<CatalogItemEntity>

    @Query("SELECT COUNT(*) FROM CatalogItem")
    suspend fun count(): Int

    @Query("DELETE FROM CatalogItem WHERE category = :category")
    suspend fun deleteByCategory(category: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItemEntity>)
}
