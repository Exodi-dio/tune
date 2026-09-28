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

    @Test
    fun floatDurationsGroupWithTolerance() {
        val tracks = listOf(
            track("a", metadataJson = """{"duration":"200.5"}"""),
            track("b", metadataJson = """{"duration":201}"""),
            track("c", metadataJson = """{"duration":209.7}"""),
        )
        val groups = groupDuplicateTracks(tracks)
        assertEquals(1, groups.size)
        assertEquals(setOf("a", "b"), groups.single().trackIds.toSet())
    }

    @Test
    fun keepBestRemovalReturnsOnlyOtherIdsWithoutSideEffects() {
        val keep = track("keep", metadataJson = """{"duration":200,"codec":"flac"}""")
        val dropA = track("dropA", metadataJson = """{"duration":200,"codec":"mp3"}""")
        val dropB = track("dropB", metadataJson = """{"duration":200,"codec":"mp3"}""")
        val group = DuplicateGroup("k", listOf("dropA", "keep", "dropB"))
        val byId = mapOf(keep.id to keep, dropA.id to dropA, dropB.id to dropB)
        val removalIds = keepBestRemovalIds(group, byId)
        assertEquals(setOf("dropA", "dropB"), removalIds.toSet())
        // Purity pin: keep-best computes ids only; inputs are untouched and no file state exists.
        assertEquals(listOf("dropA", "keep", "dropB"), group.trackIds)
        assertEquals(3, byId.size)
    }

    @Test
    fun keepBestRemovalUnknownKeepIsEmpty() {
        assertTrue(keepBestRemovalIds(DuplicateGroup("k", listOf("missing")), emptyMap()).isEmpty())
    }

    @Test
    fun deleteRequestCollectsOnlyContentUris() {
        val local = track("local", metadataJson = """{"duration":200}""")
            .copy(audioPath = "content://media/external/audio/media/42")
        val file = track("file", metadataJson = """{"duration":200}""")
            .copy(audioPath = "/storage/music/song.mp3")
        val none = track("none", metadataJson = """{"duration":200}""")
        assertEquals(
            listOf("content://media/external/audio/media/42"),
            deviceDeleteUris(listOf(local, file, none)),
        )
    }

    @Test
    fun deleteDeniedIsNoopDeleteGrantedPassesThrough() {
        assertTrue(deleteConfirmedTrackIds(listOf("a", "b"), granted = false).isEmpty())
        assertEquals(listOf("a", "b"), deleteConfirmedTrackIds(listOf("a", "b"), granted = true))
    }
}
