package com.shashluchok.skinwatch.data.steam

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

internal actual fun currentDeviceRegionCode(): String? = NSLocale.currentLocale.countryCode
