package com.shashluchok.skinwatch.data.storage.catalog

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
internal interface CatalogItemDao {
    /**
     * Fixed parameters rather than SQL built per call, so no user input is spliced into the
     * statement. An unused word binds null and matches everything.
     */
    @Query(
        "SELECT * FROM CatalogItem WHERE " +
            "(:first IS NULL OR searchName LIKE '%' || :first || '%') AND " +
            "(:second IS NULL OR searchName LIKE '%' || :second || '%') AND " +
            "(:third IS NULL OR searchName LIKE '%' || :third || '%') AND " +
            "(:fourth IS NULL OR searchName LIKE '%' || :fourth || '%') " +
            "ORDER BY CASE WHEN searchName LIKE :first || '%' THEN 0 ELSE 1 END, displayName " +
            "LIMIT 50",
    )
    suspend fun search(
        first: String?,
        second: String?,
        third: String?,
        fourth: String?,
    ): List<CatalogItemEntity>

    @Query("SELECT COUNT(*) FROM CatalogItem")
    suspend fun count(): Int

    @Query("DELETE FROM CatalogItem WHERE category = :category")
    suspend fun deleteByCategory(category: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItemEntity>)
}
