package com.shashluchok.skinwatch.presentation.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

/**
 * Adds to the vertical padding, leaving the horizontal sides as they are.
 *
 * Screens use this to put the app bars' heights back into their own scrollable padding: the main
 * screen leaves both out of what it hands down, so content can be seen sliding under them.
 */
@Composable
internal fun PaddingValues.plusVertical(top: Dp, bottom: Dp): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    return PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding() + top,
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + bottom,
    )
}
