package com.exodidio.tune.library

import java.net.URLDecoder

/**
 * Scan folder whitelist (Part B).
 *
 * Choice: filter MediaStore results by path prefix in Kotlin after the query
 * materializes [MediaRow]s, rather than constraining the ContentResolver SQL.
 * This matches the existing scanner structure (`queryMediaStore()` builds a
 * `List<MediaRow>`, then `scan()` maps to tracks) and keeps the matching rule
 * pure and unit-testable without OEM SQL quirks. The whitelist itself is read
 * in [com.exodidio.tune.ui.screens.MusicSyncContent] from [ScanFolderPreferences]
 * and passed to `LocalLibraryScanner.scan(whitelist)`.
 */

internal fun normalizeScanFolder(path: String): String {
    var value = path.trim()
    if (value.isEmpty()) return value
    while (value.length > 1 && value.endsWith("/")) value = value.dropLast(1)
    return value
}

/**
 * Best-effort mapping from an `OpenDocumentTree` tree URI string to a
 * filesystem prefix. Handles the primary volume
 * (`content://com.android.externalstorage.documents/tree/primary%3AMusic...`
 * -> `/storage/emulated/0/Music...`) and maps other volumes to
 * `/storage/<volume>/...`. Returns null when the URI cannot be mapped.
 */
internal fun scanPathForTreeUriString(uriString: String): String? {
    return try {
        val treeIndex = uriString.indexOf("/tree/")
        if (treeIndex < 0) return null
        var doc = uriString.substring(treeIndex + "/tree/".length)
        val slash = doc.indexOf('/')
        if (slash >= 0) doc = doc.substring(0, slash)
        if (doc.isBlank()) return null
        doc = try {
            URLDecoder.decode(doc, "UTF-8")
        } catch (_: Exception) {
            doc
        }
        val colon = doc.indexOf(':')
        if (colon < 0) return null
        val volume = doc.substring(0, colon).trim()
        val path = doc.substring(colon + 1).trim().trim('/')
        if (volume.isBlank()) return null
        if (volume.equals("primary", ignoreCase = true)) {
            if (path.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$path"
        } else {
            if (path.isBlank()) "/storage/$volume" else "/storage/$volume/$path"
        }
    } catch (_: Exception) {
        null
    }
}

internal fun scanFilePrefixes(whitelist: Set<String>): Set<String> {
    return whitelist.mapNotNull { entry ->
        val trimmed = entry.trim()
        if (trimmed.isBlank()) {
            null
        } else if (trimmed.startsWith("content://", ignoreCase = true)) {
            scanPathForTreeUriString(trimmed)
        } else if (trimmed.startsWith("file://", ignoreCase = true)) {
            val stripped = trimmed.substring("file://".length)
            normalizeScanFolder(if (stripped.startsWith("/")) stripped else "/$stripped")
                .takeIf { it.isNotBlank() }
        } else {
            normalizeScanFolder(trimmed).takeIf { it.isNotBlank() }
        }
    }.toSet()
}

/**
 * Returns true when [filePath] is inside [whitelist]. Empty whitelist means
 * "scan everything" (current behavior). `content://` or unknown paths cannot
 * be verified against filesystem prefixes, so they are excluded when the
 * whitelist is non-empty. When the whitelist contains no convertible file
 * prefixes (e.g. only unmappable tree URIs), fall back to allowing everything
 * rather than excluding the whole library.
 */
internal fun isScanPathAllowed(filePath: String?, whitelist: Set<String>): Boolean {
    if (whitelist.isEmpty()) return true
    val prefixes = scanFilePrefixes(whitelist)
    if (prefixes.isEmpty()) return true
    val raw = filePath?.trim().takeIf { !it.isNullOrBlank() } ?: return false
    if (raw.startsWith("content://", ignoreCase = true)) return false
    val path = if (raw.startsWith("file://", ignoreCase = true)) {
        val stripped = raw.substring("file://".length)
        if (stripped.isBlank()) return false
        if (stripped.startsWith("/")) stripped else "/$stripped"
    } else {
        raw
    }
    val normalized = normalizeScanFolder(path)
    if (normalized.isBlank()) return false
    for (prefix in prefixes) {
        if (normalized == prefix) return true
        if (normalized.startsWith(prefix + "/")) return true
    }
    return false
}

internal fun filterScanPaths(paths: List<String>, whitelist: Set<String>): List<String> {
    if (whitelist.isEmpty()) return paths
    return paths.filter { isScanPathAllowed(it, whitelist) }
}

internal fun filterAllowedMediaRows(rows: List<MediaRow>, whitelist: Set<String>): List<MediaRow> {
    if (whitelist.isEmpty()) return rows
    return rows.filter { isScanPathAllowed(it.dataPath, whitelist) }
}
