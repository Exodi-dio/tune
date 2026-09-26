package com.exodidio.tune.ui.navigation

import androidx.compose.ui.geometry.RoundRect
import org.junit.Assert.assertEquals
import org.junit.Test

class NavBarMaskMathTest {
    @Test
    fun pillRectMatchesOffsetAndWidth() {
        val rect: RoundRect = navPillRectPx(offsetPx = 100f, widthPx = 80f, heightPx = 56f, radiusPx = 32f)
        assertEquals(100f, rect.left, 0f)
        assertEquals(180f, rect.right, 0f)
        assertEquals(0f, rect.top, 0f)
        assertEquals(56f, rect.bottom, 0f)
    }

    @Test
    fun zeroOffsetStartsAtZero() {
        val rect = navPillRectPx(0f, 80f, 56f, 32f)
        assertEquals(0f, rect.left, 0f)
        assertEquals(80f, rect.right, 0f)
    }
}
