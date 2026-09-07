package com.shashluchok.skinwatch.presentation.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.shashluchok.skinwatch.presentation.component.AnimatedFadeText
import com.shashluchok.skinwatch.presentation.component.BarBlurScrim
import com.shashluchok.skinwatch.presentation.component.BarEdge
import com.shashluchok.skinwatch.presentation.component.LocalBottomBarInset
import com.shashluchok.skinwatch.presentation.component.LocalTopBarInset
import com.shashluchok.skinwatch.presentation.component.modal.host.LocalModalHost
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalHostContent
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalRequest
import com.shashluchok.skinwatch.presentation.navigation.config.navigationConfig
import com.shashluchok.skinwatch.presentation.navigation.destination.Inventory
import com.shashluchok.skinwatch.presentation.navigation.navtab.NavTab
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryScreen
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryTopBarContent
import com.shashluchok.skinwatch.presentation.screen.main.component.AddFab
import com.shashluchok.skinwatch.presentation.screen.main.component.AddItemBottomSheetContent
import com.shashluchok.skinwatch.presentation.screen.main.component.MainNavigationBar
import com.shashluchok.skinwatch.presentation.screen.settings.SettingsScreen
import com.shashluchok.skinwatch.presentation.screen.watchlist.WatchlistScreen
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal val enabledNavTabs = NavTab.entries.filter { it.isEnabled }

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
        val contentHazeState = rememberHazeState()
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        var topBarHeight by remember { mutableStateOf(0.dp) }
        var navBarHeight by remember { mutableStateOf(0.dp) }
        val currentTab = enabledNavTabs.first { it.destination == backStack.lastOrNull() }

        Scaffold(
            modifier = modifier.testTag(MainScreen.Tag.ROOT).hazeSource(hazeState),
            topBar = {
                MainTopBar(
                    currentTab = currentTab,
                    hazeState = contentHazeState,
                    modifier = Modifier.onGloballyPositioned {
                        topBarHeight = with(density) { it.size.height.toDp() }
                    },
                )
            },
            bottomBar = {
                MainNavigationBar(
                    backStack = backStack,
                    hazeState = contentHazeState,
                    modifier = Modifier.onGloballyPositioned {
                        navBarHeight = with(density) { it.size.height.toDp() }
                    },
                )
            },
            floatingActionButton = {
                MainFab(
                    visible = currentTab == NavTab.INVENTORY,
                    onClick = { onAction(MainViewModel.Action.OnAddClick) },
                )
            },
        ) { contentPadding ->

            // Neither bar's height is passed down: both are translucent and content is meant to be
            // seen sliding under them. Screens add the heights back to their own scrollable padding
            // through the insets below, so nothing ends up parked where it cannot be read.
            val pagerPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            )

            CompositionLocalProvider(
                LocalTopBarInset provides topBarHeight,
                LocalBottomBarInset provides navBarHeight,
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(contentHazeState)
                        .padding(pagerPadding),
                ) { page ->
                    enabledNavTabs[page].ScreenContent(modifier = Modifier.fillMaxSize())
                }
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
        pagerState.animateScrollToPage(page = currentTabIndex)
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

/**
 * The app bar, mirroring the navigation bar: content passes underneath, blurred and faded into the
 * bar's colour rather than meeting a hard opaque edge.
 *
 * Laid out as a plain column rather than as a `TopAppBar`, whose single-row variant is a fixed 64dp
 * and would clip a tab that puts more than a title in here. Nothing else of that component was being
 * used anyway -- the container is transparent and the backdrop is drawn below.
 *
 * The height animates on the content rather than around the whole bar: `animateContentSize` clips to
 * the size it is animating, and wrapping the box would cut the scrim's overhang -- the very strip
 * over which the blur is meant to finish fading out.
 */
@Composable
private fun MainTopBar(
    currentTab: NavTab,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Box(modifier = modifier) {
        BarBlurScrim(
            hazeState = hazeState,
            containerColor = MaterialTheme.colorScheme.background,
            edge = BarEdge.Top,
        )

        Row(
            modifier = Modifier
                .testTag(MainScreen.Tag.TOP_BAR)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.small)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(dimens.padding.medium),
            // Top, not centre: a tab whose content runs to a second line would otherwise push the
            // title down to the middle of it, off the line its own figure sits on.
            verticalAlignment = Alignment.Top,
        ) {
            AnimatedFadeText(
                text = stringResource(currentTab.labelRes),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.weight(1f))
            currentTab.TopBarContent()
        }
    }
}

/** What each tab adds below the title -- its own headline, rather than a second copy of its name. */
@Composable
private fun NavTab.TopBarContent(modifier: Modifier = Modifier) {
    when (this) {
        NavTab.INVENTORY -> InventoryTopBarContent(modifier = modifier)
        NavTab.WATCHLIST, NavTab.SETTINGS -> Unit
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
