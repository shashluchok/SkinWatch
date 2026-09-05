package com.shashluchok.skinwatch.presentation.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.theme.AppFontFamilies
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.screen_splash__tagline
import com.shashluchok.skinwatch.resources.screen_splash__wordmark
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private const val REEL_ASSET_ASPECT_RATIO = 260f / 120f
private const val REEL_ASSET_PATH_DARK = "files/splash_reel.json"
private const val REEL_ASSET_PATH_LIGHT = "files/splash_reel_light.json"
private const val REEL_NARRATIVE_END_FRAME = 205f
private const val REEL_COMPOSITION_END_FRAME = 600f
private const val REEL_NARRATIVE_END_PROGRESS =
    REEL_NARRATIVE_END_FRAME / REEL_COMPOSITION_END_FRAME

private const val WORDMARK_REVEAL_DURATION_MS = 550
private const val TAGLINE_REVEAL_DURATION_MS = 650

@Composable
internal fun SplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val density = LocalDensity.current

    val slideDistancePx = with(density) { -dimens.padding.small.toPx() }
    val wordmarkAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    var isReelFinished by remember { mutableStateOf(false) }
    val currentOnFinish by rememberUpdatedState(onFinish)

    LaunchedEffect(isReelFinished) {
        if (!isReelFinished) return@LaunchedEffect
        joinAll(
            launch {
                wordmarkAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = WORDMARK_REVEAL_DURATION_MS,
                    ),
                )
            },
            launch {
                taglineAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = TAGLINE_REVEAL_DURATION_MS,
                    ),
                )
            },
        )

        currentOnFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(SplashScreen.Tag.ROOT),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = dimens.padding.extraSmall,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            ReelAnimation(
                onFinish = { isReelFinished = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SplashScreen.Tag.SLOT_STRIP),
            )
            Text(
                modifier = Modifier
                    .testTag(SplashScreen.Tag.WORDMARK)
                    .revealTransition(alpha = wordmarkAlpha, slideDistancePx = slideDistancePx),
                text = stringResource(Res.string.screen_splash__wordmark),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                modifier = Modifier
                    .testTag(SplashScreen.Tag.TAGLINE)
                    .revealTransition(alpha = taglineAlpha, slideDistancePx = slideDistancePx),
                text = stringResource(Res.string.screen_splash__tagline),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppFontFamilies.jetBrainsMono),
            )
        }
    }
}

@Composable
private fun ReelAnimation(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val compositions by SplashReelCache.compositions.collectAsStateWithLifecycle()
    val composition = compositions[SplashReelCache.assetPath(isDarkTheme = isDarkTheme)]
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)
    val currentOnFinish by rememberUpdatedState(onFinish)

    LaunchedEffect(isDarkTheme) {
        SplashReelCache.preload(isDarkTheme = isDarkTheme)
        // A composition that failed to load never advances progress, so the reel would otherwise
        // hold the splash forever instead of letting the app through.
        if (SplashReelCache.cached(isDarkTheme = isDarkTheme) == null) currentOnFinish()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { progress }.first { it >= REEL_NARRATIVE_END_PROGRESS }
        currentOnFinish()
    }

    Image(
        painter = rememberLottiePainter(composition = composition, progress = { progress }),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = modifier.aspectRatio(REEL_ASSET_ASPECT_RATIO),
    )
}

/** Fades and slides content down into place, driven by a single alpha progress value. */
private fun Modifier.revealTransition(alpha: Animatable<Float, AnimationVector1D>, slideDistancePx: Float) =
    graphicsLayer {
        this.alpha = alpha.value
        translationY = (1f - alpha.value) * slideDistancePx
    }

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

    internal fun cached(isDarkTheme: Boolean): LottieComposition? = mutableCompositions.value[assetPath(isDarkTheme)]

    internal fun assetPath(isDarkTheme: Boolean) = if (isDarkTheme) REEL_ASSET_PATH_DARK else REEL_ASSET_PATH_LIGHT
}

internal object SplashScreen {
    object Tag {
        const val ROOT = "SplashScreen"
        const val SLOT_STRIP = "$ROOT.slotStrip"
        const val WORDMARK = "$ROOT.wordmark"
        const val TAGLINE = "$ROOT.tagline"
    }
}
