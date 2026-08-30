package com.shashluchok.skinwatch.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val FADE_DURATION_MS = 300
private const val TRANSITION_LABEL = "AnimatedFadeText"

@Composable
internal fun AnimatedFadeText(
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = FADE_DURATION_MS)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = FADE_DURATION_MS))
        },
        label = TRANSITION_LABEL,
    ) { targetText ->
        Text(text = targetText)
    }
}
