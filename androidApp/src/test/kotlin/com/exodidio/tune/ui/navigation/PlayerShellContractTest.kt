package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellContractTest {
    @Test
    fun `phone width fills window and offers full bleed`() {
        assertTrue(playerFillsWindow(360))
        assertTrue(fullBleedArtworkAvailable(360))
    }
    @Test
    fun `wide phone loses full bleed but tablet regains availability`() {
        assertFalse(playerFillsWindow(800))
        assertTrue(tabletSizedPlayer(800))
        assertTrue(fullBleedArtworkAvailable(800))
    }
    @Test
    fun `mid width is neither phone nor tablet`() {
        assertFalse(playerFillsWindow(650))
        assertFalse(tabletSizedPlayer(650))
        assertFalse(fullBleedArtworkAvailable(650))
    }
    @Test
    fun `panel enum bridges to legacy without loss`() {
        assertTrue(PlayerShellPanel.LYRICS.toLegacy() == FullScreenPlayerPanel.Lyrics)
        assertTrue(PlayerShellPanel.QUEUE.toLegacy() == FullScreenPlayerPanel.Queue)
        assertTrue(FullScreenPlayerPanel.Lyrics.toShell() == PlayerShellPanel.LYRICS)
        assertTrue(FullScreenPlayerPanel.Queue.toShell() == PlayerShellPanel.QUEUE)
    }
}

