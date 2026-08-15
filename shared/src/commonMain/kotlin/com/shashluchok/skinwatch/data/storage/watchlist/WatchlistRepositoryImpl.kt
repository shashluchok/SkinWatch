package com.shashluchok.skinwatch.data.storage.watchlist

import com.shashluchok.skinwatch.domain.watchlist.WatchlistItem
import com.shashluchok.skinwatch.domain.watchlist.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class WatchlistRepositoryImpl(
    private val dao: WatchlistDao,
) : WatchlistRepository {
    override fun observeItems(): Flow<List<WatchlistItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addItem(marketHashName: String): Long = dao.insert(
        WatchlistItemEntity(marketHashName = marketHashName, addedAt = Clock.System.now()),
    )

    override suspend fun removeItem(id: Long) = dao.deleteById(id)
}

private fun WatchlistItemEntity.toDomain(): WatchlistItem = WatchlistItem(
    id = id,
    marketHashName = marketHashName,
    addedAt = addedAt,
)
