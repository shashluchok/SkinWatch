package com.shashluchok.skinwatch.data.storage.catalog

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "CatalogItem", indices = [Index(value = ["displayName"])])
internal data class CatalogItemEntity(
    @PrimaryKey val marketHashName: String,
    val displayName: String,
    val iconUrl: String,
    val category: Int,
)
