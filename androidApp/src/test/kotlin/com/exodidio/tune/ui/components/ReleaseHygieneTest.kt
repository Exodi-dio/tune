package com.exodidio.tune.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseHygieneTest {
    @Test
    fun homeTrackContentTypeIsStable() {
        assertEquals("home_track", ListContentTypes.HOME_TRACK)
    }

    @Test
    fun searchContentTypePrefixesAreStable() {
        assertEquals("search_grid_", ListContentTypes.SEARCH_GRID_PREFIX)
        assertEquals("search_row_", ListContentTypes.SEARCH_ROW_PREFIX)
    }

    @Test
    fun albumTrackContentTypeIsStable() {
        assertEquals("album_track", ListContentTypes.ALBUM_TRACK)
    }

    @Test
    fun playlistTrackContentTypeIsStable() {
        assertEquals("playlist_track", ListContentTypes.PLAYLIST_TRACK)
    }

    @Test
    fun trackInfoRowContentTypeIsStable() {
        assertEquals("track_info_row", ListContentTypes.TRACK_INFO_ROW)
    }

    @Test
    fun playlistPickerRowContentTypeIsStable() {
        assertEquals("playlist_picker_row", ListContentTypes.PLAYLIST_PICKER_ROW)
    }

    @Test
    fun lyricsResultContentTypeIsStable() {
        assertEquals("lyrics_result", ListContentTypes.LYRICS_RESULT)
    }

    @Test
    fun insightArtistContentTypeIsStable() {
        assertEquals("insight_artist", ListContentTypes.INSIGHT_ARTIST)
    }
}
