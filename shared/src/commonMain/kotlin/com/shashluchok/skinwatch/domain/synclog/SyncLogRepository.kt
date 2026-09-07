package com.shashluchok.skinwatch.domain.synclog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Diagnostic trail of the price-sync path, kept on the device instead of in the platform console: a
 * sync failure typically surfaces hours later, from a background worker, in a process lifetime that
 * has since ended, and none of that is still attached to a console session by the time anyone looks.
 *
 * [log] is deliberately not `suspend`. Schedulers and workers record from non-suspending code, and
 * recording must never be able to delay -- or fail -- the work it is describing, so implementations
 * hand the entry off and return immediately.
 */
internal interface SyncLogRepository {
    fun observeEntries(): Flow<List<SyncLogEntry>>

    fun log(
        level: SyncLogLevel,
        tag: SyncLogTag,
        message: String,
        marketHashName: String? = null,
    )

    fun clear()

    companion object {
        val EMPTY = object : SyncLogRepository {
            override fun observeEntries(): Flow<List<SyncLogEntry>> = flowOf(emptyList())

            override fun log(
                level: SyncLogLevel,
                tag: SyncLogTag,
                message: String,
                marketHashName: String?,
            ) = Unit

            override fun clear() = Unit
        }
    }
}

internal fun SyncLogRepository.info(tag: SyncLogTag, message: String, marketHashName: String? = null) =
    log(level = SyncLogLevel.INFO, tag = tag, message = message, marketHashName = marketHashName)

internal fun SyncLogRepository.warn(tag: SyncLogTag, message: String, marketHashName: String? = null) =
    log(level = SyncLogLevel.WARN, tag = tag, message = message, marketHashName = marketHashName)

internal fun SyncLogRepository.error(tag: SyncLogTag, message: String, marketHashName: String? = null) =
    log(level = SyncLogLevel.ERROR, tag = tag, message = message, marketHashName = marketHashName)
