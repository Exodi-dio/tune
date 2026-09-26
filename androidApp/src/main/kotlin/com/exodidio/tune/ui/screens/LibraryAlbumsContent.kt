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
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.AlbumRow
import com.exodidio.tune.ui.components.AlbumContextMenu
import com.exodidio.tune.ui.components.DiscGridItem
import com.exodidio.tune.ui.components.HeroCard
import com.exodidio.tune.ui.components.LibraryVirtualList
import com.exodidio.tune.ui.components.LibraryPlaybackActions
import com.exodidio.tune.ui.components.LibraryTextFilter
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import com.exodidio.tune.ui.components.discGridItems
import com.exodidio.tune.player.PlaybackQueueSnapshot

internal fun albumsById(albums: List<com.exodidio.tune.sync.LibraryAlbum>): Map<String, com.exodidio.tune.sync.LibraryAlbum> =
    albums.associateBy { it.id }

internal fun albumContextTracksByAlbumId(
    albums: List<com.exodidio.tune.sync.LibraryAlbum>,
    tracks: List<com.exodidio.tune.sync.LibraryTrack>,
): Map<String, List<com.exodidio.tune.sync.LibraryTrack>> =
    albums.associate { album ->
        album.id to albumDetailsUiStateFor(
            AlbumDetailsUiState(albums = albums, tracks = tracks),
            album.id,
        ).tracks
    }

@Composable
internal fun LibraryAlbumsContent(
    uiState: LibraryAlbumsUiState,
    modifier: Modifier = Modifier,
    listState: LazyListState = remember(uiState.sortOption, uiState.sortOrder) { LazyListState() },
    contentPadding: PaddingValues = PaddingValues(),
    onAlbumClick: ((com.exodidio.tune.sync.LibraryAlbum) -> Unit)? = null,
    hazeState: HazeState? = null,
    playbackQueue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
    onAlbumPlay: (String, Boolean) -> Unit = { _, _ -> },
    onAlbumPlayNext: (List<String>) -> Unit = {},
    onAlbumAddToQueue: (List<String>) -> Unit = {},
    onAlbumAddToFavorites: (List<String>) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
    onPlayAll: (Boolean) -> Unit = {},
    onFilterQueryChange: (String) -> Unit = {},
) {
    var contextAlbumId by remember { mutableStateOf<String?>(null) }
    val albumByIdMap = remember(uiState.albums) { albumsById(uiState.albums) }
    val albumTracksMap = remember(uiState.albums, uiState.tracks) {
        albumContextTracksByAlbumId(uiState.albums, uiState.tracks)
    }
    val unknownArtist = stringResource(R.string.album_unknown_artist)
    val gridItems = remember(uiState.albums, unknownArtist) {
        uiState.albums.map { album ->
            DiscGridItem(
                id = album.id,
                title = album.title,
                subtitle = album.artist.ifBlank { unknownArtist },
                artworkPath = album.artworkPath,
                fallbackSymbol = MaterialSymbols.Album,
            )
        }
    }
    val listPadding = remember(contentPadding) {
        PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
            start = 0.dp,
            end = 0.dp,
        )
    }
    LibraryVirtualList(
        items = uiState.albums,
        isLoaded = uiState.isLoaded,
        key = { album -> album.id },
        contentType = "album_row",
        listState = listState,
        contentPadding = listPadding,
        modifier = modifier,
        dividerTestTag = "album-row-divider",
        filterKey = "albums",
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
            AlbumSortOption.Name -> { album -> album.sortTitle.ifBlank { album.title } }
            AlbumSortOption.Artist -> { album -> album.sortArtist.ifBlank { album.artist } }
            else -> null
        },
        alphabeticalIndexItemsPerLazyItem = if (uiState.layoutMode == AlbumLayoutMode.Grid) 2 else 1,
        emptyContent = {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroCard(
                    title = stringResource(if (uiState.filterQuery.isBlank()) R.string.albums_empty_title else R.string.albums_no_match_title),
                    description = stringResource(if (uiState.filterQuery.isBlank()) R.string.albums_empty_description else R.string.filter_no_match_description),
                    symbol = MaterialSymbols.Album,
                )
            }
        },
        customItemsContent = if (uiState.layoutMode == AlbumLayoutMode.Grid) {
            {
                discGridItems(
                    items = gridItems,
                    horizontalContentPadding = 24.dp,
                    onClick = { albumId -> albumByIdMap[albumId]?.let { album -> onAlbumClick?.invoke(album) } },
                    onLongClick = { albumId -> contextAlbumId = albumId },
                    itemWrapper = { item, itemModifier, content ->
                        val album = albumByIdMap[item.id]
                        if (album == null) {
                            content()
                        } else {
                            val tracks = if (contextAlbumId == album.id) albumTracksMap[album.id].orEmpty() else emptyList()
                            AlbumContextMenu(
                                tracks = tracks,
                                expanded = contextAlbumId == album.id,
                                onDismiss = { if (contextAlbumId == album.id) contextAlbumId = null },
                                hazeState = hazeState,
                                playbackQueue = playbackQueue,
                                onPlay = { onAlbumPlay(album.id, false) },
                                onShuffle = { onAlbumPlay(album.id, true) },
                                onPlayNext = onAlbumPlayNext,
                                onAddToQueue = onAlbumAddToQueue,
                                onAddToFavorites = onAlbumAddToFavorites,
                                onBottomSheetRequested = onTrackContextBottomSheet,
                                modifier = itemModifier,
                                anchor = content,
                            )
                        }
                    },
                )
            }
        } else {
            null
        },
    ) { album ->
        val tracks = if (contextAlbumId == album.id) albumTracksMap[album.id].orEmpty() else emptyList()
        AlbumContextMenu(
            tracks = tracks,
            expanded = contextAlbumId == album.id,
            onDismiss = { if (contextAlbumId == album.id) contextAlbumId = null },
            hazeState = hazeState,
            playbackQueue = playbackQueue,
            onPlay = { onAlbumPlay(album.id, false) },
            onShuffle = { onAlbumPlay(album.id, true) },
            onPlayNext = onAlbumPlayNext,
            onAddToQueue = onAlbumAddToQueue,
            onAddToFavorites = onAlbumAddToFavorites,
            onBottomSheetRequested = onTrackContextBottomSheet,
        ) {
            AlbumRow(
                title = album.title,
                artist = album.artist.ifBlank { stringResource(R.string.album_unknown_artist) },
                artworkPath = album.artworkPath,
                onClick = onAlbumClick?.let { callback -> { callback(album) } },
                onLongClick = { contextAlbumId = album.id },
            )
        }
    }
}
