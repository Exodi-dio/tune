package com.exodidio.tune.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.R
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.LibraryVirtualList
import com.exodidio.tune.ui.components.LibraryPlaybackActions
import com.exodidio.tune.ui.components.LibraryTextFilter
import com.exodidio.tune.ui.components.TrackRow
import com.exodidio.tune.ui.components.TrackContextArtist
import com.exodidio.tune.ui.components.TrackContextMenu
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest

@Composable
internal fun LibraryTracksContent(
    uiState: LibraryTracksUiState,
    onSortOptionSelected: (TrackSortOption) -> Unit,
    onToggleSortOrder: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = remember(uiState.sortOption, uiState.sortOrder) { LazyListState() },
    contentPadding: PaddingValues = PaddingValues(),
    onTrackClick: ((LibraryTrack) -> Unit)? = null,
    onTrackMoreClick: ((LibraryTrack) -> Unit)? = null,
    playbackQueue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
    onTrackPlayNext: (LibraryTrack) -> Unit = {},
    onTrackAddToQueue: (LibraryTrack) -> Unit = {},
    onTrackFavoriteToggle: (LibraryTrack, Boolean) -> Unit = { _, _ -> },
    onTrackAlbumClick: (LibraryTrack) -> Unit = {},
    onTrackArtistClick: (TrackContextArtist) -> Unit = {},
    hazeState: HazeState? = null,
    onPlayAll: (Boolean) -> Unit = {},
    onFilterQueryChange: (String) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
) {
    var contextTrack by remember { mutableStateOf<LibraryTrack?>(null) }
    val listPadding = remember(contentPadding) {
        PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
            start = 0.dp,
            end = 0.dp,
        )
    }
    LibraryVirtualList(
        items = uiState.tracks,
        isLoaded = uiState.isLoaded,
        key = { track -> track.id.ifBlank { "${track.title}_${track.artists}" } },
        contentType = "track_row",
        listState = listState,
        contentPadding = listPadding,
        modifier = modifier,
        dividerTestTag = "track-row-divider",
        filterKey = "tracks",
        filterActive = uiState.filterQuery.isNotBlank(),
        filterContent = {
            LibraryTextFilter(
                value = uiState.filterQuery,
                onValueChange = onFilterQueryChange,
                placeholder = stringResource(R.string.filter_placeholder_search),
            )
        },
        leadingContent = {
            LibraryPlaybackActions(
                playLabel = stringResource(R.string.player_play),
                shuffleLabel = stringResource(R.string.player_shuffle),
                onPlay = { onPlayAll(false) },
                onShuffle = { onPlayAll(true) },
                hazeState = hazeState,
            )
        },
        alphabeticalIndexKey = when (uiState.sortOption) {
            TrackSortOption.Name -> { track -> track.sortTitle.ifBlank { track.title } }
            TrackSortOption.Artist -> { track -> track.sortArtists.ifBlank { track.artists } }
            else -> null
        },
        emptyContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroCard(
                    title = stringResource(if (uiState.filterQuery.isBlank()) R.string.tracks_empty_title else R.string.tracks_no_match_title),
                    description = stringResource(if (uiState.filterQuery.isBlank()) R.string.tracks_empty_description else R.string.filter_no_match_description),
                    symbol = MaterialSymbols.MusicNote,
                )
            }
        },
    ) { track ->
        val onItemClick = remember(onTrackClick, track) {
            if (onTrackClick != null) { { onTrackClick(track) } } else null
        }
        val onItemMoreClick = remember(onTrackMoreClick, track) {
            { onTrackMoreClick?.invoke(track); contextTrack = track }
        }
        TrackContextMenu(
            track = track,
            expanded = contextTrack?.id == track.id,
            onDismiss = { if (contextTrack?.id == track.id) contextTrack = null },
            playbackQueue = playbackQueue,
            onPlayNext = onTrackPlayNext,
            onAddToQueue = onTrackAddToQueue,
            onFavoriteChange = onTrackFavoriteToggle,
            onGoToAlbum = onTrackAlbumClick,
            onGoToArtist = onTrackArtistClick,
            onBottomSheetRequested = onTrackContextBottomSheet,
        ) {
            TrackRow(
                title = track.title,
                artist = track.artists,
                artworkPath = track.artworkPath,
                onClick = onItemClick,
                onMoreClick = onItemMoreClick,
                onLongClick = { contextTrack = track },
            )
        }
    }
}
