package com.shashluchok.skinwatch.data.catalog.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Same top-level shape across all 11 category files this app consumes (skins_not_grouped,
 * stickers, sticker_slabs, keychains, crates, keys, agents, patches, graffiti, music_kits,
 * collectibles). `marketHashName` is nullable: non-tradable entries (event items, achievement
 * coins) report it as `null` and must be dropped by the caller, not treated as a decode error.
 */
@Serializable
internal data class CatalogEntryDto(
    @SerialName("market_hash_name") val marketHashName: String?,
    val name: String,
    val image: String,
)
