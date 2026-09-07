package com.shashluchok.skinwatch.domain.synclog

/**
 * Hands the rendered log to whatever the platform uses to move a file off the device. Takes text
 * rather than entries so the exported file and the on-screen list are the same rendering.
 */
internal interface SyncLogExporter {
    val isSupported: Boolean

    /** `false` when the platform had nothing to hand the file to, so the UI can say so. */
    suspend fun export(text: String): Boolean

    companion object {
        val EMPTY = object : SyncLogExporter {
            override val isSupported: Boolean = false

            override suspend fun export(text: String): Boolean = false
        }
    }
}
