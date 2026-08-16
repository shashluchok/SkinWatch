package com.shashluchok.skinwatch.presentation.screen.main.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__fab__add__content_description
import org.jetbrains.compose.resources.stringResource

/**
 * A generic "add" FAB, hosted once by `MainScreen`'s shared `Scaffold` rather than by any single
 * page -- not owned by or aware of any particular screen's state/actions, so any tab (Inventory
 * today, Watchlist later) can be wired into it without this composable knowing which one.
 */
@Composable
internal fun AddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        modifier = modifier.testTag(AddFab.Tag.ROOT),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(Res.string.dev__fab__add__content_description),
        )
    }
}

internal object AddFab {
    object Tag {
        const val ROOT = "AddFab"
    }
}
