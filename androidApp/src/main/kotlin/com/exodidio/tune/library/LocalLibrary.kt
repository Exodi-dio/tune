package com.exodidio.tune.library

import com.exodidio.tune.sync.LibrarySearchDocumentEntity
import com.exodidio.tune.sync.SyncAssetEntity
import com.exodidio.tune.sync.SyncPlaylistEntity
import com.exodidio.tune.sync.SyncTrackEntity
import com.exodidio.tune.sync.searchDocumentsFor
import java.io.File
import java.security.MessageDigest
import java.time.Instant

internal const val LOCAL_PLAN_ID = "local"

/** One MediaStore audio row, straight from the cursor (all fields nullable). */
internal data class MediaRow(
    val mediaId: Long,
    val uri: String,
    val displayName: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val composer: String?,
    val year: Int?,
    val trackNo: Int?,
    val discNo: Int?,
    val totalTracks: Int?,
    val totalDiscs: Int?,
    val durationMs: Long?,
    val bitrate: Int?,
    val sizeBytes: Long?,
    val dateAddedSec: Long?,
    val mimeType: String?,
    val label: String?,
    val copyright: String?,
)

/** Validated scan result. Blank text stays blank: display layers fall back. */
internal data class LocalTrack(
    val mediaId: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val genre: String?,
    val composer: String?,
    val year: Int?,
    val trackNo: Int,
    val discNo: Int,
    val totalTracks: Int?,
    val totalDiscs: Int?,
    val durationMs: Long,
    val bitrate: Int?,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mimeType: String?,
    val label: String?,
    val copyright: String?,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
)

internal fun MediaRow.toLocalTrack(): LocalTrack? {
    if (mediaId <= 0L || uri.isBlank()) return null
    val fileTitle = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
    return LocalTrack(
        mediaId = mediaId,
        uri = uri,
        title = title?.ifBlank { null } ?: fileTitle.orEmpty(),
        artist = artist.orEmpty(),
        album = album.orEmpty(),
        albumArtist = albumArtist,
        genre = genre,
        composer = composer,
        year = year,
        trackNo = trackNo ?: 0,
        discNo = discNo ?: 0,
        totalTracks = totalTracks,
        totalDiscs = totalDiscs,
        durationMs = durationMs ?: 0L,
        bitrate = bitrate,
        sizeBytes = sizeBytes ?: 0L,
        dateAddedSec = dateAddedSec ?: 0L,
        mimeType = mimeType,
        label = label,
        copyright = copyright,
        sampleRateHz = null,
        bitDepth = null,
    )
}

/** Tags read from the file itself (null = unreadable, keep scan values). */
internal data class RawTags(
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val year: Int?,
    val trackNo: Int?,
    val discNo: Int?,
    val durationMs: Long?,
    val bitrate: Int?,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val hasEmbeddedArt: Boolean,
)

internal fun LocalTrack.applyTags(tags: RawTags?): LocalTrack {
    if (tags == null) return this
    fun pick(scan: String, tag: String?): String = tag?.ifBlank { null } ?: scan
    return copy(
        title = pick(title, tags.title),
        artist = pick(artist, tags.artist),
        album = pick(album, tags.album),
        albumArtist = tags.albumArtist ?: albumArtist,
        genre = tags.genre ?: genre,
        year = tags.year ?: year,
        trackNo = tags.trackNo ?: trackNo,
        discNo = tags.discNo ?: discNo,
        durationMs = tags.durationMs ?: durationMs,
        bitrate = tags.bitrate ?: bitrate,
        sampleRateHz = tags.sampleRateHz ?: sampleRateHz,
        bitDepth = tags.bitDepth ?: bitDepth,
    )
}

internal data class LocalArtwork(
    val relativePath: String,
    val sha256: String,
    val sizeBytes: Long,
    val mime: String,
)

internal data class LocalImport(
    val planId: String,
    val tracks: List<SyncTrackEntity>,
    val audioAssets: List<SyncAssetEntity>,
    val artworkAssets: List<SyncAssetEntity>,
    val searchDocuments: List<LibrarySearchDocumentEntity>,
    val skipped: Int,
) {
    val inserted: Int get() = tracks.size
}

internal fun trackIdFor(mediaId: Long): String = "local-$mediaId"

internal fun stableId(prefix: String, name: String): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(name.lowercase().toByteArray())
    return prefix + digest.joinToString("") { "%02x".format(it) }.take(12)
}

internal fun artistIdFor(name: String): String = stableId("local-artist-", name)

internal fun albumIdFor(name: String): String = stableId("local-album-", name)

internal fun formatFor(mimeType: String?): Pair<String, String> {
    return when (mimeType?.substringAfter('/')?.lowercase().orEmpty()) {
        "flac" -> "flac" to "flac"
        "mpeg", "mp3" -> "mp3" to "mp3"
        "mp4", "x-m4a", "m4a" -> "m4a" to ""
        "aac", "x-aac" -> "aac" to "aac"
        "ogg" -> "ogg" to "vorbis"
        "opus", "x-opus" -> "opus" to "opus"
        "wav", "x-wav" -> "wav" to ""
        "x-aiff", "aiff" -> "aiff" to ""
        "x-ape", "ape" -> "ape" to ""
        "x-wavpack", "wv" -> "wv" to ""
        "x-dsf", "dsf" -> "dsf" to ""
        "x-dff", "dff" -> "dff" to ""
        "wma", "x-ms-wma" -> "wma" to ""
        "3gpp" -> "3gp" to ""
        "amr" -> "amr" to ""
        "mid", "midi", "x-midi" -> "mid" to ""
        else -> "unknown" to ""
    }
}

private fun jsonEscape(value: String): String = buildString(value.length + 2) {
    for (c in value) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }
}

private fun jstr(value: String): String = "\"" + jsonEscape(value) + "\""

internal fun metadataJsonFor(
    track: LocalTrack,
    sampleRateHz: Int?,
    bitDepth: Int?,
    artistArtworkKey: String?,
    albumArtworkKey: String?,
): String {
    val (format, codec) = formatFor(track.mimeType)
    val artistPart = if (track.artist.isBlank()) {
        "[]"
    } else {
        val key = if (artistArtworkKey == null) "" else "," + jstr("artwork_key") + ":" + jstr(artistArtworkKey)
        "[" + "{" + jstr("id") + ":" + jstr(artistIdFor(track.artist)) + "," + jstr("name") + ":" + jstr(track.artist) + key + "}" + "]"
    }
    val albumId = if (track.album.isBlank()) "" else albumIdFor(track.album)
    val albumKey = if (albumArtworkKey == null) "" else "," + jstr("artwork_key") + ":" + jstr(albumArtworkKey)
    val albumYear = track.year?.let { "," + jstr("year") + ":" + it.toString() }.orEmpty()
    val albumCopyright = if (track.copyright == null) "" else "," + jstr("copyright") + ":" + jstr(track.copyright)
    val albumPart = "{" + jstr("id") + ":" + jstr(albumId) + "," + jstr("title") + ":" + jstr(track.album) + albumYear + albumCopyright + albumKey + "}"
    fun optLong(name: String, value: Long?): String = if (value == null) "" else "," + jstr(name) + ":" + value.toString()
    fun optInt(name: String, value: Int?): String = if (value == null) "" else "," + jstr(name) + ":" + value.toString()
    fun optStr(name: String, value: String?): String = if (value == null) "" else "," + jstr(name) + ":" + jstr(value)
    return "{" + jstr("title") + ":" + jstr(track.title) +
        "," + jstr("artists") + ":" + artistPart +
        "," + jstr("album") + ":" + albumPart +
        "," + jstr("format") + ":" + jstr(format) +
        "," + jstr("codec") + ":" + jstr(codec) +
        "," + jstr("bit_depth") + ":" + (bitDepth ?: 0).toString() +
        "," + jstr("sample_rate") + ":" + (sampleRateHz ?: 0).toString() +
        optLong("bitrate", track.bitrate?.toLong()) +
        optLong("file_size", track.sizeBytes) +
        optLong("duration_ms", track.durationMs) +
        optInt("year", track.year) +
        "," + jstr("disc_number") + ":" + track.discNo.toString() +
        "," + jstr("track_number") + ":" + track.trackNo.toString() +
        optInt("total_discs", track.totalDiscs) +
        optInt("total_tracks", track.totalTracks) +
        optStr("raw_artist_names", track.artist.ifBlank { null }) +
        optStr("raw_genre_names", track.genre) +
        optStr("raw_composer_names", track.composer) +
        optStr("label", track.label) +
        optStr("copyright", track.copyright) +
        "}"
}

internal fun buildLocalImport(
    tracks: List<LocalTrack>,
    artwork: Map<String, LocalArtwork> = emptyMap(),
    playlists: List<SyncPlaylistEntity> = emptyList(),
    planId: String = LOCAL_PLAN_ID,
): LocalImport {
    val valid = tracks.filter { it.uri.isNotBlank() && it.mediaId > 0L }
    val artistArt = linkedMapOf<String, String>()
    val albumArt = linkedMapOf<String, String>()
    valid.forEach { track ->
        val assetId = "artwork:" + trackIdFor(track.mediaId)
        if (artwork.containsKey(trackIdFor(track.mediaId))) {
            if (track.artist.isNotBlank()) artistArt.putIfAbsent(artistIdFor(track.artist), assetId)
            if (track.album.isNotBlank()) albumArt.putIfAbsent(albumIdFor(track.album), assetId)
        }
    }
    val entities = valid.mapIndexed { index, track ->
        val id = trackIdFor(track.mediaId)
        val art = artwork[id]
        SyncTrackEntity(
            planId = planId,
            trackId = id,
            title = track.title,
            artists = track.artist,
            album = track.album,
            albumId = if (track.album.isBlank()) "" else albumIdFor(track.album),
            artworkKey = art?.let { "artwork:$id" },
            playCount = 0,
            createdAt = Instant.ofEpochSecond(track.dateAddedSec).toString(),
            discNumber = track.discNo,
            trackNumber = track.trackNo,
            syncOrder = index,
            rawJson = metadataJsonFor(
                track,
                track.sampleRateHz,
                track.bitDepth,
                artistArt[artistIdFor(track.artist)].takeIf { track.artist.isNotBlank() },
                albumArt[albumIdFor(track.album)].takeIf { track.album.isNotBlank() },
            ),
        )
    }
    val audioAssets = entities.map { row ->
        val track = valid.first { trackIdFor(it.mediaId) == row.trackId }
        SyncAssetEntity(
            planId = planId,
            assetId = "audio:" + row.trackId,
            kind = "audio",
            sha256 = "",
            size = track.sizeBytes,
            relativePath = track.uri,
        )
    }
    val artworkAssets = entities.mapNotNull { row ->
        artwork[row.trackId]?.let { art ->
            SyncAssetEntity(
                planId = planId,
                assetId = "artwork:" + row.trackId,
                kind = "artwork",
                sha256 = art.sha256,
                size = art.sizeBytes,
                relativePath = art.relativePath,
            )
        }
    }
    return LocalImport(
        planId = planId,
        tracks = entities,
        audioAssets = audioAssets,
        artworkAssets = artworkAssets,
        searchDocuments = searchDocumentsFor(planId, entities, playlists),
        skipped = tracks.size - valid.size,
    )
}

internal sealed interface AudioTarget {
    data class FileTarget(val file: File) : AudioTarget
    data class ContentTarget(val uri: String) : AudioTarget
}

internal fun resolveAudioTarget(filesDir: File, storedPath: String?): AudioTarget? {
    val path = storedPath?.ifBlank { null } ?: return null
    if (path.startsWith("content://", ignoreCase = true)) return AudioTarget.ContentTarget(path)
    val file = File(path)
    return AudioTarget.FileTarget(if (file.isAbsolute) file else File(filesDir, path))
}

internal fun audioTargetExists(target: AudioTarget, contentExists: (String) -> Boolean): Boolean = when (target) {
    is AudioTarget.FileTarget -> target.file.isFile
    is AudioTarget.ContentTarget -> contentExists(target.uri)
}

internal enum class ScanPermission { Granted, NeedsRationale, Denied }

internal data class LocalImportSummary(val inserted: Int, val skipped: Int)

internal data class LocalScanUiState(
    val permission: ScanPermission? = null,
    val scanning: Boolean = false,
    val lastResult: LocalImportSummary? = null,
    val error: String? = null,
)

internal fun reduceScanPermission(state: LocalScanUiState, granted: Boolean, showRationale: Boolean): LocalScanUiState {
    val permission = when {
        granted -> ScanPermission.Granted
        showRationale -> ScanPermission.NeedsRationale
        else -> ScanPermission.Denied
    }
    return state.copy(permission = permission, error = null)
}
