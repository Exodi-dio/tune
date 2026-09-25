package com.exodidio.tune.library

import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.components.trackAudioQuality
import com.exodidio.tune.ui.components.TrackAudioQuality
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalImportTest {
    private fun flacTrack() = LocalTrack(
        mediaId = 7L,
        uri = "content://media/external/audio/media/7",
        title = "HiRes Song",
        artist = "Singer",
        album = "Record",
        albumArtist = null,
        genre = "Jazz",
        composer = null,
        year = 2023,
        trackNo = 1,
        discNo = 1,
        totalTracks = null,
        totalDiscs = null,
        durationMs = 200_000L,
        bitrate = 4_000_000,
        sizeBytes = 100_000_000L,
        dateAddedSec = 1_700_000_001L,
        mimeType = "audio/flac",
        label = null,
        copyright = null,
    ).applyTags(
        RawTags(title = null, artist = null, album = null, albumArtist = null, genre = null, year = null, trackNo = null, discNo = null, durationMs = null, bitrate = null, sampleRateHz = 96_000, bitDepth = 24, hasEmbeddedArt = false),
    )

    @Test fun buildRowsUsesLocalPlan() {
        val result = buildLocalImport(listOf(flacTrack()))
        assertEquals("local", result.planId)
        assertTrue(result.tracks.all { it.planId == "local" })
        assertTrue(result.audioAssets.all { it.planId == "local" })
        assertEquals(1, result.inserted)
        assertEquals(0, result.skipped)
    }

    @Test fun corruptTracksSkippedAndCounted() {
        val good = flacTrack()
        val bad = good.copy(uri = "")
        val result = buildLocalImport(listOf(good, bad))
        assertEquals(1, result.inserted)
        assertEquals(1, result.skipped)
    }

    @Test fun audioAssetsAddressedPerTrack() {
        val result = buildLocalImport(listOf(flacTrack()))
        assertEquals(1, result.audioAssets.size)
        assertEquals("audio:local-7", result.audioAssets[0].assetId)
        assertEquals("content://media/external/audio/media/7", result.audioAssets[0].relativePath)
    }

    @Test fun metadataJsonCarriesQualityKeys() {
        val result = buildLocalImport(listOf(flacTrack()))
        val root = Json.parseToJsonElement(result.tracks[0].rawJson).jsonObject
        assertEquals("flac", root["format"]!!.jsonPrimitive.content)
        assertEquals("flac", root["codec"]!!.jsonPrimitive.content)
        assertEquals(24, root["bit_depth"]!!.jsonPrimitive.content.toInt())
        assertEquals(96_000L, root["sample_rate"]!!.jsonPrimitive.content.toLong())
        assertEquals(100_000_000L, root["file_size"]!!.jsonPrimitive.content.toLong())
        assertEquals(200_000L, root["duration_ms"]!!.jsonPrimitive.content.toLong())
        assertEquals(2023, root["year"]!!.jsonPrimitive.content.toInt())
    }

    @Test fun metadataJsonCarriesStableArtistAlbumIds() {
        val a = buildLocalImport(listOf(flacTrack()))
        val b = buildLocalImport(listOf(flacTrack().copy(mediaId = 8L, uri = "content://media/external/audio/media/8")))
        val idA = Json.parseToJsonElement(a.tracks[0].rawJson).jsonObject["artists"]!!.toString()
        val idB = Json.parseToJsonElement(b.tracks[0].rawJson).jsonObject["artists"]!!.toString()
        assertEquals(idA, idB)
        assertTrue(idA.contains("local-artist-"))
        val albumA = Json.parseToJsonElement(a.tracks[0].rawJson).jsonObject["album"]!!.toString()
        assertTrue(albumA.contains("local-album-"))
    }

    @Test fun blankAlbumYieldsEmptyAlbumId() {
        val track = flacTrack().copy(album = "")
        val result = buildLocalImport(listOf(track))
        assertEquals("", result.tracks[0].albumId)
    }

    @Test fun searchDocumentsCoverTracksArtistsAlbums() {
        val result = buildLocalImport(listOf(flacTrack()))
        val byType = result.searchDocuments.groupBy { it.entityType }.mapValues { it.value.map { doc -> doc.entityId } }
        assertTrue((byType["track"] ?: emptyList()).any { it == "local-7" })
        assertTrue((byType["artist"] ?: emptyList()).isNotEmpty())
        assertTrue((byType["album"] ?: emptyList()).isNotEmpty())
        val trackDoc = result.searchDocuments.first { it.entityType == "track" }
        assertTrue(trackDoc.content.contains("HiRes Song"))
    }

    @Test fun qualityEndToEnd() {
        val row = buildLocalImport(listOf(flacTrack())).tracks[0]
        val hiRes = LibraryTrack(title = row.title, artists = row.artists, metadataJson = row.rawJson)
        assertEquals(TrackAudioQuality.HiRes, trackAudioQuality(hiRes))
        val mp3Row = buildLocalImport(
            listOf(flacTrack().copy(mediaId = 9L, uri = "content://media/external/audio/media/9", mimeType = "audio/mpeg")),
        ).tracks[0]
        val lossy = LibraryTrack(title = mp3Row.title, artists = mp3Row.artists, metadataJson = mp3Row.rawJson)
        assertEquals(TrackAudioQuality.Lossy, trackAudioQuality(lossy))
    }
}
