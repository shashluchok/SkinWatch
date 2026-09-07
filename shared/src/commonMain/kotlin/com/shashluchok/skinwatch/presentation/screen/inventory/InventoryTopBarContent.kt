package com.shashluchok.skinwatch.presentation.screen.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.domain.inventory.InventoryStats
import com.shashluchok.skinwatch.presentation.screen.inventory.component.InventoryStatsBar
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import org.koin.compose.viewmodel.koinViewModel

/**
 * The inventory tab's share of the app top bar: its totals, sitting under the tab title.
 *
 * Reads the same [InventoryViewModel] the screen does. Nothing in this app provides a
 * `ViewModelStoreOwner` of its own, so the bar and the screen resolve to one instance and one state.
 */
@Composable
internal fun InventoryTopBarContent(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value

    InventoryTopBarContent(
        modifier = modifier,
        stats = (state.content as? InventoryViewModel.State.Content.Items)?.stats,
    )
}

@Composable
private fun InventoryTopBarContent(
    stats: InventoryStats?,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val fadeSpec = remember(motion) {
        tween<Float>(durationMillis = motion.duration.standard, easing = motion.easing.standard)
    }

    // Sized to the figures, never wider: the bar pushes this block against its right edge, and a
    // block that filled the remaining width would leave the figures stranded at the left of it with
    // the slack showing as a gap on the right.
    AnimatedVisibility(
        modifier = modifier,
        visible = stats != null,
        enter = fadeIn(fadeSpec),
        exit = fadeOut(fadeSpec),
    ) {
        // Kept so the totals stay put while a sync briefly puts the list back into a loading state,
        // rather than collapsing the bar and dropping everything below it by its height.
        val lastStats = remember { mutableStateOf(stats) }
        stats?.let { lastStats.value = it }

        lastStats.value?.let { InventoryStatsBar(stats = it) }
    }
}
