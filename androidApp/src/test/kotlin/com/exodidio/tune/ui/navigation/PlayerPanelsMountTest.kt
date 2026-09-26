package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import com.exodidio.tune.player.shouldRefillAutoplay
import com.exodidio.tune.player.RepeatMode

class PlayerPanelsMountTest {
    @Test
    fun hostStartsWithBothPanelsClosed() {
        val host = PlayerPanelsHostState(selectedPanel = null)
        assertEquals(null, host.selectedPanel)
    }

    @Test
    fun autoplayTriggerUsesAppendOnlyIdsFromPicker() {
        val active = listOf("now", "u1")
        val upcoming = active.size - 0 - 1
        assertEquals(true, shouldRefillAutoplay(upcomingCount = upcoming, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        val sections = splitQueue(activeTrackIds = active, currentIndex = 0, userIds = setOf("u1"), autoplayIds = emptySet())
        assertEquals(listOf("u1"), sections.userIds)
        assertEquals(emptyList<String>(), sections.autoplayIds)
    }

    @Test
    fun emptyQueueMountShowsNoAutoplayHeader() {
        val sections = splitQueue(activeTrackIds = emptyList(), currentIndex = -1, userIds = emptySet(), autoplayIds = emptySet())
        assertEquals(null, sections.nowPlayingId)
        assertFalse(shouldShowAutoplayHeader(sections.autoplayIds, autoplayEnabled = false))
    }
}
