package com.exodidio.tune.library

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalComposerGenreMetadataTest {
    private fun track(
        mediaId: Long = 7L,
        genre: String? = "Jazz",
        composer: String? = "Philip Glass",
    ) = LocalTrack(
        mediaId = mediaId,
        uri = "content://media/external/audio/media/$mediaId",
        title = "Song",
        artist = "Singer",
        album = "Record",
        albumArtist = null,
        genre = genre,
        composer = composer,
        year = 2023,
        trackNo = 1,
        discNo = 1,
        totalTracks = null,
        totalDiscs = null,
        durationMs = 200_000L,
        bitrate = 4_000_000,
        sizeBytes = 10_000_000L,
        dateAddedSec = 1_700_000_001L,
        mimeType = "audio/flac",
        label = null,
        copyright = null,
    )

    private fun tags(composer: String? = "Philip Glass") = RawTags(
        title = null,
        artist = null,
        album = null,
        albumArtist = null,
        genre = null,
        composer = composer,
        year = null,
        trackNo = null,
        discNo = null,
        durationMs = null,
        bitrate = null,
        sampleRateHz = null,
        bitDepth = null,
        hasEmbeddedArt = false,
    )

    private fun metadataRoot(track: LocalTrack) =
        Json.parseToJsonElement(buildLocalImport(listOf(track)).tracks[0].rawJson).jsonObject

    @Test fun metadataJsonEmitsStructuredGenres() {
        val genres = metadataRoot(track())["genres"]!!.jsonArray
        assertEquals(1, genres.size)
        assertEquals("Jazz", genres[0].jsonObject["name"]!!.jsonPrimitive.content)
        assertTrue(genres[0].jsonObject["id"]!!.jsonPrimitive.content.isNotBlank())
    }

    @Test fun metadataJsonEmitsStructuredComposers() {
        val composers = metadataRoot(track())["composers"]!!.jsonArray
        assertEquals(1, composers.size)
        assertEquals("Philip Glass", composers[0].jsonObject["name"]!!.jsonPrimitive.content)
        assertTrue(composers[0].jsonObject["id"]!!.jsonPrimitive.content.isNotBlank())
    }

    @Test fun genreNamesWithSeparatorsStayWhole() {
        val genres = metadataRoot(track(genre = "Electronic / Ambient"))["genres"]!!.jsonArray
        assertEquals(1, genres.size)
        assertEquals("Electronic / Ambient", genres[0].jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test fun composerNamesWithSeparatorsStayWhole() {
        val composers = metadataRoot(track(composer = "Lennon / McCartney"))["composers"]!!.jsonArray
        assertEquals(1, composers.size)
        assertEquals("Lennon / McCartney", composers[0].jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test fun composerAndGenreIdsAreStableAcrossTracks() {
        val a = metadataRoot(track(mediaId = 7L))
        val b = metadataRoot(track(mediaId = 8L))
        assertEquals(a["composers"].toString(), b["composers"].toString())
        assertEquals(a["genres"].toString(), b["genres"].toString())
    }

    @Test fun blankComposerAndGenreEmitEmptyArrays() {
        val root = metadataRoot(track(genre = null, composer = null))
        assertEquals("[]", root["genres"].toString())
        assertEquals("[]", root["composers"].toString())
    }

    @Test fun retrieverComposerTagFlowsIntoLocalTrack() {
        val base = track(composer = null)
        assertEquals("Philip Glass", base.applyTags(tags(composer = "Philip Glass")).composer)
    }

    @Test fun nullOrBlankComposerTagKeepsScanValue() {
        val base = track(composer = "Scan Composer")
        assertEquals("Scan Composer", base.applyTags(tags(composer = null)).composer)
        assertEquals("Scan Composer", base.applyTags(tags(composer = "  ")).composer)
    }

    @Test fun composerAndGenreIdHelpersAreStableAndPrefixed() {
        assertEquals(composerIdFor("Philip Glass"), composerIdFor("philip glass"))
        assertEquals(genreIdFor("Jazz"), genreIdFor("jazz"))
        assertTrue(composerIdFor("Philip Glass").startsWith("local-composer-"))
        assertTrue(genreIdFor("Jazz").startsWith("local-genre-"))
        assertNotEquals(composerIdFor("Philip Glass"), artistIdFor("Philip Glass"))
        assertNotEquals(genreIdFor("Philip Glass"), composerIdFor("Philip Glass"))
    }

    @Test fun composerEntryCarriesArtworkKeyWhenTrackHasArtwork() {
        val art = LocalArtwork(
            relativePath = "artwork-local/local-7.jpg",
            sha256 = "abc",
            sizeBytes = 10L,
            mime = "image/jpeg",
        )
        val result = buildLocalImport(listOf(track()), artwork = mapOf("local-7" to art))
        val composer = Json.parseToJsonElement(result.tracks[0].rawJson).jsonObject["composers"]!!.jsonArray[0].jsonObject
        assertEquals("artwork:local-7", composer["artwork_key"]!!.jsonPrimitive.content)
    }

    @Test fun composerEntryOmitsArtworkKeyWithoutTrackArtwork() {
        val composer = metadataRoot(track())["composers"]!!.jsonArray[0].jsonObject
        assertTrue(composer["artwork_key"] == null)
    }

    @Test fun searchDocumentsCoverComposers() {
        val result = buildLocalImport(listOf(track()))
        val byType = result.searchDocuments.groupBy { it.entityType }.mapValues { it.value.map { doc -> doc.entityId } }
        val composerId = metadataRoot(track())["composers"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content
        assertTrue((byType["composer"] ?: emptyList()).contains(composerId))
    }
}
