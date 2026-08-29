package com.shashluchok.skinwatch.presentation.component.modal.bottomsheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
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

private const val PARTIAL_HEIGHT_FRACTION = 2f / 3f
private const val SCRIM_GRADIENT_OPAQUE_STOP = 0.85f
private const val SCRIM_GRADIENT_STOP_COUNT = 10
private val VISIBLE_SCRIM_BLUR_RADIUS = 8.dp
private val DRAG_HANDLE_FADE_DISTANCE = 96.dp
private val DRAG_HANDLE_WIDTH = 32.dp
private val DRAG_HANDLE_HEIGHT = 4.dp
private val DRAG_HANDLE_PADDING = 2.dp
private val DRAG_HANDLE_GAP_ABOVE_CONTENT = 28.dp
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
    val request = LocalModalHost.current.currentRequest?.takeIf { it.appearance == ModalRequest.Appearance.BottomSheet }
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
    val sheetOffsetPx = runCatching { sheetState.requireOffset() }
        .getOrDefault(windowHeightPx)
    val sheetHeight = windowHeightPx - sheetOffsetPx
    val partiallyExpandedOffsetPx = windowHeightPx * (1f - PARTIAL_HEIGHT_FRACTION)

    // Guard the window-height-unknown case explicitly: dividing by zero yields NaN, which
    // coerceIn passes through unchanged (NaN comparisons are always false) instead of clamping
    // it, so a plain division would flash a fully-opaque scrim for one frame before layout.
    val revealProgress = if (windowHeightPx <= 0f) {
        0f
    } else {
        ((windowHeightPx - sheetOffsetPx) / (windowHeightPx - partiallyExpandedOffsetPx)).coerceIn(
            minimumValue = 0f,
            maximumValue = 1f,
        )
    }

    val scrimAlphaFadeDistancePx = partiallyExpandedOffsetPx
    val scrimAlpha = easing.transform((sheetHeight / scrimAlphaFadeDistancePx).coerceIn(0f, 1f))

    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    }

    val dragHandleFadeDistancePx = with(density) { DRAG_HANDLE_FADE_DISTANCE.toPx() }
    val dragHandleRevealProgress = easing.transform(
        ((sheetOffsetPx) / dragHandleFadeDistancePx).coerceIn(
            minimumValue = 0f,
            maximumValue = 1f,
        ),
    )

    BlurScrim(
        hazeState = hazeState,
        containerColor = containerColor,
        blurProgress = revealProgress,
        scrimAlpha = scrimAlpha,
        sheetOffsetPx = sheetOffsetPx,
    )

    val dragHandleTopOffset = (statusBarHeight - DRAG_HANDLE_GAP_ABOVE_CONTENT).coerceAtLeast(0.dp)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
            .fillMaxHeight()
            .animateContentSize(),
        containerColor = containerColor,
        scrimColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing.exclude(WindowInsets.statusBars) },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        NavigationBackHandler(
            state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
            isBackEnabled = sheetState.isVisible,
            onBackCompleted = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
            },
        )
        Box(
            contentAlignment = Alignment.TopCenter,
        ) {
            DragHandle(
                revealProgress = dragHandleRevealProgress,
                topOffset = dragHandleTopOffset,
            )
            Box(modifier = Modifier.padding(top = statusBarHeight)) {
                request.content()
            }
        }
    }
}

@Composable
private fun DragHandle(revealProgress: Float, topOffset: Dp, modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DRAG_HANDLE_BACKGROUND_ALPHA)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = revealProgress
                alpha = revealProgress
            }.padding(top = topOffset)
            .background(color = backgroundColor, shape = CircleShape)
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
    blurProgress: Float,
    scrimAlpha: Float,
    sheetOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    val gradientColorStops = rememberScrimGradientColorStops(containerColor = containerColor)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                performanceMode = HazePerformanceMode.Balanced,
                style = HazeBlurStyle { blurRadius(VISIBLE_SCRIM_BLUR_RADIUS * blurProgress) },
            ),
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = gradientColorStops,
                startY = 0f,
                endY = sheetOffsetPx.coerceAtLeast(0f),
            ),
            alpha = scrimAlpha,
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
