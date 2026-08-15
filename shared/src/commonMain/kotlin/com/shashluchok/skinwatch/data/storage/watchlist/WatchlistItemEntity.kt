package com.shashluchok.skinwatch.data.storage.watchlist

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "WatchlistItem")
internal data class WatchlistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marketHashName: String,
    val addedAt: Instant,
)
