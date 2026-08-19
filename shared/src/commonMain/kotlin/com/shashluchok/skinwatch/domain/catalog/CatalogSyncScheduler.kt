package com.shashluchok.skinwatch.domain.catalog

internal interface CatalogSyncScheduler {
    fun schedulePeriodicSync()

    companion object {
        val EMPTY = object : CatalogSyncScheduler {
            override fun schedulePeriodicSync() = Unit
        }
    }
}
