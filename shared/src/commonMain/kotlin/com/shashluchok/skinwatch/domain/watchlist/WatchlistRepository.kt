package com.shashluchok.skinwatch.domain.watchlist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface WatchlistRepository {
    fun observeItems(): Flow<List<WatchlistItem>>

    suspend fun addItem(marketHashName: String): Long

    suspend fun removeItem(id: Long)

    companion object {
        val EMPTY = object : WatchlistRepository {
            override fun observeItems(): Flow<List<WatchlistItem>> = flowOf(emptyList())

            override suspend fun addItem(marketHashName: String): Long = 0L

            override suspend fun removeItem(id: Long) = Unit
        }
    }
}
