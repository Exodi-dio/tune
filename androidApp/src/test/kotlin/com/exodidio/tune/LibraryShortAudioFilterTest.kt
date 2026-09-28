package com.exodidio.tune

import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.screens.HideShortAudioThresholdSeconds
import com.exodidio.tune.ui.screens.filterShortAudioTracks
import com.exodidio.tune.ui.screens.isShortAudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryShortAudioFilterTest {
    private fun track(id: String, metadataJson: String): LibraryTrack =
        LibraryTrack(id = id, title = "Title $id", artists = "Artist", metadataJson = metadataJson)

    @Test
    fun `short audio threshold is forty seconds`() {
        assertEquals(40L, HideShortAudioThresholdSeconds)
    }

    @Test
    fun `track under forty seconds is short audio`() {
        assertTrue(isShortAudioTrack(track("t39", "{\"duration\":39}")))
    }

    @Test
    fun `track at exactly forty seconds is kept`() {
        assertFalse(isShortAudioTrack(track("t40", "{\"duration\":40}")))
    }

    @Test
    fun `track without duration is never hidden`() {
        assertFalse(isShortAudioTrack(track("t-unknown", "{}")))
    }

    @Test
    fun `float duration strings are tolerated`() {
        assertTrue(isShortAudioTrack(track("t-float-short", "{\"duration\":\"39.5\"}")))
        assertFalse(isShortAudioTrack(track("t-float-kept", "{\"duration\":\"40.0\"}")))
    }

    @Test
    fun `filter excludes short tracks when enabled`() {
        val tracks = listOf(
            track("t39", "{\"duration\":39}"),
            track("t40", "{\"duration\":40}"),
            track("t-unknown", "{}"),
        )

        assertEquals(listOf("t40", "t-unknown"), filterShortAudioTracks(tracks, hideEnabled = true).map { it.id })
    }

    @Test
    fun `filter keeps everything when disabled`() {
        val tracks = listOf(
            track("t39", "{\"duration\":39}"),
            track("t40", "{\"duration\":40}"),
        )

        assertEquals(tracks, filterShortAudioTracks(tracks, hideEnabled = false))
    }
}
