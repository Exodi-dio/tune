package com.exodidio.tune

import com.exodidio.tune.sync.LibraryAlbum
import com.exodidio.tune.ui.screens.AlbumSortOption
import com.exodidio.tune.ui.screens.AlbumLayoutMode
import com.exodidio.tune.ui.screens.SortOrder
import com.exodidio.tune.ui.screens.sortAlbums
import com.exodidio.tune.ui.screens.LibraryAlbumsUiState
import com.exodidio.tune.ui.screens.albumCollectionTrackIdsFor
import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryAlbumsViewModelTest {
    private val albums = listOf(
        LibraryAlbum(id = "1", title = "Zebra", artist = "Delta", createdAt = "2026-01-01T00:00:00Z", sortTitle = "Zebra", sortArtist = "Delta"),
        LibraryAlbum(id = "2", title = "Alpha", artist = "Zebra", createdAt = "2026-05-01T00:00:00Z", sortTitle = "Alpha", sortArtist = "Zebra"),
        LibraryAlbum(id = "3", title = "Bravo", artist = "Alpha", createdAt = "2025-12-01T00:00:00Z", sortTitle = "Bravo", sortArtist = "Alpha"),
    )

    @Test
    fun sortsAlbumsByNameArtistAndDateAdded() {
        assertEquals(listOf("Alpha", "Bravo", "Zebra"), sortAlbums(albums, AlbumSortOption.Name, SortOrder.Ascending).map { it.title })
        assertEquals(listOf("Alpha", "Delta", "Zebra"), sortAlbums(albums, AlbumSortOption.Artist, SortOrder.Ascending).map { it.artist })
        assertEquals(listOf("2025-12-01T00:00:00Z", "2026-01-01T00:00:00Z", "2026-05-01T00:00:00Z"), sortAlbums(albums, AlbumSortOption.DateAdded, SortOrder.Ascending).map { it.createdAt })
    }

    @Test
    fun reversesTheSortedAlbumOrder() {
        assertEquals(listOf("Zebra", "Bravo", "Alpha"), sortAlbums(albums, AlbumSortOption.Name, SortOrder.Descending).map { it.title })
    }

    @Test
    fun usesCanonicalAlbumAndArtistSortKeysInsteadOfDisplayLabels() {
        val albums = listOf(
            LibraryAlbum(id = "1", title = "Zulu", artist = "Zulu Artist", sortTitle = "Alpha", sortArtist = "Beta"),
            LibraryAlbum(id = "2", title = "Alpha", artist = "Alpha Artist", sortTitle = "Zulu", sortArtist = "Alpha"),
        )

        assertEquals(listOf("Zulu", "Alpha"), sortAlbums(albums, AlbumSortOption.Name, SortOrder.Ascending).map { it.title })
        assertEquals(listOf("Alpha Artist", "Zulu Artist"), sortAlbums(albums, AlbumSortOption.Artist, SortOrder.Ascending).map { it.artist })
    }

    @Test
    fun collectionPlaybackFlattensVisibleAlbumsAndKeepsDiscTrackOrder() {
        val state = LibraryAlbumsUiState(
            albums = listOf(
                LibraryAlbum(id = "second", title = "Second"),
                LibraryAlbum(id = "first", title = "First"),
            ),
            tracks = listOf(
                LibraryTrack(id = "second-track-2", title = "Two", artists = "Artist", albumId = "second", discNumber = 1, trackNumber = 2),
                LibraryTrack(id = "first-track", title = "One", artists = "Artist", albumId = "first", discNumber = 1, trackNumber = 1),
                LibraryTrack(id = "second-track-1", title = "One", artists = "Artist", albumId = "second", discNumber = 1, trackNumber = 1),
            ),
        )

        assertEquals(
            listOf("second-track-1", "second-track-2", "first-track"),
            albumCollectionTrackIdsFor(state),
        )
    }

    @Test
    fun defaultsToListLayoutForMissingOrInvalidStoredValue() {
        assertEquals(AlbumLayoutMode.List, AlbumLayoutMode.fromStorage(null))
        assertEquals(AlbumLayoutMode.List, AlbumLayoutMode.fromStorage("unexpected"))
        assertEquals(AlbumLayoutMode.Grid, AlbumLayoutMode.fromStorage("grid"))
    }
}
