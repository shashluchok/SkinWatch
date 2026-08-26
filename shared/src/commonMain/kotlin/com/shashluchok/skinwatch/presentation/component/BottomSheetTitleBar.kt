package com.shashluchok.skinwatch.presentation.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomSheetTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        windowInsets = windowInsets,
    )
}
