package com.shashluchok.skinwatch.domain.synclog

/**
 * Records whatever the platform's own scheduler knows that the shared code cannot see -- on Android,
 * the state and attempt count of the background work chains, which is what decides how soon a failed
 * item is actually retried.
 */
internal interface PlatformSyncStateInspector {
    fun inspect()

    companion object {
        val EMPTY = object : PlatformSyncStateInspector {
            override fun inspect() = Unit
        }
    }
}
