package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.theme.AppFontFamilies
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.tabularNumeric
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_sheet__price_axis_label
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
private const val CHART_HEIGHT_DP = 240
private const val CHART_AREA_FILL_TOP_ALPHA_DARK = 0.28f
private const val CHART_AREA_FILL_TOP_ALPHA_LIGHT = 0.16f
private const val CHART_POINT_DIAMETER_DP = 3
private const val CHART_GUIDELINE_ALPHA = 0.5f

/**
 * X values are each snapshot's `capturedAt` as epoch milliseconds (real temporal spacing, not an
 * even index), Y values are `lowestPrice` in major units. [LineCartesianLayer.Interpolator.Sharp]
 * draws straight segments between points -- no smoothing, so the line only ever shows real, honest
 * readings (see design addendum section 2). The purchase-price horizontal reference line uses
 * Vico's [HorizontalLine] decoration plus an always-present [PurchasePriceLegend] row below the
 * chart (the legend is the required, reliable treatment; the line's own optional inline label is
 * not used here to keep the two purchase-price call sites -- 1-point and 2+-point states --
 * visually identical).
 */
@Composable
internal fun PriceHistoryChart(
    snapshots: List<PriceSnapshot>,
    purchasePrice: Money?,
) {
    val dimens = LocalDimens.current
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(snapshots) {
        modelProducer.runTransaction {
            lineModel {
                series(
                    x = snapshots.map { it.capturedAt.toEpochMilliseconds() },
                    y = snapshots.map { (it.lowestPrice?.minorUnits ?: 0L) / MINOR_UNITS_PER_MAJOR_UNIT },
                )
            }
        }
    }

    Column {
        Text(
            text = stringResource(
                Res.string.dev__screen_inventory__price_history_sheet__price_axis_label,
                snapshots.first().currency.name,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariant,
        )
        PriceHistoryChartHost(modelProducer = modelProducer, purchasePrice = purchasePrice)
        if (purchasePrice != null) {
            PurchasePriceLegend(
                modifier = Modifier.padding(top = dimens.padding.small),
                purchasePrice = purchasePrice,
            )
        }
    }
}

/**
 * The Vico chart itself, wrapped in an M3-aware theme so unstyled parts (e.g. tick marks) inherit
 * this project's Material3 color scheme instead of Vico's own light/dark defaults.
 */
@Composable
private fun PriceHistoryChartHost(
    modelProducer: CartesianChartModelProducer,
    purchasePrice: Money?,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val axisLabelStyle = MaterialTheme.typography.labelSmall
        .copy(fontFamily = AppFontFamilies.jetBrainsMono, color = onSurfaceVariant)
        .tabularNumeric

    ProvideVicoTheme(theme = rememberM3VicoTheme()) {
        CartesianChartHost(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT_DP.dp)
                .testTag(PriceHistoryBottomSheetContent.Tag.CHART),
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = priceHistoryLineProvider()),
                startAxis = priceHistoryStartAxis(labelStyle = axisLabelStyle),
                bottomAxis = priceHistoryBottomAxis(labelStyle = axisLabelStyle),
                decorations = purchasePriceDecorations(purchasePrice = purchasePrice),
            ),
            modelProducer = modelProducer,
        )
    }
}

/**
 * A single [LineCartesianLayer.Line] for the market-price series: straight segments
 * ([LineCartesianLayer.Interpolator.Sharp], no smoothing), a soft vertical gradient area fill under
 * the line, and a small filled circle at every vertex to keep sparse readings visually honest (see
 * design addendum section 2).
 */
@Composable
private fun priceHistoryLineProvider(): LineCartesianLayer.LineProvider {
    val primary = MaterialTheme.colorScheme.primary
    val areaFillTopAlpha = if (isSystemInDarkTheme()) {
        CHART_AREA_FILL_TOP_ALPHA_DARK
    } else {
        CHART_AREA_FILL_TOP_ALPHA_LIGHT
    }
    return LineCartesianLayer.LineProvider.series(
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill = Fill(primary)),
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = LocalDimens.current.border.thick),
            areaFill = LineCartesianLayer.AreaFill.single(
                fill = Fill(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = areaFillTopAlpha), primary.copy(alpha = 0f)),
                    ),
                ),
            ),
            pointProvider = LineCartesianLayer.PointProvider.single(
                LineCartesianLayer.Point(
                    component = rememberShapeComponent(fill = Fill(primary), shape = CircleShape),
                    size = CHART_POINT_DIAMETER_DP.dp,
                ),
            ),
            interpolator = LineCartesianLayer.Interpolator.Sharp,
        ),
    )
}

/** Y axis: plain numbers (currency already labeled once above the chart), horizontal guideline only. */
@Composable
private fun priceHistoryStartAxis(labelStyle: TextStyle): VerticalAxis<Axis.Position.Vertical.Start> {
    val dimens = LocalDimens.current
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    return VerticalAxis.rememberStart(
        label = rememberTextComponent(style = labelStyle),
        guideline = rememberLineComponent(
            fill = Fill(outlineVariant.copy(alpha = CHART_GUIDELINE_ALPHA)),
            thickness = dimens.border.thin,
        ),
        valueFormatter = CartesianValueFormatter { _, value, _ -> value.toString() },
    )
}

/**
 * X axis: "dd.MM" dates in fixed UTC. No vertical guideline -- readings aren't evenly spaced in
 * time, so a guideline per point would look noisy rather than informative (design addendum
 * section 2).
 */
@Composable
private fun priceHistoryBottomAxis(labelStyle: TextStyle): HorizontalAxis<Axis.Position.Horizontal.Bottom> =
    HorizontalAxis.rememberBottom(
        label = rememberTextComponent(style = labelStyle),
        guideline = null,
        valueFormatter = CartesianValueFormatter { _, value, _ ->
            Instant.fromEpochMilliseconds(value.toLong()).toAxisDateLabel()
        },
    )

/** Dashed [HorizontalLine] at [purchasePrice], omitted entirely when unset (see design addendum section 4). */
@Composable
private fun purchasePriceDecorations(purchasePrice: Money?): List<Decoration> {
    if (purchasePrice == null) return emptyList()
    val dimens = LocalDimens.current
    val outline = MaterialTheme.colorScheme.outline
    return listOf(
        HorizontalLine(
            y = { purchasePrice.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT },
            line = rememberLineComponent(
                fill = Fill(outline),
                thickness = dimens.border.thin,
                shape = DashedShape(dashLength = dimens.padding.small, gapLength = dimens.padding.extraSmall),
            ),
        ),
    )
}

/** "dd.MM", fixed UTC -- see design addendum section 1 for why UTC (not local timezone). */
private fun Instant.toAxisDateLabel(): String {
    val date = toLocalDateTime(TimeZone.UTC).date
    return "${date.day.pad2()}.${date.month.number.pad2()}"
}
