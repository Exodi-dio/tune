package com.exodidio.tune.library

import com.exodidio.tune.sync.LibraryTrack

/**
 * Delete-from-device helpers (pure, unit-tested).
 *
 * File deletion itself always goes through the system [android.provider.MediaStore]
 * delete consent dialog (`MediaStore.createDeleteRequest`); these helpers only
 * compute *which* tracks participate. They never touch files or the store.
 */

/**
 * Content URIs eligible for a [android.provider.MediaStore] delete request.
 * Only `content://` audio paths qualify; plain file paths and tracks without a
 * path are excluded (no broad storage permission, no direct `File.delete`).
 */
fun deviceDeleteUris(tracks: List<LibraryTrack>): List<String> =
    tracks.mapNotNull { track ->
        track.audioPath?.takeIf { it.startsWith("content://", ignoreCase = true) }
    }.distinct()

/**
 * Library ids to drop after the system delete consent dialog resolves.
 * Consent granted → remove library entries for the deleted tracks.
 * Consent denied/cancelled → empty (nothing happens, clean state).
 */
fun deleteConfirmedTrackIds(requestedTrackIds: List<String>, granted: Boolean): List<String> =
    if (granted) requestedTrackIds.toList() else emptyList()
