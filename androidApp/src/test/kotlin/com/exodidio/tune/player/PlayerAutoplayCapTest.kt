package com.exodidio.tune.ui.navigation

import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.player.autoplayRefillIds
import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertTrue
import org.junit.Test

// Fix wave (C4): pins the autoplay queue-size bound. Refills stop once the
// active queue reaches MaxAutoplayQueueSize, even with Repeat.All and a low
// upcoming tail; all other trigger behavior is identical.
class PlayerAutoplayCapTest {
    private fun track(id: String): LibraryTrack =
        LibraryTrack(id = id, title = "Title $id", artists = "Same Artist")

    @Test
    fun refillSkippedOnceQueueReachesCap() {
        val current = track("now")
        val library = listOf(current) + List(210) { track("lib-$it") }
        val atCap = PlaybackQueueSnapshot(
            activeTrackIds = listOf("now") + List(199) { "q-$it" },
            currentIndex = 199,
            repeatMode = RepeatMode.All,
        )
        assertTrue(autoplayRefillIds(atCap, library, current).isEmpty())
    }

    @Test
    fun refillProceedsBelowCap() {
        val current = track("now")
        val library = listOf(current, track("next"))
        val belowCap = PlaybackQueueSnapshot(
            activeTrackIds = listOf("now"),
            currentIndex = 0,
            repeatMode = RepeatMode.All,
        )
        assertTrue(autoplayRefillIds(belowCap, library, current).isNotEmpty())
    }
}
