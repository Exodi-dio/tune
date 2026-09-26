package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellPanelStateTest {
    @Test
    fun `stored panel restores per track place`() {
        assertEquals(PlayerShellPanel.LYRICS, restoreShellPanel("LYRICS"))
        assertEquals(PlayerShellPanel.QUEUE, restoreShellPanel("QUEUE"))
        assertNull(restoreShellPanel("MAIN"))
        assertNull(restoreShellPanel(null))
    }
    @Test
    fun `storage round-trips`() {
        assertEquals("LYRICS", shellPanelStorageKey(PlayerShellPanel.LYRICS))
        assertEquals("QUEUE", shellPanelStorageKey(PlayerShellPanel.QUEUE))
        assertEquals("MAIN", shellPanelStorageKey(null))
    }
    @Test
    fun `back closes panel before player`() {
        assertEquals(null, nextShellPanelOnBack(PlayerShellPanel.LYRICS))
        assertEquals(null, nextShellPanelOnBack(PlayerShellPanel.QUEUE))
        assertEquals(null, nextShellPanelOnBack(null))
    }
    @Test
    fun `prewarm staggers expensive mounts`() {
        assertTrue(prewarmStageFor(null, false, 1))
        assertFalse(prewarmStageFor(PlayerShellPanel.LYRICS, false, 1))
        assertTrue(prewarmStageFor(PlayerShellPanel.LYRICS, false, 2))
        assertTrue(prewarmStageFor(PlayerShellPanel.LYRICS, true, 0))
        assertTrue(prewarmStageFor(PlayerShellPanel.QUEUE, false, 3))
    }
}
