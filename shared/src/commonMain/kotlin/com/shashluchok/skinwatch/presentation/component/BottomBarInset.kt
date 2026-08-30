package com.shashluchok.skinwatch.presentation.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Extra bottom inset a screen's own scrollable content should add on top of its usual content
 * padding -- set by the app's main screen to the measured height of its translucent bottom
 * navigation bar, so list items can still scroll fully clear of it instead of settling
 * underneath where they'd be unreachable.
 */
internal val LocalBottomBarInset = staticCompositionLocalOf { 0.dp }
