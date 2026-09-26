package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellGesturesTest {
    @Test
    fun `horizontal fling past threshold dispatches`() {
        assertTrue(shouldDispatchShellSwipe(80f, 10f, 72f, 2.0f, 1.2f, 28f))
    }
    @Test
    fun `vertical-dominant drag never dispatches`() {
        assertFalse(shouldDispatchShellSwipe(40f, 60f, 72f, 2.0f, 1.2f, 28f))
    }
    @Test
    fun `fast short fling dispatches via velocity minimum`() {
        assertTrue(shouldDispatchShellSwipe(30f, 5f, 72f, 1.5f, 1.2f, 28f))
    }
    @Test
    fun `queue owns collapse while dragging`() {
        assertEquals(0.4f, shellCollapseProgress(true, false, 0.4f, 0.0f, true), 0.0001f)
        assertEquals(1.0f, shellCollapseProgress(false, true, 0.0f, 1.0f, false), 0.0001f)
        assertEquals(0.0f, shellCollapseProgress(false, false, 0.0f, 0.0f, false), 0.0001f)
    }
    @Test
    fun `carry fraction and flick decide queue release`() {
        assertTrue(shouldCarryQueueOpen(0.31f, 0f))
        assertFalse(shouldCarryQueueOpen(0.1f, 0f))
        assertTrue(shouldCarryQueueOpen(0.05f, 500f))
    }
}
