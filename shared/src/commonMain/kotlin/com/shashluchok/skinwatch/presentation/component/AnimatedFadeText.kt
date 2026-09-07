package com.shashluchok.skinwatch.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import kotlin.time.Duration

private const val TRANSITION_LABEL = "AnimatedFadeText"

/**
 * Text that fades between values instead of swapping instantly.
 *
 * [startDelay] lets several of these be staggered against one another, so a group that changes
 * together still reads in the order it should be read in rather than all at once.
 */
@Composable
internal fun AnimatedFadeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    startDelay: Duration = Duration.ZERO,
    contentAlignment: Alignment = Alignment.TopStart,
) {
    val delayMillis = startDelay.inWholeMilliseconds.toInt()
    // Taken from the theme rather than kept as a constant here, so anything that has to move in step
    // with this fade -- a colour crossing at the same time, for one -- can ask for the same token.
    val durationMillis = LocalMotion.current.duration.standard

    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis),
            ) togetherWith
                fadeOut(
                    animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis),
                )
        },
        contentAlignment = contentAlignment,
        label = TRANSITION_LABEL,
    ) { targetText ->
        Text(text = targetText, style = style, color = color)
    }
}
