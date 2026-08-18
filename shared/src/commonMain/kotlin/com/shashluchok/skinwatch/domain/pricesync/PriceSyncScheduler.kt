package com.shashluchok.skinwatch.domain.pricesync

internal interface PriceSyncScheduler {
    fun schedulePeriodicSync()

    companion object {
        val EMPTY = object : PriceSyncScheduler {
            override fun schedulePeriodicSync() = Unit
        }
    }
}
