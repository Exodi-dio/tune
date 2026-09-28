package com.exodidio.tune.library

import java.net.URLDecoder

/**
 * Scan folder whitelist (Part B, hardened round-1 review fixes).
 *
 * Choice: filter MediaStore results by path prefix in Kotlin after the query
 * materializes [MediaRow]s, rather than constraining the ContentResolver SQL.
 * This matches the existing scanner structure (`queryMediaStore()` builds a
 * `List<MediaRow>`, then `scan()` maps to tracks) and keeps the matching rule
 * pure and unit-testable without OEM SQL quirks. The whitelist itself is read
 * in [com.exodidio.tune.ui.screens.MusicSyncContent] from [ScanFolderPreferences]
 * and passed to `LocalLibraryScanner.scan(whitelist)`.
 *
 * Fail direction (deliberate, fail-closed): an empty whitelist means "scan
 * everything" (current behavior). A NON-empty whitelist is restrictive: paths
 * that cannot be verified (`content://`, null/blank, unresolvable rows) are
 * DENIED, and a non-empty whitelist that yields ZERO convertible file prefixes
 * (e.g. only unmappable tree URIs) denies everything rather than silently
 * scanning the whole library (F1). This keeps both fail directions consistent:
 * constrained + unverifiable = excluded.
 *
 * Scoped-storage note (F3): `MediaStore.Audio.Media.DATA` is null on Android
 * 10+ for most rows (verified against `MediaStore.MediaColumns`: `DATA` is the
 * absolute path, `RELATIVE_PATH` + `DISPLAY_NAME` are the supported location
 * columns). When `DATA` is null, [scanPathForRow] falls back to composing
 * `/storage/emulated/0/<RELATIVE_PATH>/<DISPLAY_NAME>`; rows with neither are
 * still denied when constrained (fail-closed, documented — not silent).
 */

/**
 * Percent-decodes [value] while preserving a literal `+` (F2).
 * `java.net.URLDecoder` maps `+` to space (form encoding), which corrupts
 * folders like `My+Music`. Pre-escaping `+` to `%2B` gives `Uri.decode()`
 * semantics using JVM-only APIs (this file stays Android-free for unit tests).
 * Inputs without `%` are returned as-is. Malformed sequences fall back to the
 * raw value instead of throwing.
 */
internal fun decodeScanPathSegment(value: String): String {
    if (!value.contains('%')) return value
    return try {
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    } catch (_: Exception) {
        value
    }
}

/**
 * Single normalization helper (F4): trim, percent-decode file paths
 * (`%20` in `file://` entries), collapse duplicate `/`, map known storage
 * aliases (`/sdcard`, `/mnt/sdcard`, `/storage/sdcard0` and their children)
 * to `/storage/emulated/0`, strip trailing `/` (except root). Comparison
 * itself is case-insensitive (see [isScanPathAllowed]); this helper preserves
 * case and only canonicalizes shape.
 */
internal fun canonicalizeScanPath(raw: String): String {
    var value = raw.trim()
    if (value.isEmpty()) return value
    value = decodeScanPathSegment(value)
    value = value.replace(Regex("/+"), "/")
    val lower = value.lowercase()
    value = when {
        lower == "/sdcard" || lower == "/mnt/sdcard" || lower == "/storage/sdcard0" ->
            "/storage/emulated/0"
        lower.startsWith("/sdcard/") ->
            "/storage/emulated/0" + value.substring("/sdcard".length)
        lower.startsWith("/mnt/sdcard/") ->
            "/storage/emulated/0" + value.substring("/mnt/sdcard".length)
        lower.startsWith("/storage/sdcard0/") ->
            "/storage/emulated/0" + value.substring("/storage/sdcard0".length)
        else -> value
    }
    while (value.length > 1 && value.endsWith("/")) value = value.dropLast(1)
    return value
}

internal fun normalizeScanFolder(path: String): String = canonicalizeScanPath(path)

/**
 * Best-effort mapping from an `OpenDocumentTree` tree URI string to a
 * filesystem prefix. Handles the primary volume
 * (`content://com.android.externalstorage.documents/tree/primary%3AMusic...`
 * -> `/storage/emulated/0/Music...`) and maps other volumes to
 * `/storage/<volume>/...`. Returns null when the URI cannot be mapped.
 * Decoding preserves literal `+` (F2); the result is canonicalized (F4).
 */
internal fun scanPathForTreeUriString(uriString: String): String? {
    return try {
        val treeIndex = uriString.indexOf("/tree/")
        if (treeIndex < 0) return null
        var doc = uriString.substring(treeIndex + "/tree/".length)
        val slash = doc.indexOf('/')
        if (slash >= 0) doc = doc.substring(0, slash)
        if (doc.isBlank()) return null
        doc = decodeScanPathSegment(doc)
        val colon = doc.indexOf(':')
        if (colon < 0) return null
        val volume = doc.substring(0, colon).trim()
        val path = doc.substring(colon + 1).trim().trim('/')
        if (volume.isBlank()) return null
        val mapped = if (volume.equals("primary", ignoreCase = true)) {
            if (path.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$path"
        } else {
            if (path.isBlank()) "/storage/$volume" else "/storage/$volume/$path"
        }
        canonicalizeScanPath(mapped)
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
            canonicalizeScanPath(if (stripped.startsWith("/")) stripped else "/$stripped")
                .takeIf { it.isNotBlank() }
        } else {
            canonicalizeScanPath(trimmed).takeIf { it.isNotBlank() }
        }
    }.toSet()
}

/**
 * Fallback scan path when `MediaStore.Audio.Media.DATA` is null (F3, scoped
 * storage): composes `/storage/emulated/0/<RELATIVE_PATH>/<DISPLAY_NAME>`.
 * `RELATIVE_PATH` values look like `Music/` or `Music/Album/`; the result is
 * canonicalized. Returns null when [displayName] is missing (nothing to anchor
 * the file name to) — callers deny such rows when constrained.
 */
internal fun fallbackScanPathFor(relativePath: String?, displayName: String?): String? {
    val name = displayName?.trim().takeIf { !it.isNullOrBlank() } ?: return null
    val rel = relativePath?.trim()?.trim('/')?.takeIf { it.isNotBlank() }
    val joined = if (rel == null) name.trim('/') else rel + "/" + name.trim('/')
    if (joined.isBlank()) return null
    return canonicalizeScanPath("/storage/emulated/0/$joined").takeIf { it.isNotBlank() }
}

/** Effective filesystem path for [row]: `DATA` first, else the F3 fallback, else null. */
internal fun scanPathForRow(row: MediaRow): String? {
    row.dataPath?.trim().takeIf { !it.isNullOrBlank() }?.let { return it }
    return fallbackScanPathFor(row.relativePath, row.displayName)
}

/**
 * Returns true when [filePath] is inside [whitelist]. Empty whitelist means
 * "scan everything" (current behavior). Matching is trim- and
 * case-insensitive with alias/`//`/percent normalization (F4/F6). Fail-closed
 * (F1): a non-empty whitelist with zero convertible prefixes denies everything;
 * `content://`/null/blank/unresolvable paths are denied when constrained.
 */
internal fun isScanPathAllowed(filePath: String?, whitelist: Set<String>): Boolean {
    if (whitelist.isEmpty()) return true
    val prefixes = scanFilePrefixes(whitelist).map { it.lowercase() }.toSet()
    if (prefixes.isEmpty()) return false
    val raw = filePath?.trim().takeIf { !it.isNullOrBlank() } ?: return false
    if (raw.startsWith("content://", ignoreCase = true)) return false
    val stripped = if (raw.startsWith("file://", ignoreCase = true)) {
        raw.substring("file://".length).takeIf { it.isNotBlank() } ?: return false
    } else {
        raw
    }
    val normalized = canonicalizeScanPath(if (stripped.startsWith("/")) stripped else "/$stripped")
    if (normalized.isBlank()) return false
    val lower = normalized.lowercase()
    for (prefix in prefixes) {
        if (lower == prefix) return true
        if (lower.startsWith("$prefix/")) return true
    }
    return false
}

internal fun filterScanPaths(paths: List<String>, whitelist: Set<String>): List<String> {
    if (whitelist.isEmpty()) return paths
    return paths.filter { isScanPathAllowed(it, whitelist) }
}

internal fun filterAllowedMediaRows(rows: List<MediaRow>, whitelist: Set<String>): List<MediaRow> {
    if (whitelist.isEmpty()) return rows
    return rows.filter { isScanPathAllowed(scanPathForRow(it), whitelist) }
}
