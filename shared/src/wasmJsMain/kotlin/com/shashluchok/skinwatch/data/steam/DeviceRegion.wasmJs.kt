package com.shashluchok.skinwatch.data.steam

// Kotlin/Wasm's JS interop (external/@JsFun) differs from Kotlin/JS's dynamic js(...) snippets
// used in DeviceRegion.js.kt, and no real WasmJs target consumes this yet -- returning null is
// honest (resolveSteamCurrency already treats null as "fall back to USD") rather than guessing at
// an unverified interop call.
internal actual fun currentDeviceRegionCode(): String? = null
