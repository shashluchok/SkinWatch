package com.shashluchok.skinwatch.data.steam.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchRenderResponseDto(
    val success: Boolean,
    @SerialName("total_count") val totalCount: Int = 0,
    val results: List<SearchResultDto> = emptyList(),
)

@Serializable
internal data class SearchResultDto(
    val name: String,
    @SerialName("hash_name") val hashName: String,
    @SerialName("sell_listings") val sellListings: Int = 0,
    // Null when Steam reports zero active listings for the item.
    @SerialName("sell_price") val sellPrice: Long? = null,
    @SerialName("asset_description") val assetDescription: AssetDescriptionDto,
)

@Serializable
internal data class AssetDescriptionDto(
    @SerialName("icon_url") val iconUrl: String,
)
