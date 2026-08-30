package com.shashluchok.skinwatch.presentation.screen.main.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.shashluchok.skinwatch.presentation.navigation.navtab.GlowIcon
import com.shashluchok.skinwatch.presentation.screen.main.MainScreen
import com.shashluchok.skinwatch.presentation.screen.main.enabledNavTabs
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import org.jetbrains.compose.resources.stringResource

private const val NAV_BAR_ITEM_INDICATOR_ALPHA = 0.16f
private const val SCRIM_GRADIENT_STOP_COUNT = 14
private val BLUR_RADIUS = 16.dp
private val BLUR_SCRIM_EXTRA_HEIGHT = 12.dp

@Composable
internal fun MainNavigationBar(
    backStack: NavBackStack<NavKey>,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        NavBarBlurScrim(hazeState = hazeState, containerColor = NavigationBarDefaults.containerColor)

        NavigationBar(
            modifier = Modifier.testTag(MainScreen.Tag.NAV_BAR),
            containerColor = Color.Transparent,
        ) {
            enabledNavTabs.forEach { tab ->
                val isSelected = backStack.lastOrNull() == tab.destination

                NavigationBarItem(
                    modifier = Modifier.testTag(MainScreen.Tag.navBarItem(tab)),
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            backStack.clear()
                            backStack.add(tab.destination)
                        }
                    },
                    icon = { tab.GlowIcon(isSelected = isSelected) },
                    label = { Text(text = stringResource(tab.labelRes)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = NAV_BAR_ITEM_INDICATOR_ALPHA,
                        ),
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.NavBarBlurScrim(
    hazeState: HazeState,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val gradientColorStops = rememberScrimGradientColorStops(containerColor = containerColor)

    OverflowingTop(extraHeight = BLUR_SCRIM_EXTRA_HEIGHT, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    performanceMode = HazePerformanceMode.Balanced,
                    style = HazeBlurStyle {
                        blurRadius(BLUR_RADIUS)
                        progressive(HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f))
                    },
                ),
        ) {
            drawRect(brush = Brush.verticalGradient(colorStops = gradientColorStops))
        }
    }
}

@Composable
private fun BoxScope.OverflowingTop(
    extraHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.matchParentSize(),
    ) { measurables, constraints ->
        val extraHeightPx = extraHeight.roundToPx()
        val childConstraints = Constraints.fixed(
            width = constraints.maxWidth,
            height = constraints.maxHeight + extraHeightPx,
        )
        val placeable = measurables.single().measure(childConstraints)
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            placeable.placeRelative(x = 0, y = -extraHeightPx)
        }
    }
}

@Composable
private fun rememberScrimGradientColorStops(containerColor: Color): Array<Pair<Float, Color>> {
    val easing = LocalMotion.current.easing.standard
    return remember(containerColor, easing) {
        Array(SCRIM_GRADIENT_STOP_COUNT) { index ->
            val fraction = index / (SCRIM_GRADIENT_STOP_COUNT - 1f)
            val alpha = easing.transform(fraction)
            fraction to containerColor.copy(alpha = alpha)
        }
    }
}
