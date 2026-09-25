package com.exodidio.tune.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.ArtistRow
import com.exodidio.tune.ui.components.ArtistContextMenu
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.LibraryVirtualList
import com.exodidio.tune.ui.components.LibraryTextFilter
import com.exodidio.tune.ui.components.MaterialSymbols
import dev.chrisbanes.haze.HazeState
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import com.exodidio.tune.player.PlaybackQueueSnapshot

@Composable
internal fun LibraryArtistsContent(
    uiState: LibraryArtistsUiState,
    modifier: Modifier = Modifier,
    listState: LazyListState = remember(uiState.sortOption, uiState.sortOrder) { LazyListState() },
    contentPadding: PaddingValues = PaddingValues(),
    onArtistClick: ((com.exodidio.tune.sync.LibraryArtist) -> Unit)? = null,
    onFilterQueryChange: (String) -> Unit = {},
    orderedTrackIdsForArtist: (String) -> List<String> = { emptyList() },
    onArtistPlayNext: (List<String>) -> Unit = {},
    onArtistAddToQueue: (List<String>) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
    hazeState: HazeState? = null,
    playbackQueue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
) {
    var contextArtistId by remember { mutableStateOf<String?>(null) }
    val listPadding = remember(contentPadding) {
        PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
            start = 0.dp,
            end = 0.dp,
        )
    }
    LibraryVirtualList(
        items = uiState.artists,
        isLoaded = uiState.isLoaded,
        key = { artist -> artist.id },
        contentType = "artist_row",
        listState = listState,
        contentPadding = listPadding,
        modifier = modifier,
        dividerTestTag = "artist-row-divider",
        filterKey = "artists",
        filterActive = uiState.filterQuery.isNotBlank(),
        filterContent = {
            LibraryTextFilter(
                value = uiState.filterQuery,
                onValueChange = onFilterQueryChange,
                placeholder = stringResource(R.string.filter_placeholder_search),
            )
        },
        alphabeticalIndexKey = if (uiState.sortOption == ArtistSortOption.Name) ({ artist -> artist.sortName.ifBlank { artist.name } }) else null,
        emptyContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroCard(
                    title = stringResource(if (uiState.filterQuery.isBlank()) R.string.artists_empty_title else R.string.artists_no_match_title),
                    description = stringResource(if (uiState.filterQuery.isBlank()) R.string.artists_empty_description else R.string.filter_no_match_description),
                    symbol = MaterialSymbols.People,
                )
            }
        },
    ) { artist ->
        // Track IDs are only needed by the context menu. Avoid rebuilding the
        // artist's full track/album ordering while every row is composed or
        // recomposed during scrolling.
        val trackIds = if (contextArtistId == artist.id) {
            orderedTrackIdsForArtist(artist.id)
        } else {
            emptyList()
        }
        ArtistContextMenu(
            trackIds = trackIds,
            expanded = contextArtistId == artist.id,
            onDismiss = { if (contextArtistId == artist.id) contextArtistId = null },
            onPlayNext = onArtistPlayNext,
            onAddToQueue = onArtistAddToQueue,
            onBottomSheetRequested = onTrackContextBottomSheet,
            addToPlaylistOnly = true,
            hazeState = hazeState,
            playbackQueue = playbackQueue,
        ) {
            ArtistRow(
                name = artist.name,
                artworkPath = artist.artworkPath,
                onClick = onArtistClick?.let { callback -> { callback(artist) } },
                onLongClick = { contextArtistId = artist.id },
            )
        }
    }
}
