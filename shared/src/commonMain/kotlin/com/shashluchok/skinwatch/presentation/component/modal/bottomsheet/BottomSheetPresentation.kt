package com.shashluchok.skinwatch.presentation.component.modal.bottomsheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.shashluchok.skinwatch.presentation.component.modal.host.LocalModalHost
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalRequest
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.launch

// Material rests the partially expanded sheet at half its container's height. Both reveal curves
// are normalised against that, so they complete when the sheet has settled rather than at an
// assumed fraction of the window.
private const val PARTIALLY_EXPANDED_HEIGHT_FRACTION = 0.5f

// The scrim finishes ahead of the blur, at this fraction of the sheet's travel to its resting
// height -- the same lead the two curves had before they were tied to real geometry.
private const val SCRIM_LEAD_FRACTION = 0.5f

private const val SCRIM_GRADIENT_OPAQUE_STOP = 0.85f
private const val SCRIM_GRADIENT_STOP_COUNT = 7
private val VISIBLE_SCRIM_BLUR_RADIUS = 8.dp
private val DRAG_HANDLE_FADE_DISTANCE = 48.dp
private val DRAG_HANDLE_WIDTH = 32.dp
private val DRAG_HANDLE_HEIGHT = 4.dp
private val DRAG_HANDLE_PADDING = 2.dp
private val DRAG_HANDLE_TOP_PADDING = 22.dp
private const val DRAG_HANDLE_BACKGROUND_ALPHA = 0.12f

/**
 * The single app-level bottom sheet. Unlike `AlertPresentation`, this can be gated on
 * [LocalModalHost]'s current request being non-null: `ModalBottomSheet` plays its own hide
 * animation while it stays composed, it doesn't need to be kept mounted past that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomSheetPresentation(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
) {
    val request =
        LocalModalHost.current.currentRequest?.takeIf { it.appearance == ModalRequest.Appearance.BottomSheet }
            ?: return
    val onDismissRequest = request.onDismissRequest
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val density = LocalDensity.current
    val easing = LocalMotion.current.easing.standard
    val scope = rememberCoroutineScope()

    val windowHeightPx = with(density) {
        LocalWindowInfo.current.containerDpSize.height
            .toPx()
    }
    val imeInsets = WindowInsets.ime
    val statusBarInsets = WindowInsets.statusBars
    val dragHandleFadeDistancePx = with(density) { DRAG_HANDLE_FADE_DISTANCE.toPx() }

    val sheetOffsetPx: () -> Float = {
        runCatching { sheetState.requireOffset() }.getOrDefault(windowHeightPx)
    }

    val sheetProperties = remember { ModalBottomSheetProperties(shouldDismissOnBackPress = false) }

    val revealFraction: () -> Float = {
        sheetRevealFraction(
            windowHeightPx = windowHeightPx,
            imeBottomPx = imeInsets.getBottom(density),
            sheetOffsetPx = sheetOffsetPx(),
        )
    }

    BlurScrim(
        hazeState = hazeState,
        containerColor = containerColor,
        blurProgress = revealFraction,
        scrimAlpha = {
            val scrimFraction = (revealFraction() / SCRIM_LEAD_FRACTION)
                .coerceIn(minimumValue = 0f, maximumValue = 1f)
            easing.transform(scrimFraction)
        },
        sheetOffsetPx = sheetOffsetPx,
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
            .fillMaxHeight()
            .animateContentSize(),
        containerColor = containerColor,
        scrimColor = Color.Transparent,
        dragHandle = {
            DragHandle(
                revealProgress = {
                    easing.transform(
                        dragHandleRevealFraction(
                            sheetOffsetPx = sheetOffsetPx(),
                            statusBarTopPx = statusBarInsets.getTop(density),
                            fadeDistancePx = dragHandleFadeDistancePx,
                        ),
                    )
                },
            )
        },
        properties = sheetProperties,
    ) {
        NavigationBackHandler(
            state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
            isBackEnabled = sheetState.isVisible,
            onBackCompleted = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
            },
        )

        request.content()
    }
}

private fun sheetRevealFraction(windowHeightPx: Float, imeBottomPx: Int, sheetOffsetPx: Float): Float {
    val containerHeightPx = windowHeightPx - imeBottomPx
    val restingHeightPx = containerHeightPx * PARTIALLY_EXPANDED_HEIGHT_FRACTION

    // Guard the height-unknown case explicitly: dividing by zero yields NaN, which coerceIn passes
    // through unchanged (NaN comparisons are always false) instead of clamping it, so a plain
    // division would flash a fully-opaque scrim for one frame before layout.
    if (restingHeightPx <= 0f) return 0f

    val visibleHeightPx = (containerHeightPx - sheetOffsetPx).coerceAtLeast(0f)

    return (visibleHeightPx / restingHeightPx).coerceIn(minimumValue = 0f, maximumValue = 1f)
}

/** Fades the handle in over the sheet's first [DRAG_HANDLE_FADE_DISTANCE] of travel. */
private fun dragHandleRevealFraction(sheetOffsetPx: Float, statusBarTopPx: Int, fadeDistancePx: Float): Float {
    val travelPx = sheetOffsetPx - statusBarTopPx

    return (travelPx / fadeDistancePx).coerceIn(minimumValue = 0f, maximumValue = 1f)
}

@Composable
private fun DragHandle(revealProgress: () -> Float, modifier: Modifier = Modifier) {
    val backgroundColor =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DRAG_HANDLE_BACKGROUND_ALPHA)

    Box(
        modifier = modifier
            .padding(top = DRAG_HANDLE_TOP_PADDING)
            .graphicsLayer {
                val progress = revealProgress()
                scaleX = progress
                alpha = progress
            }.background(color = backgroundColor, shape = CircleShape)
            .padding(DRAG_HANDLE_PADDING),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = CircleShape,
        ) {
            Box(modifier = Modifier.size(width = DRAG_HANDLE_WIDTH, height = DRAG_HANDLE_HEIGHT))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlurScrim(
    hazeState: HazeState,
    containerColor: Color,
    blurProgress: () -> Float,
    scrimAlpha: () -> Float,
    sheetOffsetPx: () -> Float,
    modifier: Modifier = Modifier,
) {
    val gradientColorStops = rememberScrimGradientColorStops(containerColor = containerColor)

    val blurRadiusDp = VISIBLE_SCRIM_BLUR_RADIUS * blurProgress()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                performanceMode = HazePerformanceMode.Balanced,
                style = HazeBlurStyle { blurRadius(blurRadiusDp) },
            ),
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = gradientColorStops,
                startY = 0f,
                endY = sheetOffsetPx().coerceAtLeast(0f),
            ),
            alpha = scrimAlpha(),
        )
    }
}

@Composable
private fun rememberScrimGradientColorStops(containerColor: Color): Array<Pair<Float, Color>> {
    val easing = LocalMotion.current.easing.standard
    return remember(containerColor, easing) {
        Array(SCRIM_GRADIENT_STOP_COUNT) { index ->
            val fraction = index / (SCRIM_GRADIENT_STOP_COUNT - 1f)
            val position = SCRIM_GRADIENT_OPAQUE_STOP * fraction
            val alpha = easing.transform(fraction)
            position to containerColor.copy(alpha = alpha)
        }
    }
}
