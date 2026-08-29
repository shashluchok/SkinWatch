package com.shashluchok.skinwatch.domain.pricesnapshot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class PriceSnapshotRetentionCalculationsTest {
    private val now = Instant.parse("2026-08-29T12:00:00Z")

    @Test
    fun `nothing is deleted when every snapshot is within the last 7 days`() {
        val snapshots = listOf(
            1L to now - 6.days,
            2L to now - 3.days,
            3L to now,
        )

        assertEquals(emptyList(), idsToDeleteForRetention(snapshots = snapshots, now = now))
    }

    @Test
    fun `snapshots between 7 and 90 days old are thinned to the last one per calendar day`() {
        val day = Instant.parse("2026-07-30T00:00:00Z")
        val snapshots = listOf(
            1L to day,
            2L to day + 6.hours,
            3L to day + 12.hours,
            4L to day + 18.hours,
        )

        val idsToDelete = idsToDeleteForRetention(snapshots = snapshots, now = now)

        assertEquals(setOf(1L, 2L, 3L), idsToDelete.toSet())
    }

    @Test
    fun `snapshots older than 90 days are thinned to the last one per week`() {
        // 1970-01-01 is epoch day 0, which lands in week bucket 0 together with the next 6 days.
        val weekStart = Instant.parse("1970-01-01T00:00:00Z")
        val snapshots = listOf(
            1L to weekStart,
            2L to weekStart + 2.days,
            3L to weekStart + 4.days,
            4L to weekStart + 6.days,
        )

        val idsToDelete = idsToDeleteForRetention(snapshots = snapshots, now = now)

        assertEquals(setOf(1L, 2L, 3L), idsToDelete.toSet())
    }

    @Test
    fun `each tier is thinned independently across a mixed history`() {
        val recent = now - 1.days
        val midDay = Instant.parse("2026-07-30T00:00:00Z")
        val old = Instant.parse("1970-01-01T00:00:00Z")
        val snapshots = listOf(
            1L to recent,
            2L to midDay,
            3L to midDay + 12.hours,
            4L to old,
            5L to old + 3.days,
        )

        val idsToDelete = idsToDeleteForRetention(snapshots = snapshots, now = now)

        assertEquals(setOf(2L, 4L), idsToDelete.toSet())
    }

    @Test
    fun `a lone snapshot in an older bucket is never deleted`() {
        val snapshots = listOf(1L to now - 30.days, 2L to now - 200.days)

        assertEquals(emptyList(), idsToDeleteForRetention(snapshots = snapshots, now = now))
    }

    @Test
    fun `an empty history deletes nothing`() {
        assertEquals(emptyList(), idsToDeleteForRetention(snapshots = emptyList(), now = now))
    }
}
