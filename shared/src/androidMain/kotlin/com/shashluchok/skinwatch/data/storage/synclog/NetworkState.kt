package com.shashluchok.skinwatch.data.storage.synclog

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService

/**
 * What connectivity looked like at the moment of a log entry, as a short readable token.
 *
 * This is the one piece of context a "the price did not arrive after the network came back" report
 * is always missing: whether the device actually believed it was online, and whether that link was
 * validated rather than merely present (a captive portal or a just-associated Wi-Fi reports the
 * latter while every request still fails).
 */
internal fun Context.describeNetworkState(): String {
    val manager = getSystemService<ConnectivityManager>() ?: return "connectivity-service-unavailable"
    val capabilities = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }

    return capabilities?.describe() ?: "offline"
}

private fun NetworkCapabilities.describe(): String {
    val transport = when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
        hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
        else -> "other"
    }
    val internet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val validated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    return "$transport${if (internet) "" else "+no-internet"}${if (validated) "" else "+unvalidated"}"
}
