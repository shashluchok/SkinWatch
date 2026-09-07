package com.shashluchok.skinwatch.presentation.screen.settings.component.synclog

import com.shashluchok.skinwatch.domain.synclog.SyncLogEntry
import com.shashluchok.skinwatch.presentation.util.displayTimeZone
import com.shashluchok.skinwatch.presentation.util.pad2
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val MILLIS_PAD_LENGTH = 3
private const val LEVEL_PAD_LENGTH = 5
private const val MILLIS_IN_SECOND = 1000

/**
 * Renders entries the same way for the on-screen list and the exported file, so a line quoted from
 * one is searchable in the other.
 */
internal object SyncLogFormatter {
    fun formatTimestamp(at: Instant): String {
        val local = at.toLocalDateTime(displayTimeZone)
        val millis = (at.toEpochMilliseconds() % MILLIS_IN_SECOND)
            .toString()
            .padStart(length = MILLIS_PAD_LENGTH, padChar = '0')
        return "${local.day.pad2()}.${local.month.number.pad2()} " +
            "${local.hour.pad2()}:${local.minute.pad2()}:${local.second.pad2()}.$millis"
    }

    fun formatLine(entry: SyncLogEntry): String = buildString {
        append(formatTimestamp(entry.at))
        append(' ')
        append(entry.level.name.padEnd(LEVEL_PAD_LENGTH))
        append(" [")
        append(entry.tag.name)
        append(']')
        entry.networkState?.let {
            append(" {")
            append(it)
            append('}')
        }
        entry.marketHashName?.let {
            append(' ')
            append(it)
            append(" --")
        }
        append(' ')
        append(entry.message)
    }

    fun formatExport(entries: List<SyncLogEntry>): String = buildString {
        appendLine("SkinWatch price sync log")
        appendLine("entries: ${entries.size}")
        appendLine("timezone: $displayTimeZone")
        appendLine()
        entries.forEach { appendLine(formatLine(it)) }
    }
}
