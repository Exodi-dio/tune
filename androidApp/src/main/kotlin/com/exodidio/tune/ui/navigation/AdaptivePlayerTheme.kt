package com.exodidio.tune.ui.navigation

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AdaptivePlayerColors(
    val background: Color,
    val accent: Color,
    val dim: Color,
    val faint: Color,
) {
    companion object {
        val FallbackDark = AdaptivePlayerColors(
            background = Color(0xFF1C1C1E),
            accent = Color.White,
            dim = Color.White.copy(alpha = 0.55f),
            faint = Color.White.copy(alpha = 0.32f),
        )
        val FallbackLight = AdaptivePlayerColors(
            background = Color(0xFFF4F1EA),
            accent = Color(0xFF1C1C1E),
            dim = Color(0xFF1C1C1E).copy(alpha = 0.6f),
            faint = Color(0xFF1C1C1E).copy(alpha = 0.38f),
        )
    }
}

fun Bitmap.extractAdaptivePlayerColors(): AdaptivePlayerColors {
    val palette = runCatching {
        Palette.from(this).maximumColorCount(16).generate()
    }.getOrNull() ?: return AdaptivePlayerColors.FallbackDark
    val bgInt = palette.getDominantColor(
        palette.getVibrantColor(
            palette.getMutedColor(
                palette.getDarkVibrantColor(
                    palette.getDarkMutedColor(0xFF1C1C1E.toInt())
                )
            )
        )
    )
    val background = Color(bgInt)
    val isBgDark = ColorUtils.calculateLuminance(bgInt) < 0.35
    val candidates = listOfNotNull(
        palette.vibrantSwatch,
        palette.lightVibrantSwatch,
        palette.darkVibrantSwatch,
        palette.mutedSwatch,
        palette.lightMutedSwatch,
        palette.darkMutedSwatch,
        palette.dominantSwatch,
    ).sortedByDescending { it.population }
    var accentInt: Int? = null
    for (swatch in candidates) {
        val ratio = runCatching {
            ColorUtils.calculateContrast(swatch.rgb, bgInt)
        }.getOrDefault(0.0)
        if (ratio >= 3.0) {
            accentInt = swatch.rgb
            break
        }
    }
    if (accentInt == null) {
        accentInt = if (isBgDark) 0xFFFFFFFF.toInt() else 0xFF1C1C1E.toInt()
    }
    val accent = Color(accentInt)
    return AdaptivePlayerColors(
        background = background,
        accent = accent,
        dim = accent.copy(alpha = 0.58f),
        faint = accent.copy(alpha = 0.34f),
    )
}

@Composable
fun rememberAdaptivePlayerColors(
    bitmap: Bitmap?,
    enabled: Boolean,
    fallbackDark: Boolean = true,
): AdaptivePlayerColors {
    val fallback = if (fallbackDark) AdaptivePlayerColors.FallbackDark else AdaptivePlayerColors.FallbackLight
    val target = if (!enabled || bitmap == null || bitmap.isRecycled) {
        fallback
    } else {
        val extracted = produceState<AdaptivePlayerColors>(initialValue = fallback, key1 = bitmap) {
            value = withContext(Dispatchers.Default) {
                runCatching { bitmap.extractAdaptivePlayerColors() }.getOrDefault(fallback)
            }
        }.value
        extracted
    }
    val bg by animateColorAsState(target.background, tween(350, easing = FastOutSlowInEasing), label = "adaptive-bg")
    val accent by animateColorAsState(target.accent, tween(350, easing = FastOutSlowInEasing), label = "adaptive-accent")
    val dim by animateColorAsState(target.dim, tween(350, easing = FastOutSlowInEasing), label = "adaptive-dim")
    val faint by animateColorAsState(target.faint, tween(350, easing = FastOutSlowInEasing), label = "adaptive-faint")
    return AdaptivePlayerColors(background = bg, accent = accent, dim = dim, faint = faint)
}

fun Color.darkenForGradient(factor: Float = 0.72f): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
