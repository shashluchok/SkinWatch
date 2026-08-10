package com.shashluchok.skinwatch.presentation.screen.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.shashluchok.skinwatch.presentation.navigation.config.navigationConfig
import com.shashluchok.skinwatch.presentation.navigation.destination.Inventory
import com.shashluchok.skinwatch.presentation.navigation.destination.Settings
import com.shashluchok.skinwatch.presentation.navigation.destination.Watchlist
import com.shashluchok.skinwatch.presentation.navigation.navtab.GlowIcon
import com.shashluchok.skinwatch.presentation.navigation.navtab.NavTab
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryScreen
import com.shashluchok.skinwatch.presentation.screen.settings.SettingsScreen
import com.shashluchok.skinwatch.presentation.screen.watchlist.WatchlistScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val NAV_BAR_ITEM_INDICATOR_ALPHA = 0.16f

@Composable
internal fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    MainScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Suppress("UnusedParameter")
@Composable
private fun MainScreen(
    state: MainViewModel.State,
    onAction: (MainViewModel.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(
        configuration = navigationConfig,
        elements = arrayOf(Inventory),
    )

    Scaffold(
        modifier = modifier.testTag(MainScreen.Tag.ROOT),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag(MainScreen.Tag.NAV_BAR),
            ) {
                NavTab.entries.forEach { tab ->
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
        },
    ) { contentPadding ->
        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            backStack = backStack,
            onBack = backStack::removeLastOrNull,
            entryProvider = entryProvider {
                entry<Inventory> { InventoryScreen(modifier = Modifier.fillMaxSize()) }
                entry<Watchlist> { WatchlistScreen(modifier = Modifier.fillMaxSize()) }
                entry<Settings> { SettingsScreen(modifier = Modifier.fillMaxSize()) }
            },
        )
    }
}

internal object MainScreen {
    object Tag {
        const val ROOT = "MainScreen"
        const val NAV_BAR = "$ROOT.navBar"

        fun navBarItem(tab: NavTab) = when (tab) {
            NavTab.INVENTORY -> "$NAV_BAR.inventory"
            NavTab.WATCHLIST -> "$NAV_BAR.watchlist"
            NavTab.SETTINGS -> "$NAV_BAR.settings"
        }
    }
}
