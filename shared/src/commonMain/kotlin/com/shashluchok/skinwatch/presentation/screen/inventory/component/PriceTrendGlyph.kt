package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.shashluchok.skinwatch.presentation.theme.LocalDimens

/**
 * Normalized (fraction of glyph width/height) points for [PriceTrendGlyph]'s static up-down-up
 * polyline -- deliberately not the same "breathing glow" treatment as `NavTab.GlowIcon`, which is
 * reserved for a single selected tab at a time, not many simultaneously visible cards.
 */
private val priceTrendGlyphPoints = listOf(
    Offset(x = 0f, y = 0.70f),
    Offset(x = 0.33f, y = 0.30f),
    Offset(x = 0.66f, y = 0.55f),
    Offset(x = 1f, y = 0.15f),
)

@Composable
internal fun PriceTrendGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val strokeWidth = LocalDimens.current.border.thin
    Canvas(modifier = modifier) {
        val path = Path().apply {
            priceTrendGlyphPoints.forEachIndexed { index, point ->
                val offset = Offset(x = point.x * size.width, y = point.y * size.height)
                if (index == 0) moveTo(x = offset.x, y = offset.y) else lineTo(x = offset.x, y = offset.y)
            }
        }
        drawPath(path = path, color = color, style = Stroke(width = strokeWidth.toPx()))
    }
}
