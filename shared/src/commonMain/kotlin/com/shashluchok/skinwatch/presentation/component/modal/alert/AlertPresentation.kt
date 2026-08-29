package com.shashluchok.skinwatch.presentation.component.modal.alert

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shashluchok.skinwatch.presentation.component.modal.host.LocalModalHost
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalRequest
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalAnimatedVisibilityScope
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalSharedElementKeyTransition
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__scrim__content_description
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import org.jetbrains.compose.resources.stringResource

private const val ALERT_PROGRESS_LABEL = "AlertProgress"
private val ALERT_BLUR_MAX_RADIUS = 8.dp
private const val ALERT_SCRIM_MAX_ALPHA = 0.6f

/**
 * Blurs/dims the whole app behind whichever [ModalRequest.Appearance.Alert] request is currently open and
 * hosts its content once [LocalModalHost]'s current request becomes non-null. Kept mounted via
 * [LocalSharedElementKeyTransition]'s `AnimatedVisibility` (driven by a boolean, not by conditionally
 * composing on the request itself) so the outgoing content and its shared elements can keep
 * animating out after the request has already gone back to null -- the same reason
 * [ModalRequest.onDismissRequest] resets state immediately but the visuals linger for the fade.
 */
@Composable
internal fun AlertPresentation(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val transition = LocalSharedElementKeyTransition.current
    val alertRequest = LocalModalHost.current.currentRequest?.takeIf { it.appearance is ModalRequest.Appearance.Alert }
    val motion = LocalMotion.current

    val animationSpec = tween<Float>(durationMillis = motion.duration.standard, easing = motion.easing.standard)
    val blurProgress by transition.animateFloat(
        label = ALERT_PROGRESS_LABEL,
        transitionSpec = { animationSpec },
    ) { openedKey -> if (openedKey != null) 1f else 0f }

    // alertRequest goes back to null the instant onDismissRequest fires, but the overlay still
    // needs its last content while AnimatedVisibility fades it out below.
    var shownRequest by remember { mutableStateOf(alertRequest) }
    if (alertRequest != null) shownRequest = alertRequest

    transition.AnimatedVisibility(
        visible = { openedKey -> openedKey != null },
        enter = fadeIn(animationSpec),
        exit = fadeOut(animationSpec),
        modifier = modifier,
    ) {
        val current = shownRequest ?: return@AnimatedVisibility

        Box(modifier = Modifier.fillMaxSize()) {
            AlertBlurScrim(
                hazeState = hazeState,
                blurProgress = blurProgress,
                onDismissClick = current.onDismissRequest,
            )
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@AnimatedVisibility) {
                current.content()
            }
        }
    }
}

@Composable
private fun AlertBlurScrim(
    hazeState: HazeState,
    blurProgress: Float,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrimColor = MaterialTheme.colorScheme.background

    val dismissLabel =
        stringResource(Res.string.dev__screen_inventory__price_history_detail__scrim__content_description)
    Canvas(
        // Fading the whole layer's alpha (cheap: a composite) rather than the blur radius itself
        // (expensive: reconfigures Haze's blur shader every frame) is what keeps this animation
        // smooth -- the radius stays fixed at its final value throughout.
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = blurProgress }
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                performanceMode = HazePerformanceMode.Balanced,
                style = HazeBlurStyle { blurRadius(ALERT_BLUR_MAX_RADIUS) },
            ).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = dismissLabel,
                role = Role.Button,
                onClick = onDismissClick,
            ),
    ) {
        // A fully opaque rect here would completely hide the blur beneath it -- the scrim is meant
        // to dim the blurred background, not replace it with a flat color.
        drawRect(color = scrimColor, alpha = ALERT_SCRIM_MAX_ALPHA)
    }
}
