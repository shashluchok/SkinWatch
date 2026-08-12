package com.shashluchok.skinwatch.presentation.screen.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.shashluchok.skinwatch.presentation.navigation.config.navigationConfig
import com.shashluchok.skinwatch.presentation.navigation.destination.Inventory
import com.shashluchok.skinwatch.presentation.navigation.navtab.GlowIcon
import com.shashluchok.skinwatch.presentation.navigation.navtab.NavTab
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryScreen
import com.shashluchok.skinwatch.presentation.screen.settings.SettingsScreen
import com.shashluchok.skinwatch.presentation.screen.watchlist.WatchlistScreen
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val NAV_BAR_ITEM_INDICATOR_ALPHA = 0.16f
private const val PAGER_UNSETTLED_SCALE = 0.9f

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
    val pagerState = rememberPagerState(pageCount = { NavTab.entries.size })

    SyncPagerWithBackStack(
        backStack = backStack,
        pagerState = pagerState,
    )
    NavigateToInventoryOnBack(backStack = backStack)

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
        val motion = LocalMotion.current

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) { page ->
            val isSettledActive = !pagerState.isScrollInProgress
            val scale by animateFloatAsState(
                targetValue = if (isSettledActive) 1f else PAGER_UNSETTLED_SCALE,
                animationSpec = tween(
                    durationMillis = if (isSettledActive) motion.duration.standard else 0,
                ),
            )

            NavTab.entries[page].ScreenContent(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}

@Composable
private fun SyncPagerWithBackStack(
    backStack: NavBackStack<NavKey>,
    pagerState: PagerState,
) {
    val currentTab = NavTab.entries.first { it.destination == backStack.lastOrNull() }
    val currentTabIndex = NavTab.entries.indexOf(currentTab)

    LaunchedEffect(currentTabIndex) {
        if (pagerState.targetPage != currentTabIndex) {
            pagerState.animateScrollToPage(
                page = currentTabIndex,
            )
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val settledDestination = NavTab.entries[pagerState.settledPage].destination
        if (backStack.lastOrNull() != settledDestination) {
            backStack.clear()
            backStack.add(settledDestination)
        }
    }
}

@Composable
private fun NavigateToInventoryOnBack(backStack: NavBackStack<NavKey>) {
    val isOnInventory = backStack.lastOrNull() == Inventory

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = !isOnInventory,
        onBackCompleted = {
            backStack.clear()
            backStack.add(Inventory)
        },
    )
}

@Composable
private fun NavTab.ScreenContent(modifier: Modifier = Modifier) {
    when (this) {
        NavTab.INVENTORY -> InventoryScreen(modifier = modifier)
        NavTab.WATCHLIST -> WatchlistScreen(modifier = modifier)
        NavTab.SETTINGS -> SettingsScreen(modifier = modifier)
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
