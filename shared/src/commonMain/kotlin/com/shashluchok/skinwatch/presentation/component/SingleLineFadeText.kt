package com.shashluchok.skinwatch.presentation.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FADE_WIDTH = 24.dp

@Composable
internal fun SingleLineFadeText(
    text: String,
    fadeColor: Color,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current,
    fadeWidth: Dp = FADE_WIDTH,
) {
    Text(
        text = text,
        modifier = modifier.drawWithContent {
            drawContent()
            val fadePx = fadeWidth.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(fadeColor.copy(alpha = 0f), fadeColor),
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                topLeft = Offset(x = size.width - fadePx, y = 0f),
                size = Size(width = fadePx, height = size.height),
            )
        },
        color = color,
        style = style,
        softWrap = false,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}
