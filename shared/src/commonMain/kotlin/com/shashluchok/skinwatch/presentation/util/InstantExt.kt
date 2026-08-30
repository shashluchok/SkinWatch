package com.shashluchok.skinwatch.presentation.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal val displayTimeZone: TimeZone
    get() = runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC)

/** "dd.MM.yyyy" in the device's local timezone -- see [displayTimeZone]. */
internal fun Instant.toFullDateLabel(): String {
    val date = toLocalDateTime(displayTimeZone).date
    return "${date.day.pad2()}.${date.month.number.pad2()}.${date.year}"
}

internal fun Int.pad2(): String = toString().padStart(length = 2, padChar = '0')
