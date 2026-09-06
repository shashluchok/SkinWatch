package com.shashluchok.skinwatch.domain.pricesync

internal class FakePriceSyncScheduler : PriceSyncScheduler {
    var periodicSyncScheduledCount = 0
        private set
    var retrySyncScheduledCount = 0
        private set

    override fun schedulePeriodicSync() {
        periodicSyncScheduledCount++
    }

    override fun scheduleRetrySync() {
        retrySyncScheduledCount++
    }
}
