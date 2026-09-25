package com.exodidio.tune.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ReduceMotionTest {
    @Test
    fun marqueeStaticWhenReduced() {
        assertEquals(false, shouldAnimateMarquee(travelPx = 200, reduceMotion = true))
        assertEquals(true, shouldAnimateMarquee(travelPx = 200, reduceMotion = false))
        assertEquals(false, shouldAnimateMarquee(travelPx = 0, reduceMotion = false))
    }

    @Test
    fun indicatorStaticWhenPausedOrReduced() {
        assertEquals(false, shouldAnimateIndicator(isPlaying = false, reduceMotion = false))
        assertEquals(false, shouldAnimateIndicator(isPlaying = true, reduceMotion = true))
        assertEquals(true, shouldAnimateIndicator(isPlaying = true, reduceMotion = false))
    }
}
