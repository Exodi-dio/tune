package com.exodidio.tune.ui.components

import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.sync.LibraryPlaylist
import com.exodidio.tune.player.PlaybackQueueSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackContextMenuTest {
    @Test
    fun mirrorsDesktopQueueActionAvailability() {
        val queue = PlaybackQueueSnapshot(
            activeTrackIds = listOf("current", "queued"),
            currentIndex = 0,
        )

        assertEquals(
            TrackContextQueueAvailability(showPlayNext = false, showAddToQueue = false, addToQueueEnabled = false),
            trackContextQueueAvailability("current", queue),
        )
        assertEquals(
            TrackContextQueueAvailability(showPlayNext = true, showAddToQueue = true, addToQueueEnabled = false),
            trackContextQueueAvailability("queued", queue),
        )
        assertEquals(
            TrackContextQueueAvailability(showPlayNext = true, showAddToQueue = true, addToQueueEnabled = true),
            trackContextQueueAvailability("new", queue),
        )
    }

    @Test
    fun readsDistinctNavigableArtistsFromTrackMetadata() {
        val track = LibraryTrack(
            id = "track-1",
            title = "Collaboration",
            artists = "A, B",
            metadataJson = """{"artists":[{"id":"artist-a","name":"Artist A"},{"id":"artist-b","name":"Artist B"},{"id":"artist-a","name":"Artist A"},{"name":"No identifier"}]}""",
        )

        assertEquals(
            listOf(TrackContextArtist("artist-a", "Artist A"), TrackContextArtist("artist-b", "Artist B")),
            trackContextArtists(track),
        )
    }

    @Test
    fun ignoresMalformedOrMissingArtistMetadata() {
        assertEquals(
            emptyList<TrackContextArtist>(),
            trackContextArtists(LibraryTrack(id = "track-1", title = "Track", artists = "Artist", metadataJson = "not-json")),
        )
    }

    @Test
    fun playlistPickerExcludesFavoritesAndSmartPlaylists() {
        assertEquals(false, playlistIsEditable(LibraryPlaylist("favorites", "Favorites", emptyList(), "{}")))
        assertEquals(false, playlistIsEditable(LibraryPlaylist("smart", "Smart", emptyList(), "{\"playlist\":{\"is_smart\":true}}")))
        assertEquals(true, playlistIsEditable(LibraryPlaylist("normal", "Normal", emptyList(), "{\"playlist\":{\"is_smart\":false}}")))
    }
}
