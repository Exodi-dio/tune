package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Fix wave (C6): pins the scrubber end-to-end path — finish emits BOTH the
// lyrics seek request and onSeek with the same target, and the pending lyrics
// seek clears once playback confirms it.
class PlayerShellScrubberSeekTest {
    @Test
    fun finishEmitsBothSeekCallbacksWithSameTarget() {
        val order = mutableListOf<String>()
        var requested: Long? = null
        var sought: Long? = null
        val target = finishShellScrubberSeek(
            durationMs = 60_000L,
            fraction = 0.25f,
            onSeekRequested = { requested = it; order += "requested" },
            onSeek = { sought = it; order += "seek" },
        )
        assertEquals(15_000L, target)
        assertEquals(15_000L, requested)
        assertEquals(15_000L, sought)
        assertEquals(listOf("requested", "seek"), order)
    }

    @Test
    fun finishReturnsNullWithoutSeekingWhenNothingToSeek() {
        var calls = 0
        assertNull(finishShellScrubberSeek(60_000L, null, { calls++ }, { calls++ }))
        assertNull(finishShellScrubberSeek(0L, 0.25f, { calls++ }, { calls++ }))
        assertEquals(0, calls)
    }

    @Test
    fun pendingLyricsSeekClearsOnConfirm() {
        val holder = ShellLyricsSeekHolder()
        // Scrubber finish wires request + seek together.
        val target = finishShellScrubberSeek(
            durationMs = 60_000L,
            fraction = 0.25f,
            onSeekRequested = { holder.requestSeek(it) },
            onSeek = {},
        )
        assertEquals(15_000L, target)
        // Playback advances to the target: the host confirm path clears it.
        if (hasConfirmedLyricsSeek(holder.pendingPositionMs, 15_150L, 60_000L)) holder.confirmSeek()
        assertNull(holder.pendingPositionMs)
        assertTrue(hasConfirmedLyricsSeek(15_000L, 15_150L, 60_000L))
    }
}
