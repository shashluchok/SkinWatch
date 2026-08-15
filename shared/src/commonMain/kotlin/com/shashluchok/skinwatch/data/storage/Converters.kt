package com.shashluchok.skinwatch.data.storage

import androidx.room3.ColumnTypeConverter
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.time.Instant

internal fun instantToEpochMillis(instant: Instant): Long = instant.toEpochMilliseconds()

internal fun epochMillisToInstant(epochMillis: Long): Instant = Instant.fromEpochMilliseconds(epochMillis)

internal fun steamCurrencyToId(currency: SteamCurrency): Int = currency.id

internal fun idToSteamCurrency(id: Int): SteamCurrency =
    SteamCurrency.entries.firstOrNull { it.id == id }
        ?: throw IllegalArgumentException("Unknown SteamCurrency id: $id")

/**
 * Room's `@ColumnTypeConverter` wrapper around the pure functions above -- kept thin on purpose so
 * the conversion logic itself is testable without a real database, see [ConvertersTest]. Room 3.0
 * renamed the single-column converter annotation from `@TypeConverter`/`@TypeConverters` (Room 2.x)
 * to `@ColumnTypeConverter`/`@ColumnTypeConverters`, confirmed against the current official Room
 * documentation while implementing this class.
 */
internal class Converters {
    @ColumnTypeConverter
    fun fromInstant(instant: Instant): Long = instantToEpochMillis(instant)

    @ColumnTypeConverter
    fun toInstant(epochMillis: Long): Instant = epochMillisToInstant(epochMillis)

    @ColumnTypeConverter
    fun fromSteamCurrency(currency: SteamCurrency): Int = steamCurrencyToId(currency)

    @ColumnTypeConverter
    fun toSteamCurrency(id: Int): SteamCurrency = idToSteamCurrency(id)
}
