package com.exodidio.tune.library

import android.content.ContentResolver
import android.net.Uri
import com.exodidio.tune.sync.LibraryTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

/** Extended M3U header directive. */
private const val ExtM3u = "#EXTM3U"
/** Extended M3U track info directive: #EXTINF:<duration>,<title> */
private const val ExtInf = "#EXTINF:"

/**
 * Parses an M3U (or M3U8) playlist file, returning the list of file paths / URIs
 * it references, in order. Comment/directive lines (starting with `#`) are
 * skipped; blank lines are skipped. Works for both basic M3U (one path per
 * line) and extended M3U (#EXTINF lines followed by a path).
 */
fun parseM3u(content: String): List<String> {
    val paths = mutableListOf<String>()
    for (line in content.lines()) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed.startsWith("#")) continue
        paths.add(trimmed)
    }
    return paths
}

/**
 * Builds extended M3U content from title/path pairs. Each entry gets an
 * #EXTINF line (when a title is present) followed by its path.
 */
fun m3uContent(entries: List<Pair<String, String>>): String = buildString {
    appendLine(ExtM3u)
    for ((title, path) in entries) {
        if (title.isNotBlank()) {
            appendLine("$ExtInf:-1,$title")
        }
        appendLine(path)
    }
}

/** Reads text content from a content URI on the IO dispatcher. */
suspend fun readTextFromUri(contentResolver: ContentResolver, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: null
    }

/** Writes text content to a content URI on the IO dispatcher. Returns true on success. */
suspend fun writeTextToUri(contentResolver: ContentResolver, uri: Uri, text: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use { writer -> writer.write(text) }
            }
            true
        }.getOrNull() == true
    }

/**
 * Matches M3U paths to library track IDs.
 *
 * Resolution order per path:
 * 1. Exact match against `LibraryTrack.audioPath` (handles content:// URIs).
 * 2. Filename (last path segment) match against `LibraryTrack.audioPath`.
 *
 * Duplicate M3U entries pointing to the same track are collapsed, keeping the
 * first occurrence and preserving order.
 */
fun matchTracks(m3uPaths: List<String>, tracks: List<LibraryTrack>): List<String> {
    val byPath = tracks.associateBy({ it.audioPath.orEmpty() }, { it.id })
    val byFileName =
        tracks.associateBy({ it.audioPath?.substringAfterLast('/')?.substringAfterLast('\\') ?: it.audioPath.orEmpty() }, { it.id })
    val matched = mutableListOf<String>()
    val seen = mutableSetOf<String>()
    for (path in m3uPaths) {
        var id: String? = byPath[path]
        if (id == null) {
            id = byFileName[path.substringAfterLast('/').substringAfterLast('\\')]
        }
        if (id != null && id !in seen) {
            seen.add(id)
            matched.add(id)
        }
    }
    return matched
}
