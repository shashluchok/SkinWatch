package com.shashluchok.skinwatch.presentation.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.theme.AppFontFamilies
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.screen_splash__tagline
import com.shashluchok.skinwatch.resources.screen_splash__wordmark
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val REEL_ASSET_ASPECT_RATIO = 260f / 120f
private const val REEL_ASSET_PATH = "files/splash_reel.json"

// Must finish (delay + duration) comfortably before SplashViewModel.SPLASH_DURATION (4.5s),
// leaving the fully revealed wordmark/tagline on screen for a beat before the crossfade to
// MainScreen starts. Keep these below ~4s if SPLASH_DURATION ever changes.
private const val WORDMARK_DELAY_MS = 3_200
private const val TAGLINE_DELAY_MS = 3_300
private const val REVEAL_DURATION_MS = 550

@Composable
internal fun SplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    val currentOnFinish by rememberUpdatedState(onFinish)

    LaunchedEffect(state.isReady) {
        if (state.isReady) currentOnFinish()
    }

    SplashScreen(modifier = modifier)
}

@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    val motion = LocalMotion.current

    val wordmarkAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            wordmarkAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = REVEAL_DURATION_MS,
                    delayMillis = WORDMARK_DELAY_MS,
                    easing = motion.easing.emphasizedDecelerate,
                ),
            )
        }
        launch {
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = REVEAL_DURATION_MS,
                    delayMillis = TAGLINE_DELAY_MS,
                    easing = motion.easing.emphasizedDecelerate,
                ),
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(dimens.padding.extraSmall, Alignment.CenterVertically),
        ) {
            ReelAnimation(modifier = Modifier.fillMaxWidth().testTag(SplashScreen.Tag.SLOT_STRIP))
            Text(
                modifier = Modifier
                    .testTag(SplashScreen.Tag.WORDMARK)
                    .graphicsLayer { alpha = wordmarkAlpha.value },
                text = stringResource(Res.string.screen_splash__wordmark),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                modifier = Modifier
                    .testTag(SplashScreen.Tag.TAGLINE)
                    .graphicsLayer { alpha = taglineAlpha.value },
                text = stringResource(Res.string.screen_splash__tagline),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppFontFamilies.jetBrainsMono),
            )
        }
    }
}

@Composable
private fun ReelAnimation(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes(REEL_ASSET_PATH).decodeToString())
    }
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)

    Image(
        painter = rememberLottiePainter(composition = composition, progress = { progress }),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = modifier.aspectRatio(REEL_ASSET_ASPECT_RATIO),
    )
}

internal object SplashScreen {
    object Tag {
        const val ROOT = "SplashScreen"
        const val SLOT_STRIP = "$ROOT.slotStrip"
        const val WORDMARK = "$ROOT.wordmark"
        const val TAGLINE = "$ROOT.tagline"
    }
}
