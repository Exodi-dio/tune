package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueSectionsTest {
    @Test
    fun partitionsUpcomingByMembershipWithRemainderAsContext() {
        val sections = splitQueue(
            activeTrackIds = listOf("now", "u1", "c1", "a1", "u2"),
            currentIndex = 0,
            userIds = setOf("u1", "u2"),
            autoplayIds = setOf("a1")
        )
        assertEquals("now", sections.nowPlayingId)
        assertEquals(listOf("u1", "u2"), sections.userIds)
        assertEquals(listOf("c1"), sections.contextIds)
        assertEquals(listOf("a1"), sections.autoplayIds)
    }

    @Test
    fun legacyQueueWithAllUpcomingAsUserHasEmptyContextAndAutoplay() {
        val active = listOf("now", "n1", "n2")
        val sections = splitQueue(
            activeTrackIds = active,
            currentIndex = 0,
            userIds = active.drop(1).toSet(),
            autoplayIds = emptySet()
        )
        assertEquals(listOf("n1", "n2"), sections.userIds)
        assertEquals(emptyList<String>(), sections.contextIds)
        assertEquals(emptyList<String>(), sections.autoplayIds)
    }

    @Test
    fun emptyQueueHasNoSectionsOrHeader() {
        val sections = splitQueue(
            activeTrackIds = emptyList(),
            currentIndex = -1,
            userIds = emptySet(),
            autoplayIds = emptySet()
        )
        assertEquals(null, sections.nowPlayingId)
        assertEquals(emptyList<String>(), sections.userIds)
        assertEquals(emptyList<String>(), sections.contextIds)
        assertEquals(emptyList<String>(), sections.autoplayIds)
        assertFalse(shouldShowAutoplayHeader(sections.autoplayIds, autoplayEnabled = false))
    }

    @Test
    fun autoplayHeaderVisibleWhenEnabledOrNonEmpty() {
        assertTrue(shouldShowAutoplayHeader(listOf("a1"), autoplayEnabled = false))
        assertTrue(shouldShowAutoplayHeader(emptyList(), autoplayEnabled = true))
        assertFalse(shouldShowAutoplayHeader(emptyList(), autoplayEnabled = false))
    }

    @Test
    fun outOfRangeCurrentIndexYieldsEmptySections() {
        val sections = splitQueue(
            activeTrackIds = listOf("a", "b"),
            currentIndex = 7,
            userIds = setOf("b"),
            autoplayIds = emptySet()
        )
        assertEquals(null, sections.nowPlayingId)
        assertEquals(emptyList<String>(), sections.userIds)
    }
}
