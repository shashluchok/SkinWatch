package com.shashluchok.skinwatch.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Every Material3 ColorScheme role is explicitly designed here (see Color.kt for how the roles
// not hand-picked by the M0 spec were derived via the HCT tonal-palette algorithm) -- none of
// them fall back to Material's baseline purple defaults. `surfaceTint` is intentionally not
// passed: it defaults to `primary`, which is exactly what we want here too.
private val LightColorScheme =
    lightColorScheme(
        background = mdThemeLightBackground,
        onBackground = mdThemeLightOnBackground,
        surface = mdThemeLightSurface,
        onSurface = mdThemeLightOnSurface,
        surfaceContainerLow = mdThemeLightSurface,
        surfaceContainer = mdThemeLightSurfaceContainer,
        surfaceContainerHigh = mdThemeLightSurfaceContainerHigh,
        onSurfaceVariant = mdThemeLightOnSurfaceVariant,
        outlineVariant = mdThemeLightOutlineVariant,
        primary = mdThemeLightPrimary,
        onPrimary = mdThemeLightOnPrimary,
        error = mdThemeLightError,
        onError = mdThemeLightOnError,
        primaryContainer = mdThemeLightPrimaryContainer,
        onPrimaryContainer = mdThemeLightOnPrimaryContainer,
        inversePrimary = mdThemeLightInversePrimary,
        secondary = mdThemeLightSecondary,
        onSecondary = mdThemeLightOnSecondary,
        secondaryContainer = mdThemeLightSecondaryContainer,
        onSecondaryContainer = mdThemeLightOnSecondaryContainer,
        tertiary = mdThemeLightTertiary,
        onTertiary = mdThemeLightOnTertiary,
        tertiaryContainer = mdThemeLightTertiaryContainer,
        onTertiaryContainer = mdThemeLightOnTertiaryContainer,
        errorContainer = mdThemeLightErrorContainer,
        onErrorContainer = mdThemeLightOnErrorContainer,
        surfaceVariant = mdThemeLightSurfaceVariant,
        outline = mdThemeLightOutline,
        inverseSurface = mdThemeLightInverseSurface,
        inverseOnSurface = mdThemeLightInverseOnSurface,
        surfaceDim = mdThemeLightSurfaceDim,
        surfaceBright = mdThemeLightSurfaceBright,
        surfaceContainerLowest = mdThemeLightSurfaceContainerLowest,
        surfaceContainerHighest = mdThemeLightSurfaceContainerHighest,
        scrim = mdThemeScrim,
        primaryFixed = mdThemePrimaryFixed,
        primaryFixedDim = mdThemePrimaryFixedDim,
        onPrimaryFixed = mdThemeOnPrimaryFixed,
        onPrimaryFixedVariant = mdThemeOnPrimaryFixedVariant,
        secondaryFixed = mdThemeSecondaryFixed,
        secondaryFixedDim = mdThemeSecondaryFixedDim,
        onSecondaryFixed = mdThemeOnSecondaryFixed,
        onSecondaryFixedVariant = mdThemeOnSecondaryFixedVariant,
        tertiaryFixed = mdThemeTertiaryFixed,
        tertiaryFixedDim = mdThemeTertiaryFixedDim,
        onTertiaryFixed = mdThemeOnTertiaryFixed,
        onTertiaryFixedVariant = mdThemeOnTertiaryFixedVariant,
    )

private val DarkColorScheme =
    darkColorScheme(
        background = mdThemeDarkBackground,
        onBackground = mdThemeDarkOnBackground,
        surface = mdThemeDarkSurface,
        onSurface = mdThemeDarkOnSurface,
        surfaceContainerLow = mdThemeDarkSurface,
        surfaceContainer = mdThemeDarkSurfaceContainer,
        surfaceContainerHigh = mdThemeDarkSurfaceContainerHigh,
        onSurfaceVariant = mdThemeDarkOnSurfaceVariant,
        outlineVariant = mdThemeDarkOutlineVariant,
        primary = mdThemeDarkPrimary,
        onPrimary = mdThemeDarkOnPrimary,
        error = mdThemeDarkError,
        onError = mdThemeDarkOnError,
        primaryContainer = mdThemeDarkPrimaryContainer,
        onPrimaryContainer = mdThemeDarkOnPrimaryContainer,
        inversePrimary = mdThemeDarkInversePrimary,
        secondary = mdThemeDarkSecondary,
        onSecondary = mdThemeDarkOnSecondary,
        secondaryContainer = mdThemeDarkSecondaryContainer,
        onSecondaryContainer = mdThemeDarkOnSecondaryContainer,
        tertiary = mdThemeDarkTertiary,
        onTertiary = mdThemeDarkOnTertiary,
        tertiaryContainer = mdThemeDarkTertiaryContainer,
        onTertiaryContainer = mdThemeDarkOnTertiaryContainer,
        errorContainer = mdThemeDarkErrorContainer,
        onErrorContainer = mdThemeDarkOnErrorContainer,
        surfaceVariant = mdThemeDarkSurfaceVariant,
        outline = mdThemeDarkOutline,
        inverseSurface = mdThemeDarkInverseSurface,
        inverseOnSurface = mdThemeDarkInverseOnSurface,
        surfaceDim = mdThemeDarkSurfaceDim,
        surfaceBright = mdThemeDarkSurfaceBright,
        surfaceContainerLowest = mdThemeDarkSurfaceContainerLowest,
        surfaceContainerHighest = mdThemeDarkSurfaceContainerHighest,
        scrim = mdThemeScrim,
        primaryFixed = mdThemePrimaryFixed,
        primaryFixedDim = mdThemePrimaryFixedDim,
        onPrimaryFixed = mdThemeOnPrimaryFixed,
        onPrimaryFixedVariant = mdThemeOnPrimaryFixedVariant,
        secondaryFixed = mdThemeSecondaryFixed,
        secondaryFixedDim = mdThemeSecondaryFixedDim,
        onSecondaryFixed = mdThemeOnSecondaryFixed,
        onSecondaryFixedVariant = mdThemeOnSecondaryFixedVariant,
        tertiaryFixed = mdThemeTertiaryFixed,
        tertiaryFixedDim = mdThemeTertiaryFixedDim,
        onTertiaryFixed = mdThemeOnTertiaryFixed,
        onTertiaryFixedVariant = mdThemeOnTertiaryFixedVariant,
    )

@Composable
internal fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dimens: Dimens = Dimens.DEFAULT,
    semanticColors: SemanticColors = SemanticColors.DEFAULT,
    motion: Motion = Motion.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
    ) {
        CompositionLocalProvider(
            LocalDimens provides dimens,
            LocalSemanticColors provides semanticColors,
            LocalMotion provides motion,
        ) {
            content()
        }
    }
}
