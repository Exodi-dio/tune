package com.exodidio.tune.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.exodidio.tune.sync.AndroidLibrarySyncStore
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class LocalLibraryScanner(
    private val context: Context,
    private val store: AndroidLibrarySyncStore,
) {
    val state = MutableStateFlow(LocalScanUiState())

    fun refreshPermission(granted: Boolean, showRationale: Boolean) {
        state.value = reduceScanPermission(state.value, granted, showRationale)
    }

    suspend fun scan(): LocalImportSummary = withContext(Dispatchers.IO) {
        state.value = state.value.copy(scanning = true, error = null)
        try {
            val rows = queryMediaStore()
            val tracks = mutableListOf<LocalTrack>()
            var skipped = 0
            val artwork = mutableMapOf<String, LocalArtwork>()
            var mmr: MediaMetadataRetriever? = null
            fun retriever(): MediaMetadataRetriever =
                mmr ?: MediaMetadataRetriever().also { mmr = it }
            fun resetRetriever() {
                runCatching { mmr?.release() }
                mmr = null
            }
            try {
                rows.forEach { row ->
                    val base = row.toLocalTrack()
                    if (base == null) {
                        skipped++
                        return@forEach
                    }
                    val tagged = try {
                        applyRetrieverTags(retriever(), base)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        resetRetriever()
                        base
                    }
                    try {
                        extractArtwork(retriever(), tagged)?.let { artwork[trackIdFor(tagged.mediaId)] = it }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        resetRetriever()
                    }
                    tracks.add(tagged)
                }
            } finally {
                runCatching { mmr?.release() }
            }
            val playlists = store.playlistEntities(LOCAL_PLAN_ID)
            val import = buildLocalImport(tracks, artwork, playlists)
            store.commitLocalLibrary(import)
            val summary = LocalImportSummary(inserted = import.tracks.size, skipped = skipped + import.skipped)
            state.value = state.value.copy(scanning = false, lastResult = summary)
            summary
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.value = state.value.copy(scanning = false, error = e.message)
            throw e
        }
    }

    private fun queryMediaStore(): List<MediaRow> {
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val rows = mutableListOf<MediaRow>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            fun idx(name: String) = cursor.getColumnIndex(name)
            fun str(name: String): String? = idx(name).takeIf { it >= 0 }?.let { cursor.getString(it) }
            fun int(name: String): Int? = idx(name).takeIf { it >= 0 }?.let { cursor.getInt(it) }
            fun long(name: String): Long? = idx(name).takeIf { it >= 0 }?.let { cursor.getLong(it) }
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val rawTrack = int(MediaStore.Audio.Media.TRACK) ?: 0
                rows.add(
                    MediaRow(
                        mediaId = id,
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        displayName = str(MediaStore.Audio.Media.DISPLAY_NAME),
                        title = str(MediaStore.Audio.Media.TITLE),
                        artist = str(MediaStore.Audio.Media.ARTIST)?.takeUnless { it == "<unknown>" },
                        album = str(MediaStore.Audio.Media.ALBUM)?.takeUnless { it == "<unknown>" },
                        albumArtist = null,
                        genre = null,
                        composer = null,
                        year = int(MediaStore.Audio.Media.YEAR)?.takeIf { it > 0 },
                        trackNo = (if (rawTrack > 1000) rawTrack % 1000 else rawTrack).takeIf { it > 0 },
                        discNo = int(MediaStore.Audio.Media.DISC_NUMBER)?.takeIf { it > 0 }
                            ?: (rawTrack / 1000).takeIf { rawTrack > 1000 },
                        totalTracks = null,
                        totalDiscs = null,
                        durationMs = long(MediaStore.Audio.Media.DURATION),
                        bitrate = null,
                        sizeBytes = long(MediaStore.Audio.Media.SIZE),
                        dateAddedSec = long(MediaStore.Audio.Media.DATE_ADDED),
                        mimeType = str(MediaStore.Audio.Media.MIME_TYPE),
                        label = null,
                        copyright = null,
                    ),
                )
            }
        }
        return rows
    }

    private fun applyRetrieverTags(mmr: MediaMetadataRetriever, base: LocalTrack): LocalTrack {
        mmr.setDataSource(context, Uri.parse(base.uri))
        fun s(key: Int) = mmr.extractMetadata(key)
        val trackParts = s(MediaMetadataRetriever.METADATA_KEY_TRACK_NUMBER)?.split('/')?.mapNotNull { it.toIntOrNull() }
        val year = s(MediaMetadataRetriever.METADATA_KEY_YEAR)?.take(4)?.toIntOrNull()
            ?: s(MediaMetadataRetriever.METADATA_KEY_DATE)?.take(4)?.toIntOrNull()
        return base.applyTags(
            RawTags(
                title = s(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = s(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = s(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                albumArtist = s(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                genre = s(MediaMetadataRetriever.METADATA_KEY_GENRE),
                year = year,
                trackNo = trackParts?.getOrNull(0),
                discNo = s(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.split('/')?.firstOrNull()?.toIntOrNull(),
                durationMs = s(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                bitrate = s(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull(),
                sampleRateHz = null,
                bitDepth = null,
                hasEmbeddedArt = false,
            ),
        )
    }

    private fun extractArtwork(mmr: MediaMetadataRetriever, track: LocalTrack): LocalArtwork? {
        val bytes = mmr.embeddedPicture ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val out = java.io.ByteArrayOutputStream().use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)) return null
            stream.toByteArray()
        }
        if (!bitmap.isRecycled) bitmap.recycle()
        val hash = MessageDigest.getInstance("SHA-256").digest(out).joinToString("") { "%02x".format(it) }
        val relativePath = "artwork-local/${trackIdFor(track.mediaId)}.jpg"
        val target = File(context.filesDir, relativePath)
        target.parentFile?.mkdirs()
        target.outputStream().use { it.write(out) }
        return LocalArtwork(relativePath = relativePath, sha256 = hash, sizeBytes = out.size.toLong(), mime = "image/jpeg")
    }
}

fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}
