package com.shashluchok.skinwatch.data.storage.synclog

import android.content.Context
import com.shashluchok.skinwatch.domain.synclog.SyncLogEntry
import com.shashluchok.skinwatch.domain.synclog.SyncLogLevel
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant

private const val LOG_FILE_NAME = "price-sync-log.jsonl"
private const val MAX_LOG_BYTES = 1_024L * 1_024L
private const val KEPT_ENTRY_FRACTION = 2

/**
 * Append-only diagnostic log backed by a file in the app's private storage.
 *
 * A file rather than a table: this is a temporary investigation aid, and giving it a Room entity
 * would put a diagnostic concern into the app's persisted schema. One line of JSON per entry keeps
 * it parseable back into [SyncLogEntry] without inventing an escaping scheme for free-form messages.
 *
 * Writes go through a single-consumer channel, so entries keep the order they were logged in even
 * when a worker and the UI record at the same time, and [log] never blocks its caller.
 */
internal class FileSyncLogRepository(
    private val context: Context,
    scope: CoroutineScope,
) : SyncLogRepository {
    private val file = File(context.filesDir, LOG_FILE_NAME)
    private val json = Json { ignoreUnknownKeys = true }
    private val pending = Channel<SyncLogEntry>(capacity = Channel.UNLIMITED)
    private val mutableEntries = MutableStateFlow<List<SyncLogEntry>>(emptyList())

    init {
        scope.launch(Dispatchers.IO) {
            mutableEntries.value = readPersisted()
            append(
                SyncLogEntry(
                    at = Clock.System.now(),
                    level = SyncLogLevel.INFO,
                    tag = SyncLogTag.SESSION,
                    message = "--- app process started ---",
                    networkState = context.describeNetworkState(),
                ),
            )
            for (entry in pending) {
                append(entry)
            }
        }
    }

    override fun observeEntries(): Flow<List<SyncLogEntry>> = mutableEntries.asStateFlow()

    override fun log(level: SyncLogLevel, tag: SyncLogTag, message: String, marketHashName: String?) {
        pending.trySend(
            SyncLogEntry(
                at = Clock.System.now(),
                level = level,
                tag = tag,
                message = message,
                marketHashName = marketHashName,
                // Read here rather than in the consumer: by the time the queue drains, connectivity
                // may already have changed into something that no longer explains the entry.
                networkState = context.describeNetworkState(),
            ),
        )
    }

    override fun clear() {
        pending.trySend(CLEAR_MARKER)
    }

    private fun append(entry: SyncLogEntry) {
        if (entry === CLEAR_MARKER) {
            runCatching { file.writeText("") }
            mutableEntries.value = emptyList()
            return
        }
        runCatching {
            file.appendText(json.encodeToString(entry.toPersisted()) + "\n")
            if (file.length() > MAX_LOG_BYTES) trimOldest()
        }
        mutableEntries.value = mutableEntries.value + entry
    }

    /**
     * Drops the older half in one rewrite rather than trimming a line at a time, so the cost is paid
     * once per [MAX_LOG_BYTES] instead of on every append past the limit.
     */
    private fun trimOldest() {
        val kept = file.readLines().let { it.drop(it.size / KEPT_ENTRY_FRACTION) }
        file.writeText(kept.joinToString(separator = "\n", postfix = "\n"))
        mutableEntries.value = kept.mapNotNull { it.toEntryOrNull() }
    }

    private fun readPersisted(): List<SyncLogEntry> =
        runCatching { file.readLines() }.getOrDefault(emptyList()).mapNotNull { it.toEntryOrNull() }

    private fun String.toEntryOrNull(): SyncLogEntry? {
        if (isBlank()) return null
        return try {
            json.decodeFromString<PersistedSyncLogEntry>(this).toDomain()
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        /**
         * Queued like an ordinary entry so a clear cannot overtake writes that were logged before it
         * and silently survive the wipe. Identity-compared, never persisted.
         */
        val CLEAR_MARKER = SyncLogEntry(
            at = Instant.DISTANT_PAST,
            level = SyncLogLevel.INFO,
            tag = SyncLogTag.SESSION,
            message = "clear",
        )
    }
}
