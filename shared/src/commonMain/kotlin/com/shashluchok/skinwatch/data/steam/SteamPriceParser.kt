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
        if (!text.startsWith(format.prefix) || !text.endsWith(format.suffix)) {
            throw SteamPriceFormatException("\"$text\" does not match the expected $currency price format")
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
        // A whole amount can come back with the fractional part dropped entirely ("83 руб.") rather
        // than padded the way EUR pads it with ",--". Steam does this per locale, so treating the
        // separator as optional is what keeps a perfectly valid price from reading as a malformed
        // response and leaving the item permanently unpriced.
        val hasFractionalPart = decimalIndex >= 0
        val wholePart = if (hasFractionalPart) withoutThousands.substring(0, decimalIndex) else withoutThousands
        val fractionalPart = if (hasFractionalPart) {
            withoutThousands.substring(decimalIndex + format.decimalSeparator.length)
        } else {
            ZERO_CENTS
        }
        if (fractionalPart.length != ZERO_CENTS.length) {
            throw SteamPriceFormatException(
                "\"$text\" does not have exactly ${ZERO_CENTS.length} fractional digits",
            )
        }

        val whole = wholePart.toDigitsOrThrow(text = text, part = "whole")
        val fractional = fractionalPart.toDigitsOrThrow(text = text, part = "fractional")

        return Money(minorUnits = whole * MINOR_UNIT_SCALE + fractional, currency = currency)
    }

    private fun String.toDigitsOrThrow(text: String, part: String): Long =
        toLongOrNull() ?: throw SteamPriceFormatException("\"$text\" has a non-numeric $part part")
}

/**
 * A price string Steam sent that this parser cannot read.
 *
 * Its own type on purpose, and deliberately not an [IllegalArgumentException]: the mapping in
 * `SteamMarketRepositoryImpl` used to treat every `IllegalArgumentException` as an unusable answer,
 * which silently swept up `UnresolvedAddressException` -- a DNS failure -- and reported a whole
 * inventory as unpriceable. Only what this parser itself rejects should be read that way.
 */
internal class SteamPriceFormatException(
    message: String,
) : Exception(message)
