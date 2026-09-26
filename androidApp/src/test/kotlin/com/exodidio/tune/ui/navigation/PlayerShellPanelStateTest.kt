package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

// F5 scope cut: restore/prewarm helpers deleted (see PlayerShellPanelState.kt);
// back ordering is the retained contract.
class PlayerShellPanelStateTest {
    @Test
    fun `back closes panel before player`() {
        assertEquals(null, nextShellPanelOnBack(PlayerShellPanel.LYRICS))
        assertEquals(null, nextShellPanelOnBack(PlayerShellPanel.QUEUE))
        assertEquals(null, nextShellPanelOnBack(null))
    }
}

