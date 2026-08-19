package com.shashluchok.skinwatch.domain.catalog

/**
 * One entry in the local, offline-searchable index of every tradable CS2 item. Populated entirely
 * from a community-maintained dataset
 */
internal data class CatalogItem(
    val marketHashName: String,
    val displayName: String,
    val iconUrl: String,
    val category: CatalogCategory,
)

internal enum class CatalogCategory {
    SKIN,
    STICKER,
    STICKER_SLAB,
    KEYCHAIN,
    CASE,
    KEY,
    AGENT,
    PATCH,
    GRAFFITI,
    MUSIC_KIT,
    COLLECTIBLE,
}
