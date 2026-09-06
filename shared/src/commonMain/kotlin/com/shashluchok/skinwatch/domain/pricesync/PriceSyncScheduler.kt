package com.shashluchok.skinwatch.domain.pricesync

internal interface PriceSyncScheduler {
    fun schedulePeriodicSync()

    /**
     * Asks for one extra sync as soon as the device is online again, without waiting out
     * [PRICE_SYNC_INTERVAL]. Used after a fetch failed, so a price missed because of a dead
     * connection is filled in when the connection returns rather than hours later.
     */
    fun scheduleRetrySync()

    companion object {
        val EMPTY = object : PriceSyncScheduler {
            override fun schedulePeriodicSync() = Unit

            override fun scheduleRetrySync() = Unit
        }
    }
}
