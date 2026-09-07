package com.shashluchok.skinwatch.presentation.screen.settings.component.synclog

import com.shashluchok.skinwatch.domain.synclog.SyncLogEntry
import com.shashluchok.skinwatch.domain.synclog.SyncLogLevel
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private val AT = Instant.fromEpochMilliseconds(1_700_000_000_000)

class SyncLogFormatterTest {
    private fun entry(
        level: SyncLogLevel = SyncLogLevel.INFO,
        tag: SyncLogTag = SyncLogTag.ITEM,
        message: String = "fetched",
        marketHashName: String? = null,
        networkState: String? = null,
    ) = SyncLogEntry(
        at = AT,
        level = level,
        tag = tag,
        message = message,
        marketHashName = marketHashName,
        networkState = networkState,
    )

    @Test
    fun `a line carries the level, tag and message`() {
        val line = SyncLogFormatter.formatLine(entry(level = SyncLogLevel.ERROR, message = "boom"))

        assertTrue("ERROR" in line)
        assertTrue("[ITEM]" in line)
        assertTrue("boom" in line)
    }

    @Test
    fun `optional context appears only when the entry carries it`() {
        val bare = SyncLogFormatter.formatLine(entry())
        val full = SyncLogFormatter.formatLine(
            entry(marketHashName = "AK-47 | Redline", networkState = "wifi"),
        )

        assertFalse("{" in bare)
        assertTrue("{wifi}" in full)
        assertTrue("AK-47 | Redline" in full)
    }

    @Test
    fun `the export keeps one line per entry under its header`() {
        val text = SyncLogFormatter.formatExport(List(size = 3) { entry(message = "line $it") })
        val lines = text.trim().lines()

        assertEquals(3, lines.count { "line" in it })
        assertTrue(lines.any { "entries: 3" in it })
    }
}
