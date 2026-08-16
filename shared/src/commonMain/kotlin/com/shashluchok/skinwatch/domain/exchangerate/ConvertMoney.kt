package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.math.roundToLong

/**
 * [targetPerUnitRates] comes from a rate lookup made with `base = targetCurrency` -- so
 * `targetPerUnitRates[X]` means "how many units of X equal 1 unit of targetCurrency". Converting an
 * amount from X to targetCurrency is therefore division, not multiplication.
 */
internal fun convertMoney(
    money: Money,
    targetCurrency: SteamCurrency,
    targetPerUnitRates: Map<SteamCurrency, Double>,
): Money {
    if (money.currency == targetCurrency) return money
    val rate = targetPerUnitRates.getValue(money.currency)
    return money.copy(minorUnits = (money.minorUnits / rate).roundToLong(), currency = targetCurrency)
}
