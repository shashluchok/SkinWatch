package com.shashluchok.skinwatch.domain.pricesync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Which items have a price request in the air, by `marketHashName`.
 *
 * One instance is shared by every caller that fetches a price: a hand-added item fetches its first
 * price while a scheduled run may already be walking the inventory, so this is a set rather than the
 * single name a lone sequential run would need.
 */
internal class PriceFetchProgress {
    private val mutableInFlight = MutableStateFlow<Set<String>>(emptySet())
    val inFlight: StateFlow<Set<String>> = mutableInFlight.asStateFlow()

    suspend fun <T> track(marketHashName: String, fetch: suspend () -> T): T {
        mutableInFlight.update { it + marketHashName }
        return try {
            fetch()
        } finally {
            mutableInFlight.update { it - marketHashName }
        }
    }
}
