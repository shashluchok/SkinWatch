package com.shashluchok.skinwatch.presentation.util

import com.shashluchok.skinwatch.domain.steam.Money
import kotlin.math.abs

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
private const val PERCENT_MULTIPLIER = 100
private const val TENTHS_PER_UNIT = 10

/**
 * Plain and locale-independent for now, e.g. `49.0 USD` -- the currency symbol, its position and
 * locale-aware separators are a later visual-design pass, not a decision for any one screen.
 *
 * Shared rather than repeated per screen: the same amount appearing as `49.0 USD` in one place and
 * `49,00 $` in another is the kind of drift nobody notices until it is everywhere.
 */
internal fun formatMoney(money: Money): String {
    val major = money.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT

    return "$major ${money.currency.name}"
}

/** Signed amount with its optional share of the baseline, e.g. `+140.5 USD (+12.8%)`. */
internal fun formatSignedDelta(delta: Money, fraction: Float?): String {
    val sign = if (delta.minorUnits >= 0) "+" else "-"
    val amount = formatMoney(Money(minorUnits = abs(delta.minorUnits), currency = delta.currency))
    val percent = fraction
        ?.let {
            val tenths = (abs(it) * PERCENT_MULTIPLIER * TENTHS_PER_UNIT).toLong()
            " ($sign${tenths / TENTHS_PER_UNIT}.${tenths % TENTHS_PER_UNIT}%)"
        }.orEmpty()

    return "$sign$amount$percent"
}
