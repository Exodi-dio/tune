package com.exodidio.tune.ui.components

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.exodidio.tune.R
import com.exodidio.tune.ui.theme.LocalTuneColors

private const val ArtworkCacheFraction = 8

internal fun artworkCacheMaxBytes(memoryClassMb: Int): Int =
    memoryClassMb * 1024 * 1024 / ArtworkCacheFraction

internal fun artworkCacheKey(absolutePath: String, targetPx: Int): String =
    "$absolutePath:$targetPx"

internal object ArtworkThumbnailCache {
    @Volatile private var backing: LruCache<String, ImageBitmap>? = null
    private val inFlight = ConcurrentHashMap<String, Unit>()

    fun init(memoryClassMb: Int) {
        if (backing == null) {
            synchronized(this) {
                if (backing == null) {
                    val maxBytes = artworkCacheMaxBytes(memoryClassMb)
                    backing = object : LruCache<String, ImageBitmap>(maxBytes) {
                        override fun sizeOf(key: String, value: ImageBitmap): Int {
                            return try {
                                value.asAndroidBitmap().allocationByteCount.coerceAtLeast(1)
                            } catch (_: Throwable) {
                                4 * 1024
                            }
                        }
                    }
                }
            }
        }
    }

    fun cacheKey(path: String, targetPx: Int): String = artworkCacheKey(path, targetPx)

    fun get(key: String): ImageBitmap? = try {
        backing?.get(key)
    } catch (_: Throwable) {
        null
    }

    fun put(key: String, value: ImageBitmap) {
        try {
            backing?.put(key, value)
        } catch (_: Throwable) {
            Unit
        } finally {
            inFlight.remove(key)
        }
    }

    fun claimInFlight(key: String): Boolean = inFlight.putIfAbsent(key, Unit) == null
    fun releaseInFlight(key: String) { inFlight.remove(key) }
}

// After: byte-budgeted cache initialised once from ActivityManager.memoryClass.
private fun ensureArtworkCache(context: Context) {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    ArtworkThumbnailCache.init(manager.memoryClass)
}

@Composable
internal fun rememberArtworkThumbnail(
    artworkPath: String?,
    targetPx: Int = 120,
): ImageBitmap? {
    if (artworkPath.isNullOrBlank()) return null
    val context = LocalContext.current
    ensureArtworkCache(context)
    val absolutePath = remember(artworkPath, context) {
        val file = File(artworkPath)
        if (file.isAbsolute) file.absolutePath else File(context.filesDir, artworkPath).absolutePath
    }
    val cacheKey = remember(absolutePath, targetPx) { ArtworkThumbnailCache.cacheKey(absolutePath, targetPx) }
    var bitmap by remember(cacheKey) { mutableStateOf(ArtworkThumbnailCache.get(cacheKey)) }
    LaunchedEffect(cacheKey) {
        if (bitmap != null) return@LaunchedEffect
        if (!ArtworkThumbnailCache.claimInFlight(cacheKey)) return@LaunchedEffect
        var loaded: ImageBitmap? = null
        try {
            loaded = withContext(Dispatchers.IO) {
                val file = File(absolutePath)
                if (!file.isFile) return@withContext null
                runCatching {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    var sampleSize = 1
                    while (boundsOptions.outWidth / (sampleSize * 2) >= targetPx &&
                        boundsOptions.outHeight / (sampleSize * 2) >= targetPx
                    ) {
                        sampleSize *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeFile(file.absolutePath, decodeOptions)?.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) {
                // put happens on the IO result, never on the main dispatcher.
                ArtworkThumbnailCache.put(cacheKey, loaded)
                bitmap = loaded
            }
        } finally {
            if (loaded == null) ArtworkThumbnailCache.releaseInFlight(cacheKey)
        }
    }
    return bitmap
}

@Composable
fun TrackRow(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    artworkPath: String? = null,
    contentPadding: PaddingValues = PaddingValues(start = 24.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
    onClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = LocalTuneColors.current
    val bitmap = rememberArtworkThumbnail(artworkPath)
    val clickModifier = remember(onClick, onLongClick) {
        if (onClick != null || onLongClick != null) {
            Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
        } else {
            Modifier
        }
    }
    val handleMoreClick = remember(onMoreClick) {
        { onMoreClick?.invoke(); Unit }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Artwork
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.glassElevated)
                .border(1.dp, colors.borderGlass, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                MaterialSymbol(
                    symbol = MaterialSymbols.MusicNote,
                    contentDescription = null,
                    size = 22.dp,
                    tint = colors.textMuted,
                )
            }
        }

        // 2. Title & Artist Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        trailingContent?.invoke(this) ?: run {
            IconButton(
                onClick = handleMoreClick,
                modifier = Modifier.size(48.dp),
            ) {
                MaterialSymbol(
                    symbol = MaterialSymbols.MoreVert,
                    contentDescription = stringResource(R.string.track_row_more_options),
                    size = 20.dp,
                    tint = colors.textMuted,
                )
            }
        }
    }
}

