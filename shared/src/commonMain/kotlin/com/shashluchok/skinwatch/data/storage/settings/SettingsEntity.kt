package com.shashluchok.skinwatch.data.storage.settings

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** [SettingsEntity] is always exactly one row, keyed by this fixed id. */
internal const val SETTINGS_ROW_ID = 0

@Entity(tableName = "Settings")
internal data class SettingsEntity(
    @PrimaryKey val id: Int = SETTINGS_ROW_ID,
    val selectedCurrencyId: Int?,
)
