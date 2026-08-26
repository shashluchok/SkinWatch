package com.shashluchok.skinwatch.data.storage.debug

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** [DebugSettingsEntity] is always exactly one row, keyed by this fixed id. */
internal const val DEBUG_SETTINGS_ROW_ID = 0

@Entity(tableName = "DebugSettings")
internal data class DebugSettingsEntity(
    @PrimaryKey val id: Int = DEBUG_SETTINGS_ROW_ID,
    val showSplashScreen: Boolean = true,
    val mockDataEnabled: Boolean = false,
)
