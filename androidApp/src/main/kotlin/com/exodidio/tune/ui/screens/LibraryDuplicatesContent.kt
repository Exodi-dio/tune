package com.exodidio.tune.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.library.chooseKeepBest
import com.exodidio.tune.library.deleteConfirmedTrackIds
import com.exodidio.tune.library.deviceDeleteUris
import com.exodidio.tune.library.duplicateDurationSec
import com.exodidio.tune.library.duplicateQualityOf
import com.exodidio.tune.library.groupDuplicateTracks
import com.exodidio.tune.library.keepBestRemovalIds
import com.exodidio.tune.library.pendingDeleteTrackIds
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.sync.metadataObject
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.TrackContextArtist
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import com.exodidio.tune.ui.components.TrackContextMenu
import com.exodidio.tune.ui.theme.LocalTuneColors
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One duplicate copy row in [LibraryDuplicatesContent]. */
internal data class DuplicateCopyUiState(
    val track: LibraryTrack,
    val durationLabel: String,
    val filePath: String,
    val sizeLabel: String,
    val qualityLabel: String,
    val isKeep: Boolean,
)

/** One duplicate group section in [LibraryDuplicatesContent]. */
internal data class DuplicateGroupUiState(
    val key: String,
    val engineKey: String,
    val title: String,
    val artist: String,
    val copies: List<DuplicateCopyUiState>,
    val keepId: String?,
) {
    val trackIds: List<String> get() = copies.map { it.track.id }
}

/** Visible duplicate groups for [LibraryDuplicatesContent]. */
internal data class LibraryDuplicatesUiState(
    val groups: List<DuplicateGroupUiState> = emptyList(),
    val isLoaded: Boolean = true,
)

internal fun formatDuplicateDuration(totalSeconds: Int?): String {
    if (totalSeconds == null || totalSeconds < 0) return "--:--"
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun formatDuplicateSize(sizeBytes: Long?): String {
    if (sizeBytes == null || sizeBytes <= 0L) return ""
    val mb = sizeBytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "%d KB".format(sizeBytes / 1024L)
}

internal fun duplicateSizeBytes(track: LibraryTrack): Long? {
    val metadata = track.metadataObject() ?: return null
    return listOf("size", "file_size", "filesize").firstNotNullOfOrNull { key ->
        metadata[key]?.jsonPrimitive?.contentOrNull?.trim()?.toLongOrNull()
    }
}

internal fun duplicateQualityLabel(track: LibraryTrack): String {
    val quality = duplicateQualityOf(track)
    if (quality.lossless) return "Lossless"
    if (quality.bitrate >= 320_000) return "High"
    return "Standard"
}

/**
 * Pure mapping from library tracks to visible duplicate groups.
 *
 * [hiddenTrackIds] are copies removed from the library view only (keep-best and
 * post-delete cleanup — never files). [dismissedKeys] are user-dismissed groups.
 * Both are in-memory by design; no persistence required.
 */
internal fun duplicatesUiStateFor(
    tracks: List<LibraryTrack>,
    hiddenTrackIds: Set<String>,
    dismissedKeys: Set<String>,
): LibraryDuplicatesUiState {
    val visible = if (hiddenTrackIds.isEmpty()) tracks else tracks.filter { it.id !in hiddenTrackIds }
    val byId = visible.associateBy(LibraryTrack::id)
    val groups = groupDuplicateTracks(visible)
        .mapNotNull { group ->
            val copies = group.trackIds.mapNotNull { id -> byId[id] }.ifEmpty { return@mapNotNull null }
            if (copies.size < 2) return@mapNotNull null
            val keepId = chooseKeepBest(group, byId)
            val first = copies.first()
            // Sibling duration clusters share the engine bucket key, so qualify the
            // UI key with the cluster's shortest duration: dismissing one cluster
            // must not hide the others. Cluster first-durations are strictly
            // increasing, keeping keys unique.
            val uiKey = group.key + " | " + formatDuplicateDuration(duplicateDurationSec(first))
            DuplicateGroupUiState(
                key = uiKey, // filtered below: engine keys are shared across clusters
                engineKey = group.key,
                title = first.title,
                artist = first.artists,
                copies = copies.map { track ->
                    DuplicateCopyUiState(
                        track = track,
                        durationLabel = formatDuplicateDuration(duplicateDurationSec(track)),
                        filePath = track.audioPath.orEmpty(),
                        sizeLabel = formatDuplicateSize(duplicateSizeBytes(track)),
                        qualityLabel = duplicateQualityLabel(track),
                        isKeep = track.id == keepId,
                    )
                },
                keepId = keepId,
            )
        }
    return LibraryDuplicatesUiState(groups = groups.filter { it.key !in dismissedKeys })
}

/**
 * Library-visible tracks after REAL library removal of [removedIds].
 *
 * App-level hoisted hidden set lives in `App` (see `removedTrackIds`): losers
 * disappear from ALL library surfaces (tracks list, duplicates, recents) and
 * the playback queue snapshot is filtered in `AppDestinationContent`. Pure for
 * unit-testing that removed entries leave the visible track list.
 *
 * Deliberate limitation (rescan-resurrection): the set is in-memory only and is
 * NOT persisted. A library rescan/scan-replace rebuilds the store from disk and
 * the removed copies reappear (files were never deleted). Dismissed groups behave
 * the same way. This is by design — see the user-visible note
 * (`duplicates_rescan_note`) — not an accident.
 */
internal fun visibleTracksAfterRemoval(
    tracks: List<LibraryTrack>,
    removedIds: Set<String>,
): List<LibraryTrack> =
    if (removedIds.isEmpty()) tracks else tracks.filter { it.id !in removedIds }

/**
 * Exact ids the Keep-best tap removes from the library for [group].
 *
 * Single choke point for the tap handler below: resolves the retained
 * [DuplicateGroupUiState.engineKey] (never the duration-qualified UI key) and
 * returns the loser ids. The caller hides them locally AND forwards them to
 * [onTracksRemovedFromLibrary] so App-level `removedTrackIds` filtering fires.
 * Pure for unit-testing that the callback payload equals the loser ids.
 */
internal fun keepBestTapRemovalIds(
    group: DuplicateGroupUiState,
    byId: Map<String, LibraryTrack>,
): List<String> = keepBestRemovalIds(
    com.exodidio.tune.library.DuplicateGroup(group.engineKey, group.trackIds),
    byId,
)

/**
 * Queue snapshot with removed entries filtered out.
 *
 * Playing-track-removed is handled gracefully: when the current track was
 * removed, playback re-anchors to the next valid track — the entry that slid
 * into the removed position (or the new last entry if the tail was removed) —
 * so the player can skip forward cleanly. The index is always valid for a
 * non-empty queue; it is `-1` only when the queue itself is empty (clean
 * paused/empty state, no stale index, no crash). Pure for unit-testing.
 */
internal fun visiblePlaybackQueueAfterRemoval(
    queue: PlaybackQueueSnapshot,
    removedIds: Set<String>,
): PlaybackQueueSnapshot {
    if (removedIds.isEmpty()) return queue
    val visibleOriginal = queue.originalTrackIds.filter { it !in removedIds }
    val visibleActive = queue.activeTrackIds.filter { it !in removedIds }
    val currentId = queue.currentTrackId
    val newIndex = when {
        visibleActive.isEmpty() -> -1
        currentId == null || currentId !in removedIds ->
            if (currentId == null) queue.currentIndex.coerceIn(-1, visibleActive.lastIndex)
            else visibleActive.indexOf(currentId).takeIf { it >= 0 } ?: queue.currentIndex.coerceIn(0, visibleActive.lastIndex)
        else -> queue.currentIndex.coerceIn(0, visibleActive.lastIndex)
    }
    return queue.copy(
        originalTrackIds = visibleOriginal,
        activeTrackIds = visibleActive,
        currentIndex = newIndex,
    )
}

/**
 * System delete-consent launcher.
 *
 * Builds a [MediaStore.createDeleteRequest] intent for the tracks' content URIs
 * (single or multiple) and launches the system consent dialog — no broad storage
 * permission, no direct file deletion. Consent granted → [onDeleted] with the
 * confirmed ids so the caller can drop library entries; denied/cancelled →
 * nothing happens.
 */
@Composable
internal fun rememberDeleteFilesFromDeviceLauncher(
    onDeleted: (List<String>) -> Unit,
): (List<LibraryTrack>) -> Unit {
    val context = LocalContext.current
    var pendingIds by remember { mutableStateOf(emptyList<String>()) }
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val confirmed = deleteConfirmedTrackIds(pendingIds, granted = result.resultCode == Activity.RESULT_OK)
        pendingIds = emptyList()
        if (confirmed.isNotEmpty()) onDeleted(confirmed)
    }
    return remember(context, consentLauncher) {
        { tracks: List<LibraryTrack> ->
            val uris = deviceDeleteUris(tracks).mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
            if (uris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pendingIds = pendingDeleteTrackIds(tracks)
                val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
                consentLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }
        }
    }
}

@Composable
internal fun LibraryDuplicatesContent(
    tracks: List<LibraryTrack>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    playbackQueue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
    onTrackClick: (LibraryTrack) -> Unit = {},
    onTrackPlayNext: (LibraryTrack) -> Unit = {},
    onTrackAddToQueue: (LibraryTrack) -> Unit = {},
    onTrackFavoriteToggle: (LibraryTrack, Boolean) -> Unit = { _, _ -> },
    onTrackAlbumClick: (LibraryTrack) -> Unit = {},
    onTrackArtistClick: (TrackContextArtist) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
    onTracksRemovedFromLibrary: (List<String>) -> Unit = {},
) {
    val colors = LocalTuneColors.current
    var hiddenTrackIds by remember { mutableStateOf(emptySet<String>()) }
    var dismissedKeys by remember { mutableStateOf(emptySet<String>()) }
    var contextTrackId by remember { mutableStateOf<String?>(null) }
    val uiState = remember(tracks, hiddenTrackIds, dismissedKeys) {
        duplicatesUiStateFor(tracks, hiddenTrackIds, dismissedKeys)
    }
    val deleteFromDevice = rememberDeleteFilesFromDeviceLauncher { confirmedIds ->
        hiddenTrackIds = hiddenTrackIds + confirmedIds.toSet()
        onTracksRemovedFromLibrary(confirmedIds)
    }
    if (uiState.groups.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(contentPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroCard(
                symbol = MaterialSymbols.Duplicates,
                title = stringResource(R.string.duplicates_empty_title),
                description = stringResource(R.string.duplicates_empty_description),
            )
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "duplicates_rescan_note", contentType = "duplicates_note") {
            Text(
                text = stringResource(R.string.duplicates_rescan_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        items(uiState.groups, key = { it.key }, contentType = { "duplicate_group" }) { group ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textMain,
                )
                Text(
                    text = group.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(4.dp))
                group.copies.forEach { copy ->
                    val track = copy.track
                    TrackContextMenu(
                        track = track,
                        expanded = contextTrackId == track.id,
                        onDismiss = { if (contextTrackId == track.id) contextTrackId = null },
                        playbackQueue = playbackQueue,
                        onPlayNext = onTrackPlayNext,
                        onAddToQueue = onTrackAddToQueue,
                        onFavoriteChange = onTrackFavoriteToggle,
                        onGoToAlbum = onTrackAlbumClick,
                        onGoToArtist = onTrackArtistClick,
                        onBottomSheetRequested = onTrackContextBottomSheet,
                        onDeleteFromDevice = { track -> deleteFromDevice(listOf(track)) },
                        anchor = {
                            DuplicateCopyRow(
                                copy = copy,
                                onClick = { onTrackClick(track) },
                                onLongClick = { contextTrackId = track.id },
                            )
                        },
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        // Keep-best tap: hide locally AND notify the host so the
                        // App-level `removedTrackIds` filter fires on every surface.
                        val removalIds = keepBestTapRemovalIds(group, tracks.associateBy(LibraryTrack::id))
                        hiddenTrackIds = hiddenTrackIds + removalIds.toSet()
                        onTracksRemovedFromLibrary(removalIds)
                    }) {
                        Text(stringResource(R.string.duplicates_keep_best))
                    }
                    TextButton(onClick = { dismissedKeys = dismissedKeys + group.key }) {
                        Text(stringResource(R.string.duplicates_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateCopyRow(
    copy: DuplicateCopyUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
    Column(
        modifier = modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                    text = copy.track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textMain,
                    modifier = Modifier.weight(1f),
                )
            Text(
                    text = copy.durationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
        }
        Text(
                text = copy.track.artists,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        if (copy.filePath.isNotBlank()) {
            Text(
                    text = copy.filePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                    text = copy.qualityLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                )
            if (copy.sizeLabel.isNotBlank()) {
                Text(
                        text = copy.sizeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
            }
            if (copy.isKeep) {
                Text(
                        text = stringResource(R.string.duplicates_kept_badge),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                    )
            }
        }
    }
}