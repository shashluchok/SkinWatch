package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__sync_status_bar__days_ago
import com.shashluchok.skinwatch.resources.dev__screen_inventory__sync_status_bar__hours_ago
import com.shashluchok.skinwatch.resources.dev__screen_inventory__sync_status_bar__minutes_ago
import com.shashluchok.skinwatch.resources.dev__screen_inventory__sync_status_bar__never_synced
import com.shashluchok.skinwatch.resources.dev__screen_inventory__sync_status_bar__sync_now__content_description
import com.shashluchok.skinwatch.resources.dev__screen_inventory__sync_status_bar__synced_just_now
import com.shashluchok.skinwatch.resources.dev__screen_inventory__sync_status_bar__syncing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// Re-read threshold -- keeps the numeric "N minutes/hours/days ago" wording honest for as long as
// the bar stays on screen (see design addendum section 2). Also doubles as the "just now" boundary.
private val FRESHNESS_REFRESH_INTERVAL = 1.minutes
private val JUST_NOW_THRESHOLD = 1.minutes
private val HOUR_THRESHOLD = 1.hours
private val DAY_THRESHOLD = 24.hours

@Composable
internal fun SyncStatusBar(
    lastSyncedAt: Instant?,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Column(modifier = modifier.testTag(SyncStatusBar.Tag.ROOT)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = dimens.padding.medium, end = dimens.padding.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.testTag(SyncStatusBar.Tag.STATUS_TEXT),
                text = syncStatusText(lastSyncedAt = lastSyncedAt, isSyncing = isSyncing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing,
                modifier = Modifier.testTag(SyncStatusBar.Tag.SYNC_BUTTON),
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(dimens.iconSize.medium)
                            .testTag(SyncStatusBar.Tag.PROGRESS),
                        strokeWidth = dimens.border.thick,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(
                            Res.string.dev__screen_inventory__sync_status_bar__sync_now__content_description,
                        ),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun syncStatusText(
    lastSyncedAt: Instant?,
    isSyncing: Boolean,
): String = when {
    isSyncing -> stringResource(Res.string.dev__screen_inventory__sync_status_bar__syncing)
    lastSyncedAt == null -> stringResource(Res.string.dev__screen_inventory__sync_status_bar__never_synced)
    else -> freshnessText(lastSyncedAt)
}

@Composable
private fun freshnessText(lastSyncedAt: Instant): String {
    // Re-reads the clock every FRESHNESS_REFRESH_INTERVAL so the wording advances on its own while
    // this bar stays composed (e.g. "just now" -> "1 minute ago" without user interaction) -- a
    // one-shot read at first composition would freeze the label at whatever value applied when the
    // bar first appeared. Keyed on lastSyncedAt so a freshly completed run resets the ticker
    // immediately instead of waiting for the next tick.
    var now by remember(lastSyncedAt) { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(lastSyncedAt) {
        while (true) {
            delay(FRESHNESS_REFRESH_INTERVAL)
            now = Clock.System.now()
        }
    }
    val elapsed = now - lastSyncedAt

    return when {
        elapsed < JUST_NOW_THRESHOLD -> stringResource(
            Res.string.dev__screen_inventory__sync_status_bar__synced_just_now,
        )
        elapsed < HOUR_THRESHOLD -> agoText(
            resource = Res.plurals.dev__screen_inventory__sync_status_bar__minutes_ago,
            count = elapsed.inWholeMinutes,
        )
        elapsed < DAY_THRESHOLD -> agoText(
            resource = Res.plurals.dev__screen_inventory__sync_status_bar__hours_ago,
            count = elapsed.inWholeHours,
        )
        else -> agoText(
            resource = Res.plurals.dev__screen_inventory__sync_status_bar__days_ago,
            count = elapsed.inWholeDays,
        )
    }
}

@Composable
private fun agoText(
    resource: PluralStringResource,
    count: Long,
): String {
    val quantity = count.toInt()
    return pluralStringResource(resource, quantity, quantity)
}

internal object SyncStatusBar {
    object Tag {
        const val ROOT = "SyncStatusBar"
        const val STATUS_TEXT = "$ROOT.statusText"
        const val PROGRESS = "$ROOT.progress"
        const val SYNC_BUTTON = "$ROOT.syncButton"
    }
}
