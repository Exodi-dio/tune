package com.exodidio.tune.ui.navigation

import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueTransportAvailabilityTest {
    @Test
    fun keepsBoundaryTransportEnabledWhenRepeatIsOff() {
        val first = PlaybackQueueSnapshot(activeTrackIds = listOf("one", "two"), currentIndex = 0)
        val last = first.copy(currentIndex = 1)

        assertTrue(first.canNavigatePrevious())
        assertTrue(first.canNavigateNext())
        assertTrue(last.canNavigatePrevious())
        assertTrue(last.canNavigateNext())
    }

    @Test
    fun keepsBoundaryTransportEnabledWhenRepeatIsOn() {
        val queue = PlaybackQueueSnapshot(
            activeTrackIds = listOf("one", "two"),
            currentIndex = 0,
            repeatMode = RepeatMode.All,
        )

        assertTrue(queue.canNavigatePrevious())
        assertTrue(queue.copy(currentIndex = 1).canNavigateNext())
    }

    @Test
    fun dispatchesBoundarySwipesAndRejectsUnavailableTransport() {
        assertTrue(canDispatchQueueSwipe(swipeDirection = 1, canNavigatePrevious = true, canNavigateNext = true))
        assertTrue(canDispatchQueueSwipe(swipeDirection = -1, canNavigatePrevious = true, canNavigateNext = true))
        assertFalse(canDispatchQueueSwipe(swipeDirection = 1, canNavigatePrevious = false, canNavigateNext = true))
        assertFalse(canDispatchQueueSwipe(swipeDirection = -1, canNavigatePrevious = true, canNavigateNext = false))
    }
}
