package com.shashluchok.skinwatch.presentation.screen.main.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil3.compose.AsyncImage
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchResultRow(
    result: CatalogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = false, onClick = onClick)
            .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.small),
        horizontalArrangement = Arrangement.spacedBy(dimens.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = result.iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(dimens.iconSize.extraLarge)
                .clip(RoundedCornerShape(dimens.radius.small))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag(SearchResultRow.Tag.ICON),
            contentScale = ContentScale.Crop,
        )
        Column {
            Text(text = result.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(categoryLabel(result.category)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal object SearchResultRow {
    object Tag {
        private const val ROOT = "SearchResultRow"
        const val ICON = "$ROOT.icon"
    }
}
