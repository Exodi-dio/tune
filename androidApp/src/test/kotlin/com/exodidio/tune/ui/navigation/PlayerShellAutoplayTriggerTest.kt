package com.exodidio.tune.ui.navigation

import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.player.autoplayRefillIds
import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// W4: the shell autoplay trigger refills from the picker and appends only.
class PlayerShellAutoplayTriggerTest {
    private fun track(id: String, artists: String = "Same Artist"): LibraryTrack =
        LibraryTrack(id = id, title = "Title $id", artists = artists)

    @Test
    fun refillsWithPickerIdsWhenUpcomingRunsLow() {
        val current = track("now")
        val snapshot = PlaybackQueueSnapshot(activeTrackIds = listOf("now"), currentIndex = 0, repeatMode = RepeatMode.Off)
        assertEquals(listOf("next"), autoplayRefillIds(snapshot, listOf(current, track("next")), current))
    }

    @Test
    fun noRefillWhenUpcomingHealthy() {
        val current = track("now")
        val snapshot = PlaybackQueueSnapshot(activeTrackIds = listOf("now", "u1", "u2"), currentIndex = 0, repeatMode = RepeatMode.Off)
        assertTrue(autoplayRefillIds(snapshot, listOf(current, track("next")), current).isEmpty())
    }

    @Test
    fun noRefillWhenRepeatOneDisabledOrTrackUnknown() {
        val current = track("now")
        val low = PlaybackQueueSnapshot(activeTrackIds = listOf("now"), currentIndex = 0, repeatMode = RepeatMode.One)
        assertTrue(autoplayRefillIds(low, listOf(current, track("next")), current).isEmpty())
        val off = PlaybackQueueSnapshot(activeTrackIds = listOf("now"), currentIndex = 0, repeatMode = RepeatMode.Off)
        assertTrue(autoplayRefillIds(off, listOf(current, track("next")), current, autoplayEnabled = false).isEmpty())
        assertTrue(autoplayRefillIds(off, listOf(current, track("next")), null).isEmpty())
    }
}
