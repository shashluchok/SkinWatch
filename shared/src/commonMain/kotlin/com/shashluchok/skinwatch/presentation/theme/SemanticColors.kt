package com.shashluchok.skinwatch.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class SemanticColors(
    val positive: Color = Color.Unspecified,
    val negative: Color = Color.Unspecified,
) {
    companion object {
        private val LIGHT =
            SemanticColors(
                positive = Color(0xFF128A6C),
                negative = Color(0xFFB22B3A),
            )
        private val DARK =
            SemanticColors(
                positive = Color(0xFF33C9A7),
                negative = Color(0xFFE5555F),
            )

        val DEFAULT
            @Composable get() = if (isSystemInDarkTheme()) DARK else LIGHT
    }
}

internal val LocalSemanticColors =
    staticCompositionLocalOf<SemanticColors> { error("No semantic colors provided") }
