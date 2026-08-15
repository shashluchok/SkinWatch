package com.shashluchok.skinwatch.data.steam

import java.util.Locale

internal actual fun currentDeviceRegionCode(): String? =
    Locale.getDefault().country.takeIf { it.isNotBlank() }
