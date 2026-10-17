package com.exodidio.tune.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTrackMetadataFallbackTest {
    @Test
    fun usesUnknownLabelsWhenTrackMetadataIsBlank() {
        assertEquals("Unknown song", trackDisplayTitle(LibraryTrack(title = "  ")))
        assertEquals("Unknown artist", trackDisplayArtists(LibraryTrack(artists = " ")))
    }
}
