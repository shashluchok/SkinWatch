package com.shashluchok.skinwatch.data.storage.pricesync

import android.content.Context
import androidx.work.WorkManager
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.domain.synclog.info

internal const val PRICE_SYNC_WORK_NAME = "price-sync"
internal const val PRICE_SYNC_RETRY_WORK_NAME = "price-sync-retry"

/**
 * Records what WorkManager currently holds under [workName].
 *
 * The attempt count is the number that explains a retry chain nobody can see failing: under
 * `ExistingWorkPolicy.KEEP` a pending request wins over any new one, and exponential backoff pushes
 * a chain that has already failed several times hours into the future.
 *
 * Read through a listener rather than by blocking on the future -- the add-item flow schedules a
 * retry from the main dispatcher.
 */
internal fun Context.logWorkState(workName: String, syncLog: SyncLogRepository) {
    val infos = WorkManager.getInstance(this).getWorkInfosForUniqueWork(workName)
    infos.addListener(
        {
            runCatching { infos.get() }.onSuccess { states ->
                val description = states
                    .joinToString { "state=${it.state}, attempts=${it.runAttemptCount}" }
                    .ifEmpty { "none" }
                syncLog.info(
                    tag = SyncLogTag.SCHEDULE,
                    message = "'$workName' holds ${states.size} request(s): $description",
                )
            }
        },
        Runnable::run,
    )
}
