package com.shashluchok.skinwatch.domain.watchlist

import kotlinx.coroutines.flow.Flow

internal interface WatchlistRepository {
    fun observeItems(): Flow<List<WatchlistItem>>

    suspend fun addItem(marketHashName: String): Long

    suspend fun removeItem(id: Long)
}
