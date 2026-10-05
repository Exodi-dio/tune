package com.exodidio.tune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.exodidio.tune.R
import com.exodidio.tune.settings.ThemeMode

data class TuneColors(
    val background: Color,
    val playerBackdrop: Color,
    val card: Color,
    val glass: Color,
    val glassOpaque: Color,
    val glassElevated: Color,
    val sliderInactive: Color,
    val buttonSecondary: Color,
    val textFieldClear: Color,
    val borderGlass: Color,
    val textMain: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val foregroundSubtle: Color,
    val success: Color,
    val navigationActive: Color,
    // Audio quality badge accents mirror the desktop quality palette.
    val qualityHiRes: Color = Color(0xFFF59E0B),
    val qualityDsd: Color = Color(0xFFE879F9),
    val qualityDsdSurface: Color = Color(0xFFD946EF),
)

private val LightColors = TuneColors(
    background = Color(0xFFF4F4F5),
    playerBackdrop = Color(0xFF18181B),
    card = Color.White,
    glass = Color.White.copy(alpha = 0.4f),
    glassOpaque = Color.White,
    glassElevated = Color.White.copy(alpha = 0.90f),
    // The fullscreen player sits over artwork, so this remains translucent to
    // let that backdrop show through instead of reading as a solid gray bar.
    sliderInactive = Color.White.copy(alpha = 0.10f),
    buttonSecondary = Color(0xFFE4E4E7),
    textFieldClear = Color(0xFFD4D4D8),
    borderGlass = Color.Black.copy(alpha = 0.10f),
    textMain = Color(0xFF0A0A0A),
    textMuted = Color(0xFF52525B),
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    foregroundSubtle = Color.White.copy(alpha = 0.46f),
    success = Color(0xFF16A34A),
    navigationActive = Color.Black.copy(alpha = 0.08f),
    qualityHiRes = Color(0xFFF59E0B),
    qualityDsd = Color(0xFFE879F9),
    qualityDsdSurface = Color(0xFFD946EF),
)

private val DarkColors = TuneColors(
    background = Color(0xFF18181B),
    playerBackdrop = Color(0xFF0A0A0A),
    card = Color(0xFF27272A),
    glass = Color(0xFF232326).copy(alpha = 0.4f),
    glassOpaque = Color(0xFF232326),
    glassElevated = Color(0xFF37373C).copy(alpha = 0.40f),
    sliderInactive = Color.White.copy(alpha = 0.10f),
    buttonSecondary = Color(0xFF52525B),
    textFieldClear = Color(0xFF3F3F46),
    borderGlass = Color.White.copy(alpha = 0.10f),
    textMain = Color.White,
    textMuted = Color(0xFFA1A1AA),
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    foregroundSubtle = Color.White.copy(alpha = 0.46f),
    success = Color(0xFF4ADE80),
    navigationActive = Color.Black.copy(alpha = 0.40f),
    qualityHiRes = Color(0xFFF59E0B),
    qualityDsd = Color(0xFFE879F9),
    qualityDsdSurface = Color(0xFFD946EF),
)

private val AmoledColors = TuneColors(
    background = Color(0xFF000000),
    playerBackdrop = Color(0xFF000000),
    card = Color(0xFF000000),
    glass = Color(0xFF000000).copy(alpha = 0.4f),
    glassOpaque = Color(0xFF000000),
    glassElevated = Color(0xFF000000).copy(alpha = 0.40f),
    sliderInactive = Color.White.copy(alpha = 0.10f),
    buttonSecondary = Color(0xFF52525B),
    textFieldClear = Color(0xFF3F3F46),
    borderGlass = Color.White.copy(alpha = 0.10f),
    textMain = Color.White,
    textMuted = Color(0xFFA1A1AA),
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    foregroundSubtle = Color.White.copy(alpha = 0.46f),
    success = Color(0xFF4ADE80),
    navigationActive = Color.Black.copy(alpha = 0.40f),
    qualityHiRes = Color(0xFFF59E0B),
    qualityDsd = Color(0xFFE879F9),
    qualityDsdSurface = Color(0xFFD946EF),
)

val LocalTuneColors = staticCompositionLocalOf { LightColors }

// Apple Music's SF Pro Display face, with only the weights used by the app.
val SFProDisplay = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.W400),
    Font(R.font.sf_pro_display_medium, FontWeight.W500),
    Font(R.font.sf_pro_display_semibold, FontWeight.W600),
    Font(R.font.sf_pro_display_bold, FontWeight.W700),
    Font(R.font.sf_pro_display_heavy, FontWeight.W800),
)

private val TuneTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 34.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 22.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.W700, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.W400, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 11.sp),
).withFamily(SFProDisplay)

private fun Typography.withFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

@Composable
fun TuneTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Amoled -> true
    }
    val colors = when (themeMode) {
        ThemeMode.Amoled -> AmoledColors
        else -> if (darkTheme) DarkColors else LightColors
    }
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            surface = colors.glass,
            onBackground = colors.textMain,
            onSurface = colors.textMain,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            surface = colors.glass,
            onBackground = colors.textMain,
            onSurface = colors.textMain,
        )
    }

    CompositionLocalProvider(LocalTuneColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, typography = TuneTypography, content = content)
    }
}
