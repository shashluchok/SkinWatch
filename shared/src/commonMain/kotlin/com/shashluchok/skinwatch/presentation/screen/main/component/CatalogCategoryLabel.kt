package com.shashluchok.skinwatch.presentation.screen.main.component

import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_agent
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_case
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_collectible
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_graffiti
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_key
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_keychain
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_music_kit
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_patch
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_skin
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_sticker
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__category_sticker_slab
import org.jetbrains.compose.resources.StringResource

internal fun categoryLabel(category: CatalogCategory): StringResource = when (category) {
    CatalogCategory.SKIN -> Res.string.dev__screen_inventory__add_search__category_skin
    CatalogCategory.STICKER -> Res.string.dev__screen_inventory__add_search__category_sticker
    CatalogCategory.STICKER_SLAB -> Res.string.dev__screen_inventory__add_search__category_sticker_slab
    CatalogCategory.KEYCHAIN -> Res.string.dev__screen_inventory__add_search__category_keychain
    CatalogCategory.CASE -> Res.string.dev__screen_inventory__add_search__category_case
    CatalogCategory.KEY -> Res.string.dev__screen_inventory__add_search__category_key
    CatalogCategory.AGENT -> Res.string.dev__screen_inventory__add_search__category_agent
    CatalogCategory.PATCH -> Res.string.dev__screen_inventory__add_search__category_patch
    CatalogCategory.GRAFFITI -> Res.string.dev__screen_inventory__add_search__category_graffiti
    CatalogCategory.MUSIC_KIT -> Res.string.dev__screen_inventory__add_search__category_music_kit
    CatalogCategory.COLLECTIBLE -> Res.string.dev__screen_inventory__add_search__category_collectible
}
