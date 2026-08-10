package com.shashluchok.skinwatch.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.ibm_plex_sans
import com.shashluchok.skinwatch.resources.jetbrains_mono
import com.shashluchok.skinwatch.resources.oswald
import org.jetbrains.compose.resources.Font

private const val TABULAR_FIGURES = "tnum"

/** All price/quantity digits go through this so columns of numbers line up. */
internal val TextStyle.tabularNumeric: TextStyle
    get() = copy(fontFeatureSettings = TABULAR_FIGURES)

internal object AppFontFamilies {
    val oswald: FontFamily
        @Composable get() =
            FontFamily(
                Font(Res.font.oswald, weight = FontWeight.Normal),
                Font(Res.font.oswald, weight = FontWeight.Medium),
                Font(Res.font.oswald, weight = FontWeight.SemiBold),
            )

    val plexSans: FontFamily
        @Composable get() =
            FontFamily(
                Font(Res.font.ibm_plex_sans, weight = FontWeight.Normal),
                Font(Res.font.ibm_plex_sans, weight = FontWeight.Medium),
            )

    val jetBrainsMono: FontFamily
        @Composable get() =
            FontFamily(
                Font(Res.font.jetbrains_mono, weight = FontWeight.Normal),
                Font(Res.font.jetbrains_mono, weight = FontWeight.Medium),
            )
}

@Composable
internal fun appTypography(): Typography {
    val display = AppFontFamilies.oswald
    val body = AppFontFamilies.plexSans

    return Typography(
        bodySmall = TextStyle(fontFamily = body, fontSize = 12.sp, lineHeight = 16.sp),
        bodyMedium = TextStyle(fontFamily = body, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontSize = 16.sp, lineHeight = 24.sp),
        labelSmall = TextStyle(
            fontFamily = body,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        ),
        titleSmall = TextStyle(
            fontFamily = body,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        ),
        titleMedium = TextStyle(
            fontFamily = body,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineLarge = TextStyle(
            fontFamily = display,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        displaySmall = TextStyle(fontFamily = display, fontSize = 36.sp, lineHeight = 44.sp),
        displayMedium = TextStyle(fontFamily = display, fontSize = 45.sp, lineHeight = 52.sp),
        displayLarge = TextStyle(fontFamily = display, fontSize = 57.sp, lineHeight = 64.sp),
    )
}
