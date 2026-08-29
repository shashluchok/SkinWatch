package com.shashluchok.skinwatch.presentation.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.shashluchok.skinwatch.presentation.component.modal.host.LocalModalHost
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalHostContent
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalRequest
import com.shashluchok.skinwatch.presentation.navigation.config.navigationConfig
import com.shashluchok.skinwatch.presentation.navigation.destination.Inventory
import com.shashluchok.skinwatch.presentation.navigation.navtab.GlowIcon
import com.shashluchok.skinwatch.presentation.navigation.navtab.NavTab
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryScreen
import com.shashluchok.skinwatch.presentation.screen.main.component.AddFab
import com.shashluchok.skinwatch.presentation.screen.main.component.AddItemBottomSheetContent
import com.shashluchok.skinwatch.presentation.screen.settings.SettingsScreen
import com.shashluchok.skinwatch.presentation.screen.watchlist.WatchlistScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val NAV_BAR_ITEM_INDICATOR_ALPHA = 0.16f
private val enabledNavTabs = NavTab.entries.filter { it.isEnabled }

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
    val pagerState = rememberPagerState(pageCount = { enabledNavTabs.size })

    SyncPagerWithBackStack(
        backStack = backStack,
        pagerState = pagerState,
    )
    NavigateToInventoryOnBack(backStack = backStack)

    ProvideMainScreenAmbients {
        val hazeState = rememberHazeState()
        val currentTab = enabledNavTabs.first { it.destination == backStack.lastOrNull() }

        Scaffold(
            modifier = modifier.testTag(MainScreen.Tag.ROOT).hazeSource(hazeState),
            topBar = {
                TopAppBar(
                    modifier = Modifier.testTag(MainScreen.Tag.TOP_BAR),
                    title = { Text(text = stringResource(currentTab.labelRes)) },
                )
            },
            bottomBar = { MainNavigationBar(backStack = backStack) },
            floatingActionButton = {
                MainFab(
                    visible = currentTab == NavTab.INVENTORY,
                    onClick = { onAction(MainViewModel.Action.OnAddClick) },
                )
            },
        ) { contentPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) { page ->
                enabledNavTabs[page].ScreenContent(modifier = Modifier.fillMaxSize())
            }
        }

        state.addSheet?.let { sheet ->
            RegisterAddSheet(sheet = sheet, onAction = onAction)
        }

        ModalHostContent(hazeState = hazeState)
    }
}

@Composable
private fun RegisterAddSheet(
    sheet: MainViewModel.AddSheetState,
    onAction: (MainViewModel.Action) -> Unit,
) {
    LocalModalHost.current.Show(
        ModalRequest(
            appearance = ModalRequest.Appearance.BottomSheet,
            onDismissRequest = { onAction(MainViewModel.Action.OnDismissSheet) },
            content = {
                AddItemBottomSheetContent(
                    sheet = sheet,
                    onQueryChange = { onAction(MainViewModel.Action.OnSearchQueryChanged(it)) },
                    onResultSelect = { onAction(MainViewModel.Action.OnSearchResultSelected(it)) },
                    onBackClick = { onAction(MainViewModel.Action.OnAddDetailsBackClick) },
                    onQuantityChange = { onAction(MainViewModel.Action.OnQuantityChanged(it)) },
                    onPurchasePriceChange = { onAction(MainViewModel.Action.OnPurchasePriceChanged(it)) },
                    onSaveClick = { onAction(MainViewModel.Action.OnSaveClick) },
                )
            },
        ),
    )
}

@Composable
private fun MainNavigationBar(backStack: NavBackStack<NavKey>) {
    NavigationBar(
        modifier = Modifier.testTag(MainScreen.Tag.NAV_BAR),
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

@Composable
private fun MainFab(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        AddFab(onClick = onClick)
    }
}

@Composable
private fun SyncPagerWithBackStack(
    backStack: NavBackStack<NavKey>,
    pagerState: PagerState,
) {
    val currentTab = enabledNavTabs.first { it.destination == backStack.lastOrNull() }
    val currentTabIndex = enabledNavTabs.indexOf(currentTab)

    LaunchedEffect(currentTabIndex) {
        if (pagerState.targetPage != currentTabIndex) {
            pagerState.animateScrollToPage(
                page = currentTabIndex,
            )
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val settledDestination = enabledNavTabs[pagerState.settledPage].destination
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
        const val TOP_BAR = "$ROOT.topBar"
        const val NAV_BAR = "$ROOT.navBar"

        fun navBarItem(tab: NavTab) = when (tab) {
            NavTab.INVENTORY -> "$NAV_BAR.inventory"
            NavTab.WATCHLIST -> "$NAV_BAR.watchlist"
            NavTab.SETTINGS -> "$NAV_BAR.settings"
        }
    }
}
