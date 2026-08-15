package com.shashluchok.skinwatch.data.steam.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `lowestPrice`/`medianPrice`/`volume` are independently nullable -- Steam returns
 * `success: true` with none of them present for items that currently have no active listings.
 */
@Serializable
internal data class PriceOverviewResponseDto(
    val success: Boolean,
    @SerialName("lowest_price") val lowestPrice: String? = null,
    @SerialName("median_price") val medianPrice: String? = null,
    val volume: String? = null,
)
