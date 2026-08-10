@file:Suppress("ktlint:standard:filename")

package com.shashluchok.skinwatch.presentation.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object Inventory : NavKey

@Serializable
internal data object Watchlist : NavKey

@Serializable
internal data object Settings : NavKey
