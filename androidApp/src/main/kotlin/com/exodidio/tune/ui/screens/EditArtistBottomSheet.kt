package com.exodidio.tune.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.ArtworkThumbnailCache
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.TuneBottomSheet
import com.exodidio.tune.ui.components.rememberArtworkThumbnail
import com.exodidio.tune.ui.theme.LocalTuneColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun EditArtistBottomSheet(
    artworkPath: String?,
    onDismiss: () -> Unit,
    onSave: (Uri?, Boolean) -> Unit,
) {
    val colors = LocalTuneColors.current
    val context = LocalContext.current
    var artworkUri by remember { mutableStateOf<Uri?>(null) }
    var clearArtwork by remember { mutableStateOf(false) }
    val canClearArtwork = artworkPath != null
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> artworkUri = uri; if (uri != null) clearArtwork = false }
    // After: sampled IO decode through the shared byte-budgeted cache.
    var artwork by remember(artworkUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(artworkUri) {
        val uri = artworkUri
        if (uri == null) { artwork = null; return@LaunchedEffect }
        val key = ArtworkThumbnailCache.cacheKey(uri.toString(), 336)
        ArtworkThumbnailCache.get(key)?.let { artwork = it; return@LaunchedEffect }
        if (!ArtworkThumbnailCache.claimInFlight(key)) return@LaunchedEffect
        var loaded: ImageBitmap? = null
        try {
            loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    val sample = artistPickerSampleSize(bounds.outWidth, bounds.outHeight, 336)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.RGB_565
                        })
                    }?.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) { ArtworkThumbnailCache.put(key, loaded); artwork = loaded }
        } finally {
            if (loaded == null) ArtworkThumbnailCache.releaseInFlight(key)
        }
    }
    val existingArtwork = rememberArtworkThumbnail(artworkPath, targetPx = 336)
    TuneBottomSheet(
        title = { Text(stringResource(R.string.artist_edit_title), style = MaterialTheme.typography.titleMedium, color = colors.textMain) },
        onDismiss = onDismiss,
        leadingAction = { dismiss ->
            ArtistEditorSheetIconButton(MaterialSymbols.Close, stringResource(R.string.cancel), dismiss)
        },
        trailingAction = {
            ArtistEditorSheetIconButton(
                MaterialSymbols.Check, stringResource(R.string.save),
                { onSave(artworkUri, clearArtwork) },
                modifier = Modifier.testTag("artist-save-button"),
                primary = true,
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(168.dp).clip(RoundedCornerShape(18.dp)).background(colors.glassElevated)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(18.dp)).testTag("artist-artwork-picker")
                    .clickable(onClick = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }),
                contentAlignment = Alignment.Center,
            ) {
                val pickedArtwork = artwork
                if (pickedArtwork != null) Image(pickedArtwork, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                else if (!clearArtwork && existingArtwork != null) Image(existingArtwork, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArtistEditorSheetIconButton(
                        MaterialSymbols.Image,
                        stringResource(R.string.artist_choose_artwork),
                        { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        primary = true,
                    )
                    if ((canClearArtwork || artworkUri != null) && !clearArtwork) {
                        ArtistEditorSheetIconButton(
                            MaterialSymbols.Close,
                            stringResource(R.string.artist_clear_artwork),
                            { artworkUri = null; clearArtwork = canClearArtwork },
                        )
                    }
                }
            }
        }
    }
}

internal fun artistPickerSampleSize(outWidth: Int, outHeight: Int, targetPx: Int): Int {
    var sampleSize = 1
    while (outWidth / (sampleSize * 2) >= targetPx && outHeight / (sampleSize * 2) >= targetPx) {
        sampleSize *= 2
    }
    return sampleSize
}

@Composable
private fun ArtistEditorSheetIconButton(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val colors = LocalTuneColors.current
    Box(
        modifier = modifier.size(48.dp).clip(RoundedCornerShape(24.dp))
            .background(if (primary) if (enabled) colors.primary else colors.buttonSecondary else colors.glassElevated)
            .border(1.dp, if (primary && enabled) colors.primary else colors.borderGlass, RoundedCornerShape(24.dp))
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol(
            symbol,
            null,
            size = 22.dp,
            tint = if (primary && enabled) colors.onPrimary else if (enabled) colors.textMain else colors.textMuted,
        )
    }
}
