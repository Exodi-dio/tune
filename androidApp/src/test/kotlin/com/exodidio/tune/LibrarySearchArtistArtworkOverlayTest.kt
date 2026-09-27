package com.exodidio.tune

import com.exodidio.tune.sync.LibraryArtist
import com.exodidio.tune.ui.screens.overlaySearchArtistArtwork
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySearchArtistArtworkOverlayTest {
    @Test fun customArtworkWinsOverSyncDerived() {
        val artists = listOf(LibraryArtist(id = "a", name = "Artist A", artworkPath = "sync/a.jpg"))
        val overlaid = overlaySearchArtistArtwork(
            artists,
            artistArtworkKeys = mapOf("a" to "custom-key"),
            artworkPaths = mapOf("custom-key" to "custom/a.jpg"),
        )
        assertEquals("custom/a.jpg", overlaid.single().artworkPath)
    }

    @Test fun fallsBackToSyncDerivedWhenNoCustomArtwork() {
        val artists = listOf(LibraryArtist(id = "a", name = "Artist A", artworkPath = "sync/a.jpg"))
        val overlaid = overlaySearchArtistArtwork(artists, emptyMap(), emptyMap())
        assertEquals("sync/a.jpg", overlaid.single().artworkPath)
    }

    @Test fun preservesNullWhenNoArtworkAnywhere() {
        val artists = listOf(LibraryArtist(id = "a", name = "Artist A", artworkPath = null))
        val overlaid = overlaySearchArtistArtwork(artists, emptyMap(), emptyMap())
        assertEquals(null, overlaid.single().artworkPath)
    }
}
