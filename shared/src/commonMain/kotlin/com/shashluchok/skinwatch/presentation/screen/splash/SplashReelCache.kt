package com.shashluchok.skinwatch.presentation.screen.splash

import com.shashluchok.skinwatch.resources.Res
import io.github.alexzhirkevich.compottie.LottieComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val REEL_ASSET_PATH_DARK = "files/splash_reel.json"
private const val REEL_ASSET_PATH_LIGHT = "files/splash_reel_light.json"

/**
 * Holds the parsed reel composition so a preload started before the first Compose frame -- while
 * the platform's native splash is still up -- is reused instead of leaving the reel blank for as
 * long as its ~200 KB Lottie JSON takes to read and parse.
 *
 * A path present in the map means its load was attempted; a null value means that attempt failed.
 */
object SplashReelCache {
    private val mutableCompositions = MutableStateFlow<Map<String, LottieComposition?>>(emptyMap())
    private val loadMutex = Mutex()

    internal val compositions: StateFlow<Map<String, LottieComposition?>> = mutableCompositions.asStateFlow()

    fun isReady(isDarkTheme: Boolean): Boolean = mutableCompositions.value.containsKey(assetPath(isDarkTheme))

    suspend fun preload(isDarkTheme: Boolean) {
        val path = assetPath(isDarkTheme)
        if (mutableCompositions.value.containsKey(path)) return
        loadMutex.withLock {
            if (mutableCompositions.value.containsKey(path)) return
            val composition = withContext(Dispatchers.Default) {
                runCatching {
                    LottieComposition.parse(Res.readBytes(path).decodeToString())
                }.getOrNull()
            }
            mutableCompositions.update { it + (path to composition) }
        }
    }

    /** The splash plays once per process, so its composition is dead weight once the screen is gone. */
    fun release() {
        mutableCompositions.value = emptyMap()
    }

    internal fun cached(isDarkTheme: Boolean): LottieComposition? = mutableCompositions.value[assetPath(isDarkTheme)]

    internal fun assetPath(isDarkTheme: Boolean) = if (isDarkTheme) REEL_ASSET_PATH_DARK else REEL_ASSET_PATH_LIGHT
}
