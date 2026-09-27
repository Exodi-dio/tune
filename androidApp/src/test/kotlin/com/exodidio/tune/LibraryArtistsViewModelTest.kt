package com.exodidio.tune

import com.exodidio.tune.sync.LibraryArtist
import com.exodidio.tune.ui.screens.ArtistSortOption
import com.exodidio.tune.ui.screens.SortOrder
import com.exodidio.tune.ui.screens.artistDisplayArtworkPath
import com.exodidio.tune.ui.screens.artistManualArtworkPath
import com.exodidio.tune.ui.screens.sortArtists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryArtistsViewModelTest {
    private val artists = listOf(
        LibraryArtist(id = "1", name = "Zebra", createdAt = "2026-01-01T00:00:00Z", sortName = "Zebra"),
        LibraryArtist(id = "2", name = "Alpha", createdAt = "2026-05-01T00:00:00Z", sortName = "Alpha"),
        LibraryArtist(id = "3", name = "Bravo", createdAt = "2025-12-01T00:00:00Z", sortName = "Bravo"),
    )

    @Test
    fun sortsByNameInBothDirections() {
        assertEquals(
            listOf("Alpha", "Bravo", "Zebra"),
            sortArtists(artists, ArtistSortOption.Name, SortOrder.Ascending).map { it.name },
        )
        assertEquals(
            listOf("Zebra", "Bravo", "Alpha"),
            sortArtists(artists, ArtistSortOption.Name, SortOrder.Descending).map { it.name },
        )
    }

    @Test
    fun sortsByDateAddedWithNameTieBreaker() {
        assertEquals(
            listOf("Bravo", "Zebra", "Alpha"),
            sortArtists(artists, ArtistSortOption.DateAdded, SortOrder.Ascending).map { it.name },
        )
    }

    @Test
    fun usesCanonicalSortNameInsteadOfDisplayName() {
        val artists = listOf(
            LibraryArtist(id = "1", name = "Zulu", sortName = "Alpha"),
            LibraryArtist(id = "2", name = "Alpha", sortName = "Zulu"),
        )

        assertEquals(listOf("Zulu", "Alpha"), sortArtists(artists, ArtistSortOption.Name, SortOrder.Ascending).map { it.name })
    }

    @Test
    fun usesCustomArtworkBeforeSyncDerived() {
        val artist = LibraryArtist(id = "a", name = "Artist A", artworkPath = "sync/a.jpg")

        assertEquals(
            "custom/a.jpg",
            artistDisplayArtworkPath(artist, mapOf("a" to "custom-key"), mapOf("custom-key" to "custom/a.jpg")),
        )
    }

    @Test
    fun fallsBackToSyncDerivedWhenNoCustomArtwork() {
        val artist = LibraryArtist(id = "a", name = "Artist A", artworkPath = "sync/a.jpg")

        assertEquals("sync/a.jpg", artistDisplayArtworkPath(artist, emptyMap(), emptyMap()))
    }

    @Test
    fun editorArtworkUsesOnlyTheManualCover() {
        assertNull(artistManualArtworkPath("a", emptyMap(), emptyMap()))
        assertEquals(
            "custom/a.jpg",
            artistManualArtworkPath("a", mapOf("a" to "custom-key"), mapOf("custom-key" to "custom/a.jpg")),
        )
    }
}
