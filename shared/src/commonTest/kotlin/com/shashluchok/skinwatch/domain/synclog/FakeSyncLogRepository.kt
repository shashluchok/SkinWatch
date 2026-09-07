package com.shashluchok.skinwatch.domain.synclog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock

internal class FakeSyncLogRepository : SyncLogRepository {
    val entries = mutableListOf<SyncLogEntry>()

    private val mutableEntries = MutableStateFlow<List<SyncLogEntry>>(emptyList())

    override fun observeEntries(): Flow<List<SyncLogEntry>> = mutableEntries

    override fun log(level: SyncLogLevel, tag: SyncLogTag, message: String, marketHashName: String?) {
        entries += SyncLogEntry(
            at = Clock.System.now(),
            level = level,
            tag = tag,
            message = message,
            marketHashName = marketHashName,
        )
        mutableEntries.value = entries.toList()
    }

    override fun clear() {
        entries.clear()
        mutableEntries.value = emptyList()
    }

    fun entriesWith(tag: SyncLogTag): List<SyncLogEntry> = entries.filter { it.tag == tag }
}
