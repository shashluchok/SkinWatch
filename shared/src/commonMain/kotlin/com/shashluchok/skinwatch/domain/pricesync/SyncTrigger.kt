package com.shashluchok.skinwatch.domain.pricesync

/**
 * Which entry point asked for a run. Recorded in the diagnostic log because several triggers can
 * fire at once -- notably when connectivity returns and every network-constrained worker is released
 * together -- and only one of them wins the run.
 */
internal enum class SyncTrigger {
    PERIODIC_WORKER,
    RETRY_WORKER,
    APP_OPEN_STALE_CHECK,
    MANUAL,
}
