package com.exodidio.tune.ui.screens

import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryDuplicatesStateTest {
    private fun track(id: String, metadataJson: String = """{"duration":200}""") =
        LibraryTrack(id = id, title = "Song", artists = "Artist", metadataJson = metadataJson)

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
    fun keepBestSurfacesKeepIdPerGroup() {
        val lossless = track("lossless", metadataJson = """{"duration":200,"codec":"flac"}""")
        val lossy = track("lossy", metadataJson = """{"duration":200,"codec":"mp3"}""")
        val state = duplicatesUiStateFor(listOf(lossless, lossy), emptySet(), emptySet())
        assertEquals("lossless", state.groups.single().keepId)
    }
}
