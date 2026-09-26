package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellBackdropTest {
    @Test
    fun `full bleed needs setting on and phone-narrow window`() {
        assertTrue(isFullBleedEnabled(true, 360))
        assertFalse(isFullBleedEnabled(false, 360))
        assertFalse(isFullBleedEnabled(true, 650))
    }
    @Test
    fun `lyrics controls hide only when idle and untouched`() {
        assertTrue(shouldAutoHideLyricsControls(true, true, false, false))
        assertFalse(shouldAutoHideLyricsControls(true, true, true, false))
        assertFalse(shouldAutoHideLyricsControls(true, true, false, true))
        assertFalse(shouldAutoHideLyricsControls(false, true, false, false))
    }
}
