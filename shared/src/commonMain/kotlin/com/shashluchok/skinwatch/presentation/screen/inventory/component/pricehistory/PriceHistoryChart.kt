package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.presentation.theme.AppFontFamilies
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalSemanticColors
import com.shashluchok.skinwatch.presentation.theme.tabularNumeric
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_sheet__price_axis_label
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.time.Instant

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
private const val CHART_HEIGHT_DP = 240
private const val CHART_AREA_FILL_TOP_ALPHA_DARK = 0.28f
private const val CHART_AREA_FILL_TOP_ALPHA_LIGHT = 0.16f
private const val CHART_POINT_DIAMETER_DP = 4.5f
private const val CHART_GUIDELINE_ALPHA = 0.5f

/**
 * How many slices the real (minX, maxX) timestamp range is divided into to derive the chart's
 * x-step -- see the `getXStep` comment on [PriceHistoryChartHost] for why this can't be left to
 * Vico's default GCD-based inference. Large enough that per-point positioning stays smooth even for
 * the app's densest realistic history, small enough that the axis's tick-placement loop, which walks
 * the domain one step at a time, always finishes in a handful of iterations.
 */
private const val X_STEP_DIVISION_COUNT = 1_000.0

/**
 * X values are each snapshot's `capturedAt` as epoch milliseconds (real temporal spacing, not an
 * even index), Y values are `lowestPrice` in major units. [LineCartesianLayer.Interpolator.Sharp]
 * draws straight segments between points -- no smoothing, so the line only ever shows real, honest
 * readings (see design addendum section 2). The purchase-price horizontal reference line uses
 * Vico's [HorizontalLine] decoration plus an always-present [PurchasePriceLegend] row below the
 * chart (the legend is the required, reliable treatment; the line's own optional inline label is
 * not used here to keep the two purchase-price call sites -- 1-point and 2+-point states --
 * visually identical). When a purchase price is known, the line/area/points split into a profit
 * (above purchase price) and loss (below) color, and the Y axis gets a guaranteed, highlighted tick
 * at that exact value -- both driven by [purchasePriceValue], the single major-units number both
 * the layer and the axis key off of.
 */
@Composable
internal fun PriceHistoryChart(
    snapshots: List<PriceSnapshot>,
    purchasePrice: Money?,
) {
    val dimens = LocalDimens.current
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

    val currency = snapshots.first().currency
    val purchasePriceValue = remember(purchasePrice) {
        purchasePrice?.let { it.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT }
    }
    // With a purchase price, the axis is centered on it (see priceHistoryStartAxis); without one,
    // it falls back to the original 0-based range with headroom over the all-time high.
    val yAxisRange = remember(snapshots, purchasePrice, purchasePriceValue) {
        if (purchasePriceValue != null) {
            val readingRange = priceHistoryReadingRange(snapshots)
            val step = purchasePriceCenteredYAxisStep(
                minPrice = readingRange.start,
                maxPrice = readingRange.endInclusive,
                purchasePrice = purchasePriceValue,
            )
            purchasePriceCenteredYAxisRange(purchasePrice = purchasePriceValue, step = step)
        } else {
            0.0..priceHistoryYAxisMax(snapshots = snapshots, purchasePrice = purchasePrice)
        }
    }
    // The chart's real first/last reading timestamps -- the X axis's adaptive item placer divides
    // this exact range into evenly spaced date labels (see priceHistoryBottomAxisItemPlacer).
    val minX = remember(snapshots) { snapshots.minOf { it.capturedAt.toEpochMilliseconds() }.toDouble() }
    val maxX = remember(snapshots) { snapshots.maxOf { it.capturedAt.toEpochMilliseconds() }.toDouble() }
    val axisTitle = stringResource(
        Res.string.dev__screen_inventory__price_history_sheet__price_axis_label,
        currency.name,
    )

    Column {
        PriceHistoryChartHost(
            modelProducer = modelProducer,
            purchasePrice = purchasePrice,
            purchasePriceValue = purchasePriceValue,
            yAxisRange = yAxisRange,
            minX = minX,
            maxX = maxX,
            axisTitle = axisTitle,
            currency = currency,
        )
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
    purchasePriceValue: Double?,
    yAxisRange: ClosedFloatingPointRange<Double>,
    minX: Double,
    maxX: Double,
    axisTitle: String,
    currency: SteamCurrency,
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
                rememberLineCartesianLayer(
                    lineProvider = priceHistoryLineProvider(purchasePriceValue = purchasePriceValue),
                    rangeProvider = remember(yAxisRange) {
                        CartesianLayerRangeProvider.fixed(minY = yAxisRange.start, maxY = yAxisRange.endInclusive)
                    },
                ),
                startAxis = priceHistoryStartAxis(
                    labelStyle = axisLabelStyle,
                    axisTitle = axisTitle,
                    purchasePriceValue = purchasePriceValue,
                ),
                bottomAxis = priceHistoryBottomAxis(labelStyle = axisLabelStyle, minX = minX, maxX = maxX),
                marker = priceHistoryMarker(currency = currency, purchasePriceValue = purchasePriceValue),
                decorations = purchasePriceDecorations(purchasePrice = purchasePrice),
                getXStep = { _, minX, maxX -> ((maxX - minX) / X_STEP_DIVISION_COUNT).coerceAtLeast(1.0) },
            ),
            modelProducer = modelProducer,
            zoomState = rememberVicoZoomState(
                zoomEnabled = false,
                initialZoom = Zoom.Content,
                minZoom = Zoom.Content,
                maxZoom = Zoom.Content,
            ),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}

/**
 * A single [LineCartesianLayer.Line] for the market-price series: straight segments
 * ([LineCartesianLayer.Interpolator.Sharp], no smoothing) and a small filled circle at every vertex
 * to keep sparse readings visually honest (see design addendum section 2). When [purchasePriceValue]
 * is known, the line/area fill and the points split into a profit color above it and a loss color
 * below via [LineCartesianLayer.LineFill.double]/[LineCartesianLayer.AreaFill.double] -- a hard
 * threshold at the purchase price, not a continuous scale, matching "above/below what I paid" being
 * a binary signal. Without a purchase price to compare against, the line falls back to a single
 * neutral theme color, same as before this redesign.
 */
@Composable
private fun priceHistoryLineProvider(purchasePriceValue: Double?): LineCartesianLayer.LineProvider {
    val primary = MaterialTheme.colorScheme.primary
    val positive = LocalSemanticColors.current.positive
    val negative = LocalSemanticColors.current.negative
    val areaFillTopAlpha = if (isSystemInDarkTheme()) {
        CHART_AREA_FILL_TOP_ALPHA_DARK
    } else {
        CHART_AREA_FILL_TOP_ALPHA_LIGHT
    }

    val lineFill = if (purchasePriceValue != null) {
        LineCartesianLayer.LineFill.double(
            topFill = Fill(positive),
            bottomFill = Fill(negative),
            splitY = { purchasePriceValue },
        )
    } else {
        LineCartesianLayer.LineFill.single(fill = Fill(primary))
    }
    val areaFill = if (purchasePriceValue != null) {
        LineCartesianLayer.AreaFill.double(
            topFill = Fill(positive.copy(alpha = areaFillTopAlpha)),
            bottomFill = Fill(negative.copy(alpha = areaFillTopAlpha)),
            splitY = { purchasePriceValue },
        )
    } else {
        LineCartesianLayer.AreaFill.single(
            fill = Fill(
                Brush.verticalGradient(
                    listOf(primary.copy(alpha = areaFillTopAlpha), primary.copy(alpha = 0f)),
                ),
            ),
        )
    }
    val pointProvider = if (purchasePriceValue != null) {
        priceHistoryPointProvider(positive = positive, negative = negative, purchasePriceValue = purchasePriceValue)
    } else {
        LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.Point(
                component = rememberShapeComponent(fill = Fill(primary), shape = CircleShape),
                size = CHART_POINT_DIAMETER_DP.dp,
            ),
        )
    }

    return LineCartesianLayer.LineProvider.series(
        LineCartesianLayer.rememberLine(
            fill = lineFill,
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = LocalDimens.current.border.thick),
            areaFill = areaFill,
            pointProvider = pointProvider,
            interpolator = LineCartesianLayer.Interpolator.Sharp,
        ),
    )
}

/** Point markers colored to match [priceHistoryLineProvider]'s profit/loss line split, per point. */
@Composable
private fun priceHistoryPointProvider(
    positive: Color,
    negative: Color,
    purchasePriceValue: Double,
): LineCartesianLayer.PointProvider {
    val positivePoint = LineCartesianLayer.Point(
        component = rememberShapeComponent(fill = Fill(positive), shape = CircleShape),
        size = CHART_POINT_DIAMETER_DP.dp,
    )
    val negativePoint = LineCartesianLayer.Point(
        component = rememberShapeComponent(fill = Fill(negative), shape = CircleShape),
        size = CHART_POINT_DIAMETER_DP.dp,
    )
    return remember(positivePoint, negativePoint, purchasePriceValue) {
        object : LineCartesianLayer.PointProvider {
            override fun getPoint(entry: LineCartesianLayerModel.Entry, extraStore: ExtraStore) =
                if (entry.y >= purchasePriceValue) positivePoint else negativePoint

            override fun getLargestPoint(extraStore: ExtraStore) = positivePoint
        }
    }
}

/**
 * Y axis: plain numbers, horizontal guideline, and the [axisTitle] rotated to read bottom-to-top
 * via Vico's default [VerticalAxis.TitlePosition.Side] (no extra configuration needed for a
 * [VerticalAxis.rememberStart], confirmed by reading `VerticalAxis.kt`'s title-rotation logic). The
 * title gets extra padding on the axis it's rotated around so it doesn't crowd the price labels,
 * and the price labels themselves get vertical padding (a minimum gap between them) plus a small
 * end padding so the numbers don't crowd the chart. When [purchasePriceValue] is known, the axis is
 * built to always be centered on it, exactly [PURCHASE_PRICE_CENTERED_TICK_COUNT] labels total (see
 * [priceHistoryStartAxisItemPlacer] and `PriceHistoryChart`'s `yAxisRange`), and the value formatter
 * colors just that centered label with the outline color already used for the dashed purchase-price
 * line -- a neutral reference color, not a profit/loss signal.
 */
@Composable
private fun priceHistoryStartAxis(
    labelStyle: TextStyle,
    axisTitle: String,
    purchasePriceValue: Double?,
): VerticalAxis<Axis.Position.Vertical.Start> {
    val dimens = LocalDimens.current
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val outline = MaterialTheme.colorScheme.outline
    return VerticalAxis.rememberStart(
        label = rememberTextComponent(
            style = labelStyle,
            padding = Insets(
                top = dimens.padding.extraSmall,
                bottom = dimens.padding.extraSmall,
                end = dimens.padding.extraSmall,
            ),
        ),
        guideline = rememberLineComponent(
            fill = Fill(outlineVariant.copy(alpha = CHART_GUIDELINE_ALPHA)),
            thickness = dimens.border.thin,
        ),
        valueFormatter = priceHistoryStartAxisValueFormatter(
            purchasePriceValue = purchasePriceValue,
            highlightColor = outline,
        ),
        itemPlacer = priceHistoryStartAxisItemPlacer(purchasePriceValue = purchasePriceValue),
        // Vertical padding, not horizontal: the title is drawn unrotated then rotated -90 degrees
        // to read bottom-to-top, so padding on this axis becomes the gap perpendicular to the text
        // -- i.e. the breathing room between the title and the price labels next to it -- rather
        // than padding along the (now vertical) reading direction.
        titleComponent = rememberTextComponent(
            style = labelStyle,
            padding = Insets(vertical = dimens.padding.extraSmall),
        ),
        title = { axisTitle },
    )
}

/**
 * Formats every label via [formatAxisPrice] (always exactly 1 decimal digit) and colors only the
 * purchase-price tick's label; every other tick keeps the plain axis label style.
 */
@Composable
private fun priceHistoryStartAxisValueFormatter(
    purchasePriceValue: Double?,
    highlightColor: Color,
): CartesianValueFormatter = remember(purchasePriceValue, highlightColor) {
    CartesianValueFormatter { _, value, _ ->
        val label = formatAxisPrice(value)
        if (purchasePriceValue != null && abs(value - purchasePriceValue) < PURCHASE_PRICE_TICK_EPSILON) {
            buildAnnotatedString { withStyle(SpanStyle(color = highlightColor)) { append(label) } }
        } else {
            label
        }
    }
}

/**
 * With a purchase price, [VerticalAxis.ItemPlacer.count] places exactly
 * [PURCHASE_PRICE_CENTERED_TICK_COUNT] evenly spaced labels across `yAxisRange` (see
 * `PriceHistoryChart`) -- since that range is built symmetrically around the purchase price (via
 * [purchasePriceCenteredYAxisRange]), the middle one of those labels always lands exactly on it.
 * Without a purchase price there's nothing to center on, so it falls back to the plain step-based
 * placer, which derives its own step from the real measured label height (`maxLabelHeight`, which
 * includes the label's own padding -- see [priceHistoryStartAxis]) and the available `axisHeight`.
 */
@Composable
private fun priceHistoryStartAxisItemPlacer(purchasePriceValue: Double?): VerticalAxis.ItemPlacer =
    if (purchasePriceValue != null) {
        remember { VerticalAxis.ItemPlacer.count(count = { PURCHASE_PRICE_CENTERED_TICK_COUNT }) }
    } else {
        remember { VerticalAxis.ItemPlacer.step() }
    }

/**
 * X axis: single-line "dd.MM" labels in fixed UTC (exact time is only shown in the tap tooltip,
 * see [priceHistoryMarker]). No vertical guideline -- readings aren't evenly spaced in time, so a
 * guideline per point would look noisy rather than informative (design addendum section 2). Labels
 * are placed by [priceHistoryBottomAxisItemPlacer], which adapts their count to the real available
 * width instead of the default aligned placer.
 */
@Composable
private fun priceHistoryBottomAxis(
    labelStyle: TextStyle,
    minX: Double,
    maxX: Double,
): HorizontalAxis<Axis.Position.Horizontal.Bottom> =
    HorizontalAxis.rememberBottom(
        label = rememberTextComponent(style = labelStyle.copy(textAlign = TextAlign.Center)),
        guideline = null,
        itemPlacer = priceHistoryBottomAxisItemPlacer(minX = minX, maxX = maxX),
        valueFormatter = CartesianValueFormatter { _, value, _ ->
            Instant.fromEpochMilliseconds(value.toLong()).toAxisDateLabel()
        },
    )

/**
 * Adaptive X-axis item placer: on every draw, [maxEvenlySpacedLabelCount] works out how many date
 * labels actually fit across [CartesianDrawingContext.layerBounds] -- the real plot area's pixel
 * width, excluding the Y axis's own reserved space -- given the real measured [maxLabelWidth] plus a
 * minimum gap, then [evenlySpacedXValues] divides `[minX, maxX]` into exactly that many equal parts.
 * Labels land at evenly spaced *times*, not necessarily on a real reading -- see [evenlySpacedXValues]
 * for why snapping to the nearest reading was dropped.
 */
@Composable
private fun priceHistoryBottomAxisItemPlacer(minX: Double, maxX: Double): HorizontalAxis.ItemPlacer {
    val dimens = LocalDimens.current
    val minLabelGapPx = with(LocalDensity.current) { dimens.padding.small.toPx() }
    val defaultPlacer = remember { HorizontalAxis.ItemPlacer.aligned() }
    return remember(defaultPlacer, minX, maxX, minLabelGapPx) {
        object : HorizontalAxis.ItemPlacer by defaultPlacer {
            override fun getLabelValues(
                context: CartesianDrawingContext,
                visibleXRange: ClosedFloatingPointRange<Double>,
                fullXRange: ClosedFloatingPointRange<Double>,
                maxLabelWidth: Float,
            ): List<Double> {
                val count = maxEvenlySpacedLabelCount(
                    axisLength = context.layerBounds.width,
                    maxLabelLength = maxLabelWidth,
                    minLabelGap = minLabelGapPx,
                )
                return evenlySpacedXValues(minX = minX, maxX = maxX, count = count)
            }
        }
    }
}

/**
 * Tap tooltip: exact price and exact reading timestamp for the tapped point. [target.x] is already
 * the real captured-at epoch millis (not an interpolated touch position -- the series' x values are
 * literal timestamps), and [LineCartesianLayerMarkerTarget.Point.entry]'s `y` is the exact plotted
 * price, so no extra lookup against the original [PriceSnapshot] list is needed. [guideline] draws
 * the vertical line from the tapped point down to its date on the X axis, tying the tooltip to the
 * exact reading it describes. The price line is tinted with the same profit/loss colors as the
 * chart itself -- green above [purchasePriceValue], red below, untinted when they're exactly equal
 * or there's no purchase price to compare against.
 */
@Composable
private fun priceHistoryMarker(currency: SteamCurrency, purchasePriceValue: Double?): CartesianMarker {
    val dimens = LocalDimens.current
    val labelStyle = MaterialTheme.typography.labelSmall
        .copy(
            fontFamily = AppFontFamilies.jetBrainsMono,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ).tabularNumeric
    val label = rememberTextComponent(
        style = labelStyle,
        lineCount = 2,
        padding = Insets(horizontal = dimens.padding.small, vertical = dimens.padding.extraSmall),
        background = rememberShapeComponent(
            fill = Fill(MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(dimens.radius.small),
        ),
    )
    // The theme's primary color is gold (see mdThemeLightPrimary/mdThemeDarkPrimary), already used
    // as the chart's neutral line color -- reused here so the tap crosshair reads as an accent, not
    // a plain grey guideline.
    val guideline = rememberLineComponent(
        fill = Fill(MaterialTheme.colorScheme.primary),
        thickness = dimens.border.thin,
    )
    val positive = LocalSemanticColors.current.positive
    val negative = LocalSemanticColors.current.negative
    val valueFormatter = remember(currency, purchasePriceValue, positive, negative) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.filterIsInstance<LineCartesianLayerMarkerTarget>().first()
            val point = target.points.first()
            val instant = Instant.fromEpochMilliseconds(target.x.toLong())
            val priceColor = purchasePriceValue?.let {
                when (priceComparedToPurchase(price = point.entry.y, purchasePrice = it)) {
                    PriceComparedToPurchase.ABOVE -> positive
                    PriceComparedToPurchase.BELOW -> negative
                    PriceComparedToPurchase.EQUAL -> null
                }
            }
            val priceText = formatPriceHistoryTooltipPrice(price = point.entry.y, currency = currency)
            buildAnnotatedString {
                if (priceColor != null) {
                    withStyle(SpanStyle(color = priceColor)) { append(priceText) }
                } else {
                    append(priceText)
                }
                append("\n")
                append(formatPriceHistoryTooltipTimestamp(instant))
            }
        }
    }
    return rememberDefaultCartesianMarker(label = label, valueFormatter = valueFormatter, guideline = guideline)
}

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
