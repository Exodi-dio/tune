package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerShellNowPlayingTest {
    @Test
    fun `now playing order matches reference credits scrubber transport volume bottom row`() {
        assertEquals(
            listOf("credits", "scrubber", "transport", "volume", "bottomRow"),
            shellNowPlayingOrder(),
        )
    }
    @Test
    fun `pause shrinks sleeve and play restores`() {
        assertEquals(0.86f, shellArtworkScale(false), 0.0001f)
        assertEquals(1.0f, shellArtworkScale(true), 0.0001f)
    }
    @Test
    fun `panel open collapses sleeve progress to one`() {
        assertEquals(1.0f, shellCollapseProgress(true, false, 1.0f, 0.0f, false), 0.0001f)
    }
}
