package com.shashluchok.skinwatch.presentation.screen.main.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.shashluchok.skinwatch.presentation.component.BarBlurScrim
import com.shashluchok.skinwatch.presentation.component.BarEdge
import com.shashluchok.skinwatch.presentation.navigation.navtab.GlowIcon
import com.shashluchok.skinwatch.presentation.screen.main.MainScreen
import com.shashluchok.skinwatch.presentation.screen.main.enabledNavTabs
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource

private const val NAV_BAR_ITEM_INDICATOR_ALPHA = 0.16f

@Composable
internal fun MainNavigationBar(
    backStack: NavBackStack<NavKey>,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BarBlurScrim(
            hazeState = hazeState,
            containerColor = MaterialTheme.colorScheme.background,
            edge = BarEdge.Bottom,
        )

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
