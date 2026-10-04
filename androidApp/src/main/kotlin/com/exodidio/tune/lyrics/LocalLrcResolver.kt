package com.exodidio.tune.lyrics

import com.exodidio.tune.ui.navigation.parsePlayerLyrics
import java.io.File

/**
 * Local sibling `.lrc`/`.ttml` resolution (Part A, v1.2.0 adds TTML).
 *
 * Order: `<trackbasename>.lrc` in the track's directory, then
 * `<artist> - <title>.lrc` in the same directory. The first candidate
 * that exists and parses wins. Parsing reuses the existing player LRC
 * parser ([parsePlayerLyrics]); there is no new parser.
 *
 * Track paths are stored as `content://` MediaStore URIs for local tracks
 * (see `LocalTrack.uri` / `LibraryTrack.audioPath` via
 * `resolveAudioTarget`). A `content://` path cannot resolve filesystem
 * siblings, so it returns null and the caller falls back to online fetch.
 * Plain file paths (including `file://` URIs) resolve siblings via [File].
 */
internal fun localLrcCandidates(trackFilePath: String, artist: String, title: String): List<String> {
    val file = File(trackFilePath)
    val base = file.name.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: return emptyList()
    val dir = file.parent.orEmpty()
    fun join(name: String): String = if (dir.isBlank()) name else dir + File.separator + name
    val candidates = mutableListOf(join("$base.lrc"))
    val safeArtist = artist.trim()
    val safeTitle = title.trim()
    if (safeArtist.isNotBlank() && safeTitle.isNotBlank()) {
        val second = "$safeArtist - $safeTitle".replace('/', '_').replace('\u0000', '_')
        candidates += join("$second.lrc")
    }
    // TTML siblings (v1.2.0): same basenames with .ttml extension, after .lrc.
    candidates += candidates.filter { it.endsWith(".lrc") }.map { it.dropLast(4) + ".ttml" }
    return candidates.distinct()
}

internal fun isParsableLrcContent(content: String): Boolean {
    if (content.isBlank()) return false
    return try {
        parsePlayerLyrics(content).isNotEmpty()
    } catch (_: Exception) {
        false
    }
}

internal fun selectLocalLrcContent(candidates: List<String>, read: (String) -> String?): String? {
    for (path in candidates) {
        val content = try {
            read(path)
        } catch (_: Exception) {
            null
        } ?: continue
        if (isParsableLrcContent(content)) return content
    }
    return null
}

/**
 * Resolves sibling `.lrc` content for [audioPath]. Returns null when there is
 * no file path (null/blank/`content://`) or when no candidate exists/parses,
 * in which case the caller falls back to online fetch as today.
 *
 * @param read seam for filesystem access; production passes a `File` reader,
 * unit tests mock it (missing file -> null -> online path).
 */
internal fun resolveLocalLrcContent(
    audioPath: String?,
    artist: String,
    title: String,
    read: (String) -> String?,
): String? {
    val raw = audioPath?.trim().takeIf { !it.isNullOrBlank() } ?: return null
    if (raw.startsWith("content://", ignoreCase = true)) return null
    val filePath = if (raw.startsWith("file://", ignoreCase = true)) {
        raw.substring("file://".length).takeIf { it.isNotBlank() }?.let {
            if (it.startsWith("/")) it else "/$it"
        } ?: return null
    } else {
        raw
    }
    if (filePath.isBlank()) return null
    val candidates = localLrcCandidates(filePath, artist, title)
    if (candidates.isEmpty()) return null
    return selectLocalLrcContent(candidates, read)
}

internal fun mediaIdForLocalTrackId(trackId: String): Long? =
    if (trackId.startsWith("local-")) trackId.removePrefix("local-").toLongOrNull() else null
