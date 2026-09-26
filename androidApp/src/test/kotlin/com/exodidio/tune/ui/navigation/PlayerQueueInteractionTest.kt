package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueInteractionTest {
    @Test
    fun reordersWithinSectionWithoutMovingOtherSections() {
        val full = listOf("now", "u1", "u2", "u3", "a1")
        val reordered = reorderWithinSection(full, sectionIds = listOf("u1", "u2", "u3"), fromInSection = 0, toInSection = 2)
        assertEquals(listOf("now", "u2", "u3", "u1", "a1"), reordered)
    }

    @Test
    fun invalidSectionMovesLeaveFullOrderUntouched() {
        val full = listOf("now", "u1", "u2")
        assertEquals(full, reorderWithinSection(full, listOf("u1", "u2"), fromInSection = 0, toInSection = 0))
        assertEquals(full, reorderWithinSection(full, listOf("u1", "u2"), fromInSection = -1, toInSection = 1))
    }

    @Test
    fun clearNextKeepsNowPlayingAndAutoplayOnly() {
        val cleared = clearNextInQueue(
            activeTrackIds = listOf("now", "u1", "c1", "a1"),
            currentIndex = 0,
            autoplayIds = setOf("a1")
        )
        assertEquals(listOf("now", "a1"), cleared)
    }

    @Test
    fun edgeScrollSpeedRampsAtEdgesAndRestsInMiddle() {
        assertTrue(edgeScrollSpeed(top = 0f, bottom = 50f, viewportStart = 0, viewportEnd = 600, zone = 80f, speed = 680f) < 0f)
        assertTrue(edgeScrollSpeed(top = 550f, bottom = 600f, viewportStart = 0, viewportEnd = 600, zone = 80f, speed = 680f) > 0f)
        assertEquals(0f, edgeScrollSpeed(top = 250f, bottom = 300f, viewportStart = 0, viewportEnd = 600, zone = 80f, speed = 680f))
        assertEquals(0f, edgeScrollSpeed(top = 0f, bottom = 50f, viewportStart = 0, viewportEnd = 600, zone = 0f, speed = 680f))
    }

    @Test
    fun scrollResetsOnlyOnTrackChange() {
        assertTrue(shouldResetQueueScroll(previousTrackId = "a", currentTrackId = "b"))
        assertFalse(shouldResetQueueScroll(previousTrackId = "a", currentTrackId = "a"))
        assertFalse(shouldResetQueueScroll(previousTrackId = null, currentTrackId = null))
    }
}
