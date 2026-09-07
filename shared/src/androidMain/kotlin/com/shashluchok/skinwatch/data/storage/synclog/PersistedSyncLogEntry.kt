package com.shashluchok.skinwatch.data.storage.synclog

import com.shashluchok.skinwatch.domain.synclog.SyncLogEntry
import com.shashluchok.skinwatch.domain.synclog.SyncLogLevel
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * On-disk shape of one log line. Times travel as epoch millis and the enums as their names, so a
 * file written by an earlier build still parses after either type gains a member -- unknown names
 * are dropped by the reader rather than taking the whole file down.
 */
@Serializable
internal data class PersistedSyncLogEntry(
    @SerialName("t") val epochMillis: Long,
    @SerialName("l") val level: String,
    @SerialName("g") val tag: String,
    @SerialName("m") val message: String,
    @SerialName("n") val marketHashName: String? = null,
    @SerialName("c") val networkState: String? = null,
)

internal fun SyncLogEntry.toPersisted(): PersistedSyncLogEntry = PersistedSyncLogEntry(
    epochMillis = at.toEpochMilliseconds(),
    level = level.name,
    tag = tag.name,
    message = message,
    marketHashName = marketHashName,
    networkState = networkState,
)

internal fun PersistedSyncLogEntry.toDomain(): SyncLogEntry = SyncLogEntry(
    at = Instant.fromEpochMilliseconds(epochMillis),
    level = SyncLogLevel.entries.firstOrNull { it.name == level } ?: SyncLogLevel.INFO,
    tag = SyncLogTag.entries.firstOrNull { it.name == tag } ?: SyncLogTag.RUN,
    message = message,
    marketHashName = marketHashName,
    networkState = networkState,
)
