package com.shashluchok.skinwatch.presentation.screen.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__currency_label__eur
import com.shashluchok.skinwatch.resources.dev__currency_label__gbp
import com.shashluchok.skinwatch.resources.dev__currency_label__rub
import com.shashluchok.skinwatch.resources.dev__currency_label__usd
import org.jetbrains.compose.resources.StringResource

internal fun currencyLabel(currency: SteamCurrency): StringResource = when (currency) {
    SteamCurrency.USD -> Res.string.dev__currency_label__usd
    SteamCurrency.GBP -> Res.string.dev__currency_label__gbp
    SteamCurrency.EUR -> Res.string.dev__currency_label__eur
    SteamCurrency.RUB -> Res.string.dev__currency_label__rub
}
