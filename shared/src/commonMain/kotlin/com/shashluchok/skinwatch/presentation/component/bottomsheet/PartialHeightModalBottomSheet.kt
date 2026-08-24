package com.shashluchok.skinwatch.presentation.component.bottomsheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private const val PARTIAL_HEIGHT_FRACTION = 2f / 3f
private const val SCRIM_ALPHA_ANIMATION_LABEL = "PartialHeightModalBottomSheetScrimAlpha"
private const val SCRIM_BLUR_RADIUS_ANIMATION_LABEL = "PartialHeightModalBottomSheetScrimBlurRadius"
private const val SCRIM_GRADIENT_OPAQUE_STOP = 0.85f
private val VISIBLE_SCRIM_BLUR_RADIUS = 10.dp

/**
 * The single app-level bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartialHeightModalBottomSheet(
    onDismissRequest: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val motionScheme = MaterialTheme.motionScheme

    val scrimAlpha = remember {
        Animatable(
            initialValue = 0f,
            typeConverter = Float.VectorConverter,
            label = SCRIM_ALPHA_ANIMATION_LABEL,
        )
    }
    val scrimBlurRadius = remember {
        Animatable(
            initialValue = 0.dp,
            typeConverter = Dp.VectorConverter,
            label = SCRIM_BLUR_RADIUS_ANIMATION_LABEL,
        )
    }

    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(targetValue = 1f, animationSpec = motionScheme.defaultEffectsSpec()) }
        launch {
            scrimBlurRadius.animateTo(
                targetValue = VISIBLE_SCRIM_BLUR_RADIUS,
                animationSpec = motionScheme.defaultEffectsSpec(),
            )
        }
    }

    BlurScrim(
        hazeState = hazeState,
        sheetState = sheetState,
        containerColor = containerColor,
        scrimAlpha = scrimAlpha.value,
        scrimBlurRadius = scrimBlurRadius.value,
    )

    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                joinAll(
                    launch {
                        scrimAlpha.animateTo(targetValue = 0f, animationSpec = motionScheme.slowEffectsSpec())
                    },
                    launch {
                        scrimBlurRadius.animateTo(targetValue = 0.dp, animationSpec = motionScheme.slowEffectsSpec())
                    },
                )
                onDismissRequest()
            }
        },
        sheetState = sheetState,
        modifier = modifier.fillMaxHeight(),
        containerColor = containerColor,
        scrimColor = Color.Transparent,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        val heightFraction =
            if (sheetState.targetValue == SheetValue.Expanded) 1f else PARTIAL_HEIGHT_FRACTION
        Box(modifier = Modifier.fillMaxHeight(heightFraction)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlurScrim(
    hazeState: HazeState,
    sheetState: SheetState,
    containerColor: Color,
    scrimAlpha: Float,
    scrimBlurRadius: Dp,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                performanceMode = HazePerformanceMode.Balanced,
                style = HazeBlurStyle { blurRadius(scrimBlurRadius) },
            ),
    ) {
        val sheetTopEdge =
            runCatching { sheetState.requireOffset() }.getOrDefault(size.height).coerceAtLeast(0f)
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    SCRIM_GRADIENT_OPAQUE_STOP to containerColor,
                ),
                startY = 0f,
                endY = sheetTopEdge,
            ),
            alpha = scrimAlpha,
        )
    }
}
