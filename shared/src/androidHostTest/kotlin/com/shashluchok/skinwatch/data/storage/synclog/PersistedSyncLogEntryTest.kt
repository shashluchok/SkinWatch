package com.shashluchok.skinwatch.data.storage.synclog

import com.shashluchok.skinwatch.domain.synclog.SyncLogEntry
import com.shashluchok.skinwatch.domain.synclog.SyncLogLevel
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val AT = Instant.fromEpochMilliseconds(1_700_000_000_123)

class PersistedSyncLogEntryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an entry survives a round trip through the persisted form`() {
        val entry = SyncLogEntry(
            at = AT,
            level = SyncLogLevel.ERROR,
            tag = SyncLogTag.HTTP,
            message = "failed with\na newline and a \"quote\"",
            marketHashName = "AK-47 | Redline (Field-Tested)",
            networkState = "wifi+unvalidated",
        )

        val restored = json
            .decodeFromString<PersistedSyncLogEntry>(json.encodeToString(entry.toPersisted()))
            .toDomain()

        assertEquals(entry, restored)
    }

    @Test
    fun `one entry stays on one line so the file can be read back line by line`() {
        val encoded = json.encodeToString(
            SyncLogEntry(
                at = AT,
                level = SyncLogLevel.INFO,
                tag = SyncLogTag.RUN,
                message = "first line\nsecond line",
            ).toPersisted(),
        )

        assertEquals(1, encoded.lines().size)
    }

    @Test
    fun `an unknown enum name from an older build degrades instead of failing the whole file`() {
        val persisted = PersistedSyncLogEntry(
            epochMillis = AT.toEpochMilliseconds(),
            level = "SOMETHING_NEW",
            tag = "SOMETHING_NEW",
            message = "still readable",
        )

        val restored = persisted.toDomain()

        assertEquals(SyncLogLevel.INFO, restored.level)
        assertEquals("still readable", restored.message)
    }
}
