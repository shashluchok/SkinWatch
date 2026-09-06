package com.shashluchok.skinwatch.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__relative_time__days_ago
import com.shashluchok.skinwatch.resources.dev__relative_time__hours_ago
import com.shashluchok.skinwatch.resources.dev__relative_time__just_now
import com.shashluchok.skinwatch.resources.dev__relative_time__minutes_ago
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val REFRESH_INTERVAL = 1.minutes
private val JUST_NOW_THRESHOLD = 1.minutes
private val HOUR_THRESHOLD = 1.hours
private val DAY_THRESHOLD = 24.hours

/**
 * How long ago [instant] was, worded on its own ("5 минут назад") so callers can place it in their
 * own sentence.
 *
 * Re-reads the clock every [REFRESH_INTERVAL] so the wording advances while this stays composed --
 * a single read at first composition would freeze the label at whatever applied back then.
 */
@Composable
internal fun relativeTimeText(instant: Instant): String {
    // Keyed on the instant so a freshly updated value restarts the ticker instead of waiting out
    // the remainder of the current tick.
    var now by remember(instant) { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(instant) {
        while (true) {
            delay(REFRESH_INTERVAL)
            now = Clock.System.now()
        }
    }
    val elapsed = now - instant

    return when {
        elapsed < JUST_NOW_THRESHOLD -> stringResource(Res.string.dev__relative_time__just_now)
        elapsed < HOUR_THRESHOLD -> agoText(
            resource = Res.plurals.dev__relative_time__minutes_ago,
            count = elapsed.inWholeMinutes,
        )

        elapsed < DAY_THRESHOLD -> agoText(
            resource = Res.plurals.dev__relative_time__hours_ago,
            count = elapsed.inWholeHours,
        )

        else -> agoText(
            resource = Res.plurals.dev__relative_time__days_ago,
            count = elapsed.inWholeDays,
        )
    }
}

@Composable
private fun agoText(resource: PluralStringResource, count: Long): String {
    val quantity = count.toInt()

    return pluralStringResource(resource, quantity, quantity)
}
