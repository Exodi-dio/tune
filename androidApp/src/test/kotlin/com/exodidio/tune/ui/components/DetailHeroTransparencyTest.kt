package com.exodidio.tune.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailHeroTransparencyTest {
    @Test
    fun reducedHasNoTranslucentLayers() {
        val reduced = heroBackdropColors(reduceTransparency = true)
        assertEquals(1f, reduced.topAlpha, 0f)
        assertEquals(0f, reduced.scrimAlpha, 0f)
        assertEquals(false, reduced.animate)
    }

    @Test
    fun fullKeepsGradient() {
        val full = heroBackdropColors(reduceTransparency = false)
        assertEquals(0.52f, full.topAlpha, 0f)
        assertEquals(0.28f, full.scrimAlpha, 0f)
        assertEquals(true, full.animate)
    }
}
