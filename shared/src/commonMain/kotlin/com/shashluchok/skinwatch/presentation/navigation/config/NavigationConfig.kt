package com.shashluchok.skinwatch.presentation.navigation.config

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.shashluchok.skinwatch.presentation.navigation.destination.Inventory
import com.shashluchok.skinwatch.presentation.navigation.destination.Settings
import com.shashluchok.skinwatch.presentation.navigation.destination.Watchlist
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val navigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(Inventory::class, Inventory.serializer())
        subclass(Watchlist::class, Watchlist.serializer())
        subclass(Settings::class, Settings.serializer())
    }
}

internal val navigationConfig = SavedStateConfiguration {
    serializersModule = navigationSerializers
}
