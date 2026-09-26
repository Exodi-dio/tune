package com.exodidio.tune.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.booleanOrNull
import com.exodidio.tune.player.PlaybackController
import com.exodidio.tune.player.PlaybackRequest
import com.exodidio.tune.sync.AndroidLibrarySyncStore
import com.exodidio.tune.sync.LibraryPlaylist
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.sync.metadataObject
import com.exodidio.tune.sync.PlaylistMutation
import com.exodidio.tune.sync.PlaylistMutationOperation
import com.exodidio.tune.sync.PlaylistMutationPayload
import java.util.UUID
import kotlinx.coroutines.launch

internal data class PlaylistDetailsUiState(
    val playlist: LibraryPlaylist? = null,
    val tracks: List<LibraryTrack> = emptyList(),
    val artworkPaths: List<String> = emptyList(),
    val customArtworkPath: String? = null,
    internal val playlists: List<LibraryPlaylist> = emptyList(),
    internal val allTracks: List<LibraryTrack> = emptyList(),
    internal val artworkPathByKey: Map<String, String> = emptyMap(),
)

internal class PlaylistDetailsViewModel(
    private val syncStore: AndroidLibrarySyncStore,
    private val playbackController: PlaybackController,
) : ViewModel() {
    class Factory(
        private val store: AndroidLibrarySyncStore,
        private val playback: PlaybackController,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaylistDetailsViewModel(store, playback) as T
    }

    val uiState: StateFlow<PlaylistDetailsUiState> = combine(
        syncStore.playlists,
        syncStore.tracks,
        syncStore.artworkPaths,
    ) { playlists, tracks, artworkPaths ->
        PlaylistDetailsUiState(
            playlists = playlists,
            allTracks = tracks,
            artworkPathByKey = artworkPaths,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistDetailsUiState())

    fun play(playlistId: String, shuffle: Boolean) {
        val tracks = playlistDetailsUiStateFor(uiState.value, playlistId).tracks
        if (tracks.isNotEmpty()) {
            val request = PlaybackRequest(tracks.map { it.id }, 0)
            if (shuffle) playbackController.shuffle(request) else playbackController.play(request)
        }
    }

    fun playTrack(playlistId: String, trackId: String) {
        val tracks = playlistDetailsUiStateFor(uiState.value, playlistId).tracks
        albumPlaybackRequestFor(tracks, trackId)?.let(playbackController::play)
    }

    fun removeTrack(playlistId: String, trackId: String) {
        if (playlistId == FavoritesPlaylistId || trackId.isBlank()) return
        viewModelScope.launch {
            syncStore.queuePlaylistMutation(
                PlaylistMutation(
                    mutationId = UUID.randomUUID().toString(),
                    playlistId = playlistId,
                    operation = PlaylistMutationOperation.REMOVE_TRACK,
                    updatedAt = System.currentTimeMillis(),
                    payload = PlaylistMutationPayload(trackId = trackId),
                ),
            )
        }
    }

    fun moveTrack(playlistId: String, trackId: String, previousTrackId: String?, nextTrackId: String?) {
        if (playlistId == FavoritesPlaylistId || trackId.isBlank()) return
        viewModelScope.launch {
            syncStore.queuePlaylistMutation(
                PlaylistMutation(
                    mutationId = UUID.randomUUID().toString(),
                    playlistId = playlistId,
                    operation = PlaylistMutationOperation.MOVE_TRACK,
                    updatedAt = System.currentTimeMillis(),
                    payload = PlaylistMutationPayload(trackId = trackId, previousTrackId = previousTrackId, nextTrackId = nextTrackId),
                ),
            )
        }
    }
}

internal fun playlistMoveAnchors(trackIds: List<String>, trackId: String): Pair<String?, String?> {
    val index = trackIds.indexOf(trackId)
    return trackIds.getOrNull(index - 1) to trackIds.getOrNull(index + 1)
}

internal fun playlistDetailsUiStateFor(
    state: PlaylistDetailsUiState,
    playlistId: String,
): PlaylistDetailsUiState {
    val basePlaylist = playlistsWithFavorites(state.playlists).firstOrNull { it.id == playlistId }
        ?: return PlaylistDetailsUiState()
    val tracksById = state.allTracks.associateBy { it.id }
    val playlist = if (basePlaylist.id == FavoritesPlaylistId) {
        basePlaylist.copy(trackIds = state.allTracks.filter(LibraryTrack::isFavorite).map(LibraryTrack::id))
    } else {
        basePlaylist
    }
    val tracks = playlist.trackIds.mapNotNull(tracksById::get)
    return PlaylistDetailsUiState(
        playlist = playlist,
        tracks = tracks,
        artworkPaths = playlistArtworkPaths(playlist, state.allTracks, state.artworkPathByKey),
        customArtworkPath = playlistManualArtworkPath(playlist, state.artworkPathByKey),
    )
}

// Pure helpers take an already-parsed object so composables parse once.
internal fun isFavoriteOf(metadata: kotlinx.serialization.json.JsonObject?): Boolean =
    (metadata?.get("is_favorite") as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull == true

internal fun durationSecondsOf(metadata: kotlinx.serialization.json.JsonObject?): Long? =
    (metadata?.get("duration") as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull?.coerceAtLeast(0L)

internal fun LibraryTrack.isFavorite(): Boolean = isFavoriteOf(metadataObject())

internal fun playlistTotalDurationSeconds(tracks: List<LibraryTrack>): Long = tracks.sumOf { track ->
    durationSecondsOf(track.metadataObject()) ?: 0L
}

internal fun formatPlaylistTotalDuration(
    totalSeconds: Long,
    day: (Long) -> String,
    hour: (Long) -> String,
    minute: (Long) -> String,
    second: (Long) -> String,
): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    val days = seconds / 86_400L
    val hours = seconds % 86_400L / 3_600L
    val minutes = seconds % 3_600L / 60L
    val remainder = seconds % 60L
    return when {
        days > 0 -> "${day(days)} ${hour(hours)}"
        hours > 0 -> "${hour(hours)} ${minute(minutes)}"
        minutes > 0 -> minute(minutes)
        else -> second(remainder)
    }
}
