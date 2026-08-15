package com.shashluchok.skinwatch.data.steam

private const val REGION_TAG_LENGTH = 2

internal actual fun currentDeviceRegionCode(): String? {
    val languageTag = js("navigator.language") as? String ?: return null
    val region = languageTag.substringAfterLast(delimiter = "-", missingDelimiterValue = "")
    return region.takeIf { it.length == REGION_TAG_LENGTH }?.uppercase()
}
