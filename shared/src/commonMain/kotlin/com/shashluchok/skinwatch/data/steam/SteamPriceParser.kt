package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency

/**
 * Parses the localized price strings Steam's `priceoverview` endpoint returns (e.g. "$51.93",
 * "35,83€", "1 560,--€", "3419,34 руб.") into exact minor units. Steam gives no numeric price
 * field for this endpoint. Each currency uses region-specific formatting: USD and GBP with
 * comma thousands separators and dot decimals; EUR with space thousands separators, comma
 * decimals, and the ",--" placeholder for round amounts; RUB with space thousands separators,
 * comma decimals, and textual suffix.
 */
internal object SteamPriceParser {
    private const val CENTS_PLACEHOLDER = "--"
    private const val ZERO_CENTS = "00"
    private const val MINOR_UNIT_SCALE = 100L

    private data class Format(
        val prefix: String = "",
        val suffix: String = "",
        val thousandsSeparator: String,
        val decimalSeparator: String,
    )

    private val formats = mapOf(
        SteamCurrency.USD to Format(prefix = "$", thousandsSeparator = ",", decimalSeparator = "."),
        SteamCurrency.GBP to Format(prefix = "£", thousandsSeparator = ",", decimalSeparator = "."),
        SteamCurrency.EUR to Format(suffix = "€", thousandsSeparator = " ", decimalSeparator = ","),
        SteamCurrency.RUB to Format(suffix = " руб.", thousandsSeparator = " ", decimalSeparator = ","),
    )

    fun parse(text: String, currency: SteamCurrency): Money {
        val format = formats.getValue(currency)
        require(text.startsWith(format.prefix) && text.endsWith(format.suffix)) {
            "\"$text\" does not match the expected $currency price format"
        }

        val withoutSymbols = text
            .removePrefix(format.prefix)
            .removeSuffix(format.suffix)
        val withZeroCents = if (withoutSymbols.endsWith(format.decimalSeparator + CENTS_PLACEHOLDER)) {
            withoutSymbols.dropLast(CENTS_PLACEHOLDER.length) + ZERO_CENTS
        } else {
            withoutSymbols
        }
        val withoutThousands = withZeroCents.replace(format.thousandsSeparator, "")

        val decimalIndex = withoutThousands.indexOf(format.decimalSeparator)
        require(decimalIndex >= 0) { "\"$text\" is missing a decimal part" }

        val wholePart = withoutThousands.substring(0, decimalIndex)
        val fractionalPart = withoutThousands.substring(decimalIndex + format.decimalSeparator.length)
        require(fractionalPart.length == 2) { "\"$text\" does not have exactly 2 fractional digits" }

        val whole = wholePart.toLongOrNull()
            ?: throw IllegalArgumentException("\"$text\" has a non-numeric whole part")
        val fractional = fractionalPart.toLongOrNull()
            ?: throw IllegalArgumentException("\"$text\" has a non-numeric fractional part")

        return Money(minorUnits = whole * MINOR_UNIT_SCALE + fractional, currency = currency)
    }
}
