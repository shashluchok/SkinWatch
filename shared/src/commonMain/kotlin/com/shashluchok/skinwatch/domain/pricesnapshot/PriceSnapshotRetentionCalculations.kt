package com.shashluchok.skinwatch.domain.pricesnapshot

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private val FULL_RESOLUTION_WINDOW = 7.days
private val DAILY_RESOLUTION_WINDOW = 90.days
private const val WEEK_LENGTH_DAYS = 7

internal fun idsToDeleteForRetention(snapshots: List<Pair<Long, Instant>>, now: Instant): List<Long> {
    val dailyThreshold = now - FULL_RESOLUTION_WINDOW
    val weeklyThreshold = now - DAILY_RESOLUTION_WINDOW

    val daily = snapshots.filter { (_, capturedAt) -> capturedAt in weeklyThreshold..<dailyThreshold }
    val weekly = snapshots.filter { (_, capturedAt) -> capturedAt < weeklyThreshold }

    return idsToDeleteForGroups(snapshots = daily, groupKey = { it.epochDay() }) +
        idsToDeleteForGroups(snapshots = weekly, groupKey = { it.epochDay() / WEEK_LENGTH_DAYS })
}

private fun idsToDeleteForGroups(snapshots: List<Pair<Long, Instant>>, groupKey: (Instant) -> Long): List<Long> =
    snapshots
        .groupBy { (_, capturedAt) -> groupKey(capturedAt) }
        .values
        .flatMap { group -> group.minus(group.maxBy { (_, capturedAt) -> capturedAt }) }
        .map { (id, _) -> id }

private fun Instant.epochDay(): Long = toLocalDateTime(TimeZone.UTC).date.toEpochDays()
