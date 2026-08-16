package com.shashluchok.skinwatch.presentation.navigation.navtab

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.shashluchok.skinwatch.presentation.navigation.destination.Inventory
import com.shashluchok.skinwatch.presentation.navigation.destination.Settings
import com.shashluchok.skinwatch.presentation.navigation.destination.Watchlist
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.navbar__inventory_tab
import com.shashluchok.skinwatch.resources.navbar__settings_tab
import com.shashluchok.skinwatch.resources.navbar__watchlist_tab
import org.jetbrains.compose.resources.StringResource

private const val NAV_GLOW_DURATION_MS = 2_000
private val navGlowSize = 34.dp
private const val NAV_GLOW_MIN_ALPHA = 0.35f
private const val NAV_GLOW_MAX_ALPHA = 0.85f
private const val NAV_GLOW_MIN_SCALE = 0.80f
private const val NAV_GLOW_MAX_SCALE = 0.92f
private const val NAV_GLOW_TRANSITION_LABEL = "navGlow"
private const val NAV_GLOW_ALPHA_LABEL = "navGlowAlpha"
private const val NAV_GLOW_SCALE_LABEL = "navGlowScale"

internal enum class NavTab(
    val destination: NavKey,
    val labelRes: StringResource,
    val icon: ImageVector,
    val isEnabled: Boolean = true,
) {
    INVENTORY(Inventory, Res.string.navbar__inventory_tab, Icons.AutoMirrored.Filled.List),

    // Disabled: a plain wishlist duplicates Inventory with no own-specific value. Worth enabling
    // once it can drive real price-target alerts -- until then it's not shown anywhere.
    WATCHLIST(Watchlist, Res.string.navbar__watchlist_tab, Icons.Default.Star, isEnabled = false),
    SETTINGS(Settings, Res.string.navbar__settings_tab, Icons.Default.Settings),
}

/** Tab icon with an idle "breathing glow" behind it while the tab is selected. */
@Composable
internal fun NavTab.GlowIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val glowModifier = if (isSelected) {
        val motion = LocalMotion.current
        val glowTransition = rememberInfiniteTransition(label = NAV_GLOW_TRANSITION_LABEL)
        val glowAnimationSpec = infiniteRepeatable(
            animation = tween<Float>(
                durationMillis = NAV_GLOW_DURATION_MS,
                easing = motion.easing.standard,
            ),
            repeatMode = RepeatMode.Reverse,
        )
        val glowAlpha by glowTransition.animateFloat(
            initialValue = NAV_GLOW_MIN_ALPHA,
            targetValue = NAV_GLOW_MAX_ALPHA,
            animationSpec = glowAnimationSpec,
            label = NAV_GLOW_ALPHA_LABEL,
        )
        val glowScale by glowTransition.animateFloat(
            initialValue = NAV_GLOW_MIN_SCALE,
            targetValue = NAV_GLOW_MAX_SCALE,
            animationSpec = glowAnimationSpec,
            label = NAV_GLOW_SCALE_LABEL,
        )
        val glowColor = MaterialTheme.colorScheme.primary

        Modifier.drawBehind {
            val radius = navGlowSize.toPx() / 2f * glowScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    } else {
        Modifier
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.then(glowModifier),
    )
}
