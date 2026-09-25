package com.exodidio.tune.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalTrackMappingTest {
    private fun row(
        mediaId: Long = 42L,
        uri: String = "content://media/external/audio/media/42",
        displayName: String? = "track.mp3",
        title: String? = "Song",
        artist: String? = "Singer",
        album: String? = "Record",
    ) = MediaRow(
        mediaId = mediaId,
        uri = uri,
        displayName = displayName,
        title = title,
        artist = artist,
        album = album,
        albumArtist = null,
        genre = "Pop",
        composer = null,
        year = 2024,
        trackNo = 3,
        discNo = 1,
        totalTracks = 10,
        totalDiscs = 1,
        durationMs = 183_000L,
        bitrate = 320_000,
        sizeBytes = 7_000_000L,
        dateAddedSec = 1_700_000_000L,
        mimeType = "audio/mpeg",
        label = null,
        copyright = null,
    )

    @Test fun mapsFullRow() {
        val track = row().toLocalTrack()!!
        assertEquals(42L, track.mediaId)
        assertEquals("content://media/external/audio/media/42", track.uri)
        assertEquals("Song", track.title)
        assertEquals("Singer", track.artist)
        assertEquals("Record", track.album)
        assertEquals(3, track.trackNo)
        assertEquals(183_000L, track.durationMs)
    }

    @Test fun blankUriIsSkipped() {
        assertNull(row(uri = "").toLocalTrack())
        assertNull(row(uri = "   ").toLocalTrack())
    }

    @Test fun badIdIsSkipped() {
        assertNull(row(mediaId = 0L).toLocalTrack())
        assertNull(row(mediaId = -5L).toLocalTrack())
    }

    @Test fun titleFallsBackToFileName() {
        assertEquals("track", row(title = null, displayName = "track.mp3").toLocalTrack()!!.title)
    }

    @Test fun blankTitleStaysBlankForDisplayFallback() {
        assertEquals("", row(title = null, displayName = null).toLocalTrack()!!.title)
    }

    @Test fun missingArtistAlbumStayBlank() {
        val track = row(artist = null, album = null).toLocalTrack()!!
        assertEquals("", track.artist)
        assertEquals("", track.album)
    }

    @Test fun nonLatinTextPassesThrough() {
        val track = row(title = "夜に駆ける", artist = "YOASOBI", album = "فيروز").toLocalTrack()!!
        assertEquals("夜に駆ける", track.title)
        assertEquals("YOASOBI", track.artist)
        assertEquals("فيروز", track.album)
    }

    @Test fun applyTagsNullKeepsScanValues() {
        val track = row().toLocalTrack()!!
        assertEquals(track, track.applyTags(null))
    }

    @Test fun applyTagsOverridesWithNonBlankValues() {
        val updated = row(title = null, artist = null).toLocalTrack()!!.applyTags(
            RawTags(title = "Real Title", artist = "Real Artist", album = null, albumArtist = null, genre = null, year = null, trackNo = null, discNo = null, durationMs = null, bitrate = null, sampleRateHz = null, bitDepth = null, hasEmbeddedArt = false),
        )
        assertEquals("Real Title", updated.title)
        assertEquals("Real Artist", updated.artist)
    }

    @Test fun applyTagsIgnoresBlankTagValues() {
        val updated = row(title = "Kept").toLocalTrack()!!.applyTags(
            RawTags(title = "  ", artist = "", album = null, albumArtist = null, genre = null, year = null, trackNo = null, discNo = null, durationMs = null, bitrate = null, sampleRateHz = null, bitDepth = null, hasEmbeddedArt = false),
        )
        assertEquals("Kept", updated.title)
    }
}
