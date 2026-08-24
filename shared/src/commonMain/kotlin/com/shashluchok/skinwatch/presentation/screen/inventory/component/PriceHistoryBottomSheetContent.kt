package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryViewModel
import com.shashluchok.skinwatch.presentation.theme.AppFontFamilies
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.tabularNumeric
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_sheet__empty_state
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_sheet__purchase_price_label
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_sheet__single_point_hint
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_sheet__title
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
private const val EMPTY_STATE_GLYPH_ALPHA = 0.5f
private const val DASH_SWATCH_WIDTH_DP = 20

/**
 * Same normalized points as `InventoryItemCard`'s trailing-edge hint glyph -- kept as a private
 * copy here rather than a shared component, since `InventoryItemCard.kt` already ships its own
 * (already-verified, separately staged) copy and isn't touched by this task.
 */
private val emptyStateGlyphPoints = listOf(
    Offset(x = 0f, y = 0.70f),
    Offset(x = 0.33f, y = 0.30f),
    Offset(x = 0.66f, y = 0.55f),
    Offset(x = 1f, y = 0.15f),
)

/** Content of the price-history bottom sheet */
@Composable
internal fun PriceHistoryBottomSheetContent(
    sheet: InventoryViewModel.PriceHistorySheetState,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    // A snapshot with a null lowestPrice contributes nothing to the chart/value -- it must not
    // count as a "data point" for the 0/1/2+ branch below (see design addendum section 7).
    val pricedSnapshots = sheet.snapshots.filter { it.lowestPrice != null }

    Column(
        modifier = modifier
            .testTag(PriceHistoryBottomSheetContent.Tag.ROOT)
            .padding(
                horizontal = dimens.padding.medium,
                vertical = dimens.padding.medium,
            ),
    ) {
        Text(
            text = stringResource(Res.string.dev__screen_inventory__price_history_sheet__title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            modifier = Modifier.padding(top = dimens.padding.tiny),
            text = sheet.item.marketHashName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = dimens.padding.medium),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        when {
            pricedSnapshots.isEmpty() -> EmptyPriceHistory()
            pricedSnapshots.size == 1 -> SinglePricePoint(
                snapshot = pricedSnapshots.single(),
                purchasePrice = sheet.item.purchasePrice,
            )
            else -> PriceHistoryChart(
                snapshots = pricedSnapshots,
                purchasePrice = sheet.item.purchasePrice,
            )
        }
    }
}

@Composable
private fun EmptyPriceHistory() {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.padding.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyStateGlyph(
            modifier = Modifier
                .size(dimens.iconSize.extraLarge)
                .testTag(PriceHistoryBottomSheetContent.Tag.EMPTY_STATE),
        )
        Text(
            modifier = Modifier.padding(top = dimens.padding.small),
            text = stringResource(Res.string.dev__screen_inventory__price_history_sheet__empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Same static "sparkline" polyline as `InventoryItemCard`'s hint glyph, muted for an empty state. */
@Composable
private fun EmptyStateGlyph(modifier: Modifier = Modifier) {
    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = EMPTY_STATE_GLYPH_ALPHA)
    val strokeWidth = LocalDimens.current.border.thin
    Canvas(modifier = modifier) {
        val path = Path().apply {
            emptyStateGlyphPoints.forEachIndexed { index, point ->
                val offset = Offset(x = point.x * size.width, y = point.y * size.height)
                if (index == 0) moveTo(x = offset.x, y = offset.y) else lineTo(x = offset.x, y = offset.y)
            }
        }
        drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth.toPx()))
    }
}

@Composable
private fun SinglePricePoint(
    snapshot: PriceSnapshot,
    purchasePrice: Money?,
) {
    val dimens = LocalDimens.current
    Column(modifier = Modifier.padding(top = dimens.padding.extraLarge)) {
        Text(
            modifier = Modifier.testTag(PriceHistoryBottomSheetContent.Tag.SINGLE_POINT_VALUE),
            text = snapshot.lowestPrice?.let(::formatMoney).orEmpty(),
            style = MaterialTheme.typography.headlineSmall
                .copy(fontFamily = AppFontFamilies.jetBrainsMono)
                .tabularNumeric,
        )
        Text(
            modifier = Modifier.padding(top = dimens.padding.tiny),
            text = snapshot.capturedAt.toFullDateLabel(),
            style = MaterialTheme.typography.bodySmall
                .copy(fontFamily = AppFontFamilies.jetBrainsMono)
                .tabularNumeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.padding(top = dimens.padding.small),
            text = stringResource(Res.string.dev__screen_inventory__price_history_sheet__single_point_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (purchasePrice != null) {
            PurchasePriceLegend(
                modifier = Modifier.padding(top = dimens.padding.medium),
                purchasePrice = purchasePrice,
            )
        }
    }
}

/** Dash swatch + label + amount, e.g. "┄┄ Цена покупки  49.00 USD" -- see design addendum section 4. */
@Composable
internal fun PurchasePriceLegend(
    purchasePrice: Money,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    Row(modifier = modifier) {
        DashSwatch(
            modifier = Modifier.size(width = DASH_SWATCH_WIDTH_DP.dp, height = dimens.border.thin),
        )
        Text(
            modifier = Modifier.padding(start = dimens.padding.small),
            text = stringResource(Res.string.dev__screen_inventory__price_history_sheet__purchase_price_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.padding(start = dimens.padding.extraSmall),
            text = formatMoney(purchasePrice),
            style = MaterialTheme.typography.labelSmall
                .copy(fontFamily = AppFontFamilies.jetBrainsMono)
                .tabularNumeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Miniature preview of the chart's own dashed purchase-price line, same dash/gap pattern. */
@Composable
private fun DashSwatch(modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    val color = MaterialTheme.colorScheme.outline
    val strokeWidth = dimens.border.thin
    Canvas(modifier = modifier) {
        val y = size.height / 2f
        drawLine(
            color = color,
            start = Offset(x = 0f, y = y),
            end = Offset(x = size.width, y = y),
            strokeWidth = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dimens.padding.small.toPx(), dimens.padding.extraSmall.toPx()),
            ),
        )
    }
}

/** "dd.MM.yyyy", same fixed-UTC rationale as `PriceHistoryChart.kt`'s `toAxisDateLabel`. */
private fun Instant.toFullDateLabel(): String {
    val date = toLocalDateTime(TimeZone.UTC).date
    return "${date.day.pad2()}.${date.month.number.pad2()}.${date.year}"
}

internal fun Int.pad2(): String = toString().padStart(length = 2, padChar = '0')

/**
 * Plain, locale-independent formatting (e.g. "49.00 USD"), same pattern as `InventoryItemCard`'s
 * private `formatMoney`. Both copies should eventually move to one shared multiplatform money
 * formatter -- not in scope for this task.
 */
private fun formatMoney(money: Money): String {
    val major = money.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT
    return "$major ${money.currency.name}"
}

internal object PriceHistoryBottomSheetContent {
    object Tag {
        const val ROOT = "PriceHistoryBottomSheetContent"
        const val EMPTY_STATE = "$ROOT.emptyState"
        const val SINGLE_POINT_VALUE = "$ROOT.singlePointValue"
        const val CHART = "$ROOT.chart"
    }
}
