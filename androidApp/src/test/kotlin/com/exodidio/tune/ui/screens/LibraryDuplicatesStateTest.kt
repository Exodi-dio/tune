package com.exodidio.tune.ui.screens

import com.exodidio.tune.library.keepBestRemovalIds
import com.exodidio.tune.library.DuplicateGroup
import com.exodidio.tune.library.pendingDeleteTrackIds
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryDuplicatesStateTest {
    private fun track(
        id: String,
        metadataJson: String = """{"duration":200}""",
        audioPath: String? = null,
    ) = LibraryTrack(id = id, title = "Song", artists = "Artist", metadataJson = metadataJson, audioPath = audioPath)

    @Test
    fun groupsMapAndDismissedAndHiddenAreExcluded() {
        val tracks = listOf(track("a"), track("b"), track("c", metadataJson = """{"duration":300}"""), track("d", metadataJson = """{"duration":300}"""))
        val visible = duplicatesUiStateFor(tracks, hiddenTrackIds = emptySet(), dismissedKeys = emptySet())
        assertEquals(2, visible.groups.size)

        val dismissedKey = visible.groups.first().key
        val afterDismiss = duplicatesUiStateFor(tracks, hiddenTrackIds = emptySet(), dismissedKeys = setOf(dismissedKey))
        assertEquals(1, afterDismiss.groups.size)
        assertTrue(afterDismiss.groups.none { it.key == dismissedKey })

        val afterHide = duplicatesUiStateFor(tracks, hiddenTrackIds = setOf("a"), dismissedKeys = emptySet())
        assertTrue(afterHide.groups.none { group -> "a" in group.trackIds })
    }

    @Test
    fun pendingIdsOnlyCoverUriEligibleTracks() {
        val eligible = track("ok", audioPath = "content://media/audio/1")
        val filePath = track("file", audioPath = "/storage/song.mp3")
        val noPath = track("none")
        assertEquals(listOf("ok"), pendingDeleteTrackIds(listOf(eligible, filePath, noPath)))
    }

    @Test
    fun removedEntriesLeaveVisibleTrackList() {
        val tracks = listOf(track("a"), track("b"))
        val visible = visibleTracksAfterRemoval(tracks, removedIds = setOf("a"))
        assertEquals(listOf("b"), visible.map { it.id })
    }

    @Test
    fun keepBestUsesRetainedEngineKey() {
        val lossless = track("lossless", metadataJson = """{"duration":200,"codec":"flac"}""")
        val lossy = track("lossy", metadataJson = """{"duration":200,"codec":"mp3"}""")
        val state = duplicatesUiStateFor(listOf(lossless, lossy), emptySet(), emptySet())
        val group = state.groups.single()
        // Engine key retained in UI state (not reconstructed from the UI key).
        val removalIds = keepBestRemovalIds(
            DuplicateGroup(group.engineKey, group.trackIds),
            listOf(lossless, lossy).associateBy(LibraryTrack::id),
        )
        assertEquals(listOf("lossy"), removalIds)
    }

    @Test
    fun keepBestSurfacesKeepIdPerGroup() {
        val lossless = track("lossless", metadataJson = """{"duration":200,"codec":"flac"}""")
        val lossy = track("lossy", metadataJson = """{"duration":200,"codec":"mp3"}""")
        val state = duplicatesUiStateFor(listOf(lossless, lossy), emptySet(), emptySet())
        assertEquals("lossless", state.groups.single().keepId)
    }

    @Test
    fun keepBestTapRemovalIdsEqualLoserIdsForCallback() {
        // Simulates exactly what the Keep-best tap forwards to
        // onTracksRemovedFromLibrary: the callback payload must be the loser ids.
        val lossless = track("lossless", metadataJson = """{"duration":200,"codec":"flac"}""")
        val lossy = track("lossy", metadataJson = """{"duration":200,"codec":"mp3"}""")
        val tracks = listOf(lossless, lossy)
        val group = duplicatesUiStateFor(tracks, emptySet(), emptySet()).groups.single()
        val removalIds = keepBestTapRemovalIds(group, tracks.associateBy(LibraryTrack::id))
        assertEquals(listOf("lossy"), removalIds)
        // Populating App-level removedTrackIds with the payload hides losers everywhere.
        assertEquals(listOf("lossless"), visibleTracksAfterRemoval(tracks, removalIds.toSet()).map { it.id })
    }

    @Test
    fun queueReanchorsToNextValidTrackWhenPlayingTrackRemoved() {
        val queue = PlaybackQueueSnapshot(
            originalTrackIds = listOf("a", "b", "c"),
            activeTrackIds = listOf("a", "b", "c"),
            currentIndex = 1,
        )
        val visible = visiblePlaybackQueueAfterRemoval(queue, removedIds = setOf("b"))
        assertEquals(listOf("a", "c"), visible.activeTrackIds)
        assertEquals(listOf("a", "c"), visible.originalTrackIds)
        // Re-anchored to the next valid track (c slid into index 1) — never -1.
        assertEquals(1, visible.currentIndex)
        assertEquals("c", visible.currentTrackId)
    }

    @Test
    fun queueKeepsCurrentTrackWhenUnrelatedTrackRemoved() {
        val queue = PlaybackQueueSnapshot(
            originalTrackIds = listOf("a", "b", "c"),
            activeTrackIds = listOf("a", "b", "c"),
            currentIndex = 0,
        )
        val visible = visiblePlaybackQueueAfterRemoval(queue, removedIds = setOf("c"))
        assertEquals(0, visible.currentIndex)
        assertEquals("a", visible.currentTrackId)
    }

    @Test
    fun queueEmptyAfterRemovalIsCleanPausedState() {
        val queue = PlaybackQueueSnapshot(
            originalTrackIds = listOf("a"),
            activeTrackIds = listOf("a"),
            currentIndex = 0,
        )
        val visible = visiblePlaybackQueueAfterRemoval(queue, removedIds = setOf("a"))
        assertTrue(visible.activeTrackIds.isEmpty())
        assertEquals(-1, visible.currentIndex)
    }
}