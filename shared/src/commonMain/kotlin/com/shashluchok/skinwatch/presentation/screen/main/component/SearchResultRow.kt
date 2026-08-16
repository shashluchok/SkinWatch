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
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamMarketItem
import com.shashluchok.skinwatch.presentation.theme.LocalDimens

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0

@Composable
internal fun SearchResultRow(
    result: SteamMarketItem,
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
                text = result.sellPrice?.let(::formatMoney).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Plain, locale-independent formatting for now (e.g. "49.00 USD") -- exact currency symbol/
 * position/locale formatting is a follow-up visual-design pass, not decided by this screen.
 */
private fun formatMoney(money: Money): String {
    val major = money.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT
    return "$major ${money.currency.name}"
}

internal object SearchResultRow {
    object Tag {
        private const val ROOT = "SearchResultRow"
        const val ICON = "$ROOT.icon"
    }
}
