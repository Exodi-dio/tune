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

    // F1 (v0.3 UI fixes): height-aware sleeve math — minOf(width - 40dp,
    // height - reserve, 288dp), floored at the 80dp collapsed size.
    @Test
    fun `sleeve fits width with 40dp inset on tall screens`() {
        assertEquals(288f, sleeveExpandedSizeDp(maxWidthDp = 400f, maxHeightDp = 900f), 0.0001f)
    }

    @Test
    fun `sleeve caps by height reserve on short screens`() {
        assertEquals(152f, sleeveExpandedSizeDp(maxWidthDp = 400f, maxHeightDp = 600f), 0.0001f)
    }

    @Test
    fun `sleeve floors at collapsed size`() {
        assertEquals(80f, sleeveExpandedSizeDp(maxWidthDp = 100f, maxHeightDp = 200f), 0.0001f)
    }
}
