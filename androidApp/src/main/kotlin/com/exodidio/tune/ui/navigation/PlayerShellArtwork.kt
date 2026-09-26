package com.exodidio.tune.ui.navigation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.exodidio.tune.player.ArtworkCrossfadeTransition
import com.exodidio.tune.ui.components.ArtworkThumbnailCache
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.theme.LocalTuneColors
import java.io.File

private val FullScreenArtworkShape = RoundedCornerShape(16.dp)

internal data class FullScreenArtwork(
    val image: androidx.compose.ui.graphics.ImageBitmap,
    val dominant: Color,
)

@Composable
internal fun FullScreenPlayerBackground(
    artwork: FullScreenArtwork?,
    outgoingArtwork: FullScreenArtwork?,
    incomingArtwork: FullScreenArtwork?,
    crossfadeProgress: Float,
    isArtworkCrossfading: Boolean,
    modifier: Modifier,
) {
    val colors = LocalTuneColors.current
    val dominantColor by animateColorAsState(
        targetValue = artwork?.dominant ?: colors.playerBackdrop,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "full-screen-background-colour",
    )
    Box(modifier.background(colors.playerBackdrop)) {
        if (isArtworkCrossfading) {
            PlayerBackgroundGradient(outgoingArtwork?.dominant ?: colors.playerBackdrop, equalPowerOutgoing(crossfadeProgress))
            PlayerBackgroundGradient(incomingArtwork?.dominant ?: colors.playerBackdrop, equalPowerIncoming(crossfadeProgress))
        } else {
            PlayerBackgroundGradient(dominantColor, 1f)
        }
        Box(Modifier.fillMaxSize().background(colors.playerBackdrop.copy(alpha = 0.24f)))
    }
}

@Composable
private fun PlayerBackgroundGradient(dominant: Color, alpha: Float) {
    Box(Modifier.fillMaxSize().alpha(alpha).background(dominant.copy(alpha = 0.66f)))
}

@Composable
internal fun FullScreenPlayerArtwork(
    artwork: FullScreenArtwork?,
    outgoingArtwork: FullScreenArtwork?,
    incomingArtwork: FullScreenArtwork?,
    crossfadeProgress: Float,
    isArtworkCrossfading: Boolean,
    modifier: Modifier,
) {
    val colors = LocalTuneColors.current
    Box(
        modifier.clip(FullScreenArtworkShape).background(colors.glassElevated)
            .border(1.dp, colors.borderGlass, FullScreenArtworkShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isArtworkCrossfading) {
            ArtworkLayer(outgoingArtwork, equalPowerOutgoing(crossfadeProgress))
            ArtworkLayer(incomingArtwork, equalPowerIncoming(crossfadeProgress))
        } else {
            ArtworkLayer(artwork, 1f)
        }
    }
}

@Composable
private fun ArtworkLayer(artwork: FullScreenArtwork?, alpha: Float) {
    val colors = LocalTuneColors.current
    if (artwork != null) Image(artwork.image, null, Modifier.fillMaxSize().alpha(alpha), contentScale = ContentScale.Crop)
    else Box(Modifier.fillMaxSize().alpha(alpha), contentAlignment = Alignment.Center) {
        MaterialSymbol(MaterialSymbols.MusicNote, null, tint = colors.textMuted, size = 64.dp)
    }
}

@Composable
internal fun rememberArtworkCrossfadeProgress(crossfade: ArtworkCrossfadeTransition?): Float {
    var progress by remember(crossfade?.id) { mutableFloatStateOf(if (crossfade == null) 1f else 0f) }
    LaunchedEffect(crossfade?.id) {
        if (crossfade == null) return@LaunchedEffect
        val durationNanos = crossfade.durationMs.coerceAtLeast(1L) * 1_000_000L
        var startedAtNanos = 0L
        while (progress < 1f) withFrameNanos { frameNanos ->
            if (startedAtNanos == 0L) startedAtNanos = frameNanos
            progress = ((frameNanos - startedAtNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
        }
    }
    return progress
}

internal fun equalPowerOutgoing(progress: Float): Float = kotlin.math.cos(progress.coerceIn(0f, 1f) * Math.PI.toFloat() / 2f)
internal fun equalPowerIncoming(progress: Float): Float = kotlin.math.sin(progress.coerceIn(0f, 1f) * Math.PI.toFloat() / 2f)

internal fun fullscreenSampleSize(outWidth: Int, outHeight: Int, targetPx: Int = 1080): Int {
    var sample = 1
    while (outWidth / (sample * 2) >= targetPx && outHeight / (sample * 2) >= targetPx) {
        sample *= 2
    }
    return sample
}

@Composable
internal fun rememberFullscreenArtwork(artworkPath: String?, keepPrevious: Boolean = true): FullScreenArtwork? {
    val context = LocalContext.current
    var artwork by remember(fullscreenArtworkMemoryKey(artworkPath, keepPrevious)) { mutableStateOf<FullScreenArtwork?>(null) }
    LaunchedEffect(artworkPath) {
        if (artworkPath.isNullOrBlank()) {
            artwork = null
            return@LaunchedEffect
        }
        val file = File(if (File(artworkPath).isAbsolute) artworkPath else File(context.filesDir, artworkPath).path)
        if (!file.isFile) { artwork = null; return@LaunchedEffect }
        val key = ArtworkThumbnailCache.cacheKey(file.absolutePath, 1080)
        ArtworkThumbnailCache.get(key)?.let { cached ->
            artwork = FullScreenArtwork(cached, withContext(Dispatchers.Default) { dominantColor(cached.asAndroidBitmap()) })
            return@LaunchedEffect
        }
        artwork = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                val sample = fullscreenSampleSize(bounds.outWidth, bounds.outHeight, 1080)
                val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }) ?: return@runCatching null
                val image = bitmap.asImageBitmap()
                ArtworkThumbnailCache.put(key, image)
                FullScreenArtwork(image, dominantColor(bitmap))
            }.getOrNull()
        }
    }
    return artwork
}

/** A crossfade layer is path-scoped; only the normal player cover may persist across paths. */
internal fun fullscreenArtworkMemoryKey(artworkPath: String?, keepPrevious: Boolean): Any? =
    if (keepPrevious) FullscreenArtworkRetainedKey else artworkPath

private object FullscreenArtworkRetainedKey

private fun dominantColor(bitmap: Bitmap): Color {
    val sample = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
    val pixels = IntArray(24 * 24)
    sample.getPixels(pixels, 0, 24, 0, 0, 24, 24)
    val opaque = pixels.filter { android.graphics.Color.alpha(it) > 32 }
    if (opaque.isEmpty()) return Color.Transparent
    return Color(
        opaque.sumOf { android.graphics.Color.red(it) } / opaque.size,
        opaque.sumOf { android.graphics.Color.green(it) } / opaque.size,
        opaque.sumOf { android.graphics.Color.blue(it) } / opaque.size,
    )
}
