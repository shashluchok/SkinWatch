package com.shashluchok.skinwatch.data.storage.synclog

import android.content.Context
import com.shashluchok.skinwatch.data.storage.pricesync.PRICE_SYNC_RETRY_WORK_NAME
import com.shashluchok.skinwatch.data.storage.pricesync.PRICE_SYNC_WORK_NAME
import com.shashluchok.skinwatch.data.storage.pricesync.logWorkState
import com.shashluchok.skinwatch.domain.synclog.PlatformSyncStateInspector
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository

internal class WorkManagerSyncStateInspector(
    private val context: Context,
    private val syncLog: SyncLogRepository,
) : PlatformSyncStateInspector {
    override fun inspect() {
        context.logWorkState(workName = PRICE_SYNC_WORK_NAME, syncLog = syncLog)
        context.logWorkState(workName = PRICE_SYNC_RETRY_WORK_NAME, syncLog = syncLog)
    }
}
