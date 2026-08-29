package com.shashluchok.skinwatch.presentation.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** "dd.MM.yyyy" in fixed UTC, not the device's local timezone. */
internal fun Instant.toFullDateLabel(): String {
    val date = toLocalDateTime(TimeZone.UTC).date
    return "${date.day.pad2()}.${date.month.number.pad2()}.${date.year}"
}

internal fun Int.pad2(): String = toString().padStart(length = 2, padChar = '0')
