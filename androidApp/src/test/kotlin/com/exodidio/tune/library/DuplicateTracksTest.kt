package com.exodidio.tune.library

import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateTracksTest {
    private fun track(
        id: String,
        title: String = "Song",
        artists: String = "Artist",
        metadataJson: String = """{"duration":200}""",
    ) = LibraryTrack(id = id, title = title, artists = artists, metadataJson = metadataJson)

    @Test
    fun exactDuplicatesGroup() {
        val tracks = listOf(
            track("a", metadataJson = """{"duration":200}"""),
            track("b", title = "  SONG ", artists = "artist ", metadataJson = """{"duration":200}"""),
        )
        val groups = groupDuplicateTracks(tracks)
        assertEquals(1, groups.size)
        assertEquals(setOf("a", "b"), groups.single().trackIds.toSet())
    }

    @Test
    fun twoSecondDifferenceGroupsButThreeSecondsSplit() {
        val tracks = listOf(
            track("a", metadataJson = """{"duration":200}"""),
            track("b", metadataJson = """{"duration":202}"""),
            track("c", metadataJson = """{"duration":205}"""),
        )
        val groups = groupDuplicateTracks(tracks)
        assertEquals(1, groups.size)
        assertEquals(listOf("a", "b"), groups.single().trackIds)
    }

    @Test
    fun differentArtistOrTitleSplitGroups() {
        val tracks = listOf(
            track("a", artists = "Artist One"),
            track("b", artists = "Artist Two"),
            track("c", title = "Other Song", artists = "Artist One"),
        )
        assertTrue(groupDuplicateTracks(tracks).isEmpty())
    }

    @Test
    fun durationLessTracksAreExcluded() {
        val tracks = listOf(
            track("a", metadataJson = """{"duration":200}"""),
            track("b", metadataJson = """{}"""),
            track("c", metadataJson = """{"duration":"unknown"}"""),
        )
        assertTrue(groupDuplicateTracks(tracks).isEmpty())
    }

    @Test
    fun keepBestPrefersLosslessThenBitrateThenSize() {
        val lossy = track("lossy", metadataJson = """{"duration":200,"codec":"mp3","bitrate":320,"size":8000}""")
        val lossless = track("lossless", metadataJson = """{"duration":200,"codec":"flac","bitrate":128,"size":1000}""")
        val byId = mapOf(lossy.id to lossy, lossless.id to lossless)
        assertEquals("lossless", chooseKeepBest(DuplicateGroup("k", listOf("lossy", "lossless")), byId))

        val low = track("low", metadataJson = """{"duration":200,"codec":"mp3","bitrate":128,"size":9000}""")
        val high = track("high", metadataJson = """{"duration":200,"codec":"mp3","bitrate":320,"size":1000}""")
        val byBitrate = mapOf(low.id to low, high.id to high)
        assertEquals("high", chooseKeepBest(DuplicateGroup("k", listOf("low", "high")), byBitrate))

        val small = track("small", metadataJson = """{"duration":200,"codec":"mp3","bitrate":320,"size":1000}""")
        val big = track("big", metadataJson = """{"duration":200,"codec":"mp3","bitrate":320,"size":9000}""")
        val bySize = mapOf(small.id to small, big.id to big)
        assertEquals("big", chooseKeepBest(DuplicateGroup("k", listOf("small", "big")), bySize))
    }

    @Test
    fun emptyLibraryProducesNoGroupsAndUnknownKeepIsNull() {
        assertTrue(groupDuplicateTracks(emptyList()).isEmpty())
        assertNull(chooseKeepBest(DuplicateGroup("k", listOf("missing")), emptyMap()))
    }
}
