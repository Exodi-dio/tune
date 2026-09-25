package com.exodidio.tune.ui.screens

import com.exodidio.tune.sync.LibraryAlbum
import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLookupMapTest {
    private fun track(id: String, albumId: String) = LibraryTrack(id = id, title = id, artists = "a", albumId = albumId)

    @Test
    fun groupsTracksByAlbum() {
        val map = tracksByAlbumId(listOf(track("t1", "a1"), track("t2", "a1"), track("t3", "a2")))
        assertEquals(listOf("t1", "t2"), map["a1"]!!.map { it.id })
        assertEquals(listOf("t3"), map["a2"]!!.map { it.id })
    }

    @Test
    fun indexMapReplacesIndexOfFirst() {
        val tracks = listOf(track("t1", "a1"), track("t2", "a1"))
        assertEquals(1, trackIndexById(tracks)["t2"])
    }

    @Test
    fun albumMapReplacesFind() {
        val albums = listOf(LibraryAlbum(id = "a1", title = "A"))
        assertEquals("A", albumsById(albums)["a1"]!!.title)
    }

    @Test
    fun albumContextTracksIncludeMetadataFallback() {
        val albums = listOf(LibraryAlbum(id = "a1", title = "A"))
        val tracks = listOf(
            LibraryTrack(id = "legacy", title = "Legacy", artists = "a", metadataJson = "{\"album\":{\"id\":\"a1\"}}"),
        )
        val map = albumContextTracksByAlbumId(albums, tracks)
        assertEquals(listOf("legacy"), map["a1"]!!.map { it.id })
    }

    @Test
    fun albumContextTracksSortByDiscAndTrack() {
        val albums = listOf(LibraryAlbum(id = "a1", title = "A"))
        val tracks = listOf(
            LibraryTrack(id = "missing", title = "Missing", artists = "a", album = "Album", albumId = "a1", syncOrder = 0),
            LibraryTrack(id = "second", title = "Second", artists = "a", album = "Album", albumId = "a1", discNumber = 1, trackNumber = 2, syncOrder = 2),
            LibraryTrack(id = "first", title = "First", artists = "a", album = "Album", albumId = "a1", discNumber = 1, trackNumber = 1, syncOrder = 1),
        )
        val map = albumContextTracksByAlbumId(albums, tracks)
        assertEquals(listOf("first", "second", "missing"), map["a1"]!!.map { it.id })
    }
}
