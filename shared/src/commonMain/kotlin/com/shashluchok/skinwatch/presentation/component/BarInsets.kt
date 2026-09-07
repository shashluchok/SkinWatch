package com.shashluchok.skinwatch.presentation.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Extra insets a screen's own scrollable content should add on top of its usual content padding.
 *
 * Both bars are translucent and content passes underneath them, so the main screen deliberately
 * leaves them out of the padding it hands down and publishes their measured heights here instead.
 * A list then scrolls its first and last items fully clear of the bars rather than parking them
 * where they cannot be read or reached, while still showing content sliding under the blur.
 */
internal val LocalTopBarInset = compositionLocalOf { 0.dp }

/** @see LocalTopBarInset */
internal val LocalBottomBarInset = compositionLocalOf { 0.dp }
