package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// W1: the shell host mirrors the old FullScreenPlayer confirmation path —
// once playback position reaches the requested seek target, the shared
// pending-seek holder clears via holder.confirmSeek().
class PlayerShellSeekConfirmTest {
    @Test
    fun seekConfirmationDetectedNearTarget() {
        assertTrue(hasConfirmedLyricsSeek(pendingPositionMs = 15_000L, currentPositionMs = 15_150L, durationMs = 60_000L))
        assertFalse(hasConfirmedLyricsSeek(pendingPositionMs = 15_000L, currentPositionMs = 12_000L, durationMs = 60_000L))
        assertFalse(hasConfirmedLyricsSeek(pendingPositionMs = null, currentPositionMs = 15_000L, durationMs = 60_000L))
        assertFalse(hasConfirmedLyricsSeek(pendingPositionMs = 15_000L, currentPositionMs = 15_150L, durationMs = 0L))
    }

    @Test
    fun confirmSeekClearsPendingLyricsSeek() {
        val holder = ShellLyricsSeekHolder()
        holder.requestSeek(15_000L)
        assertEquals(15_000L, displayedLyricsPositionMs(0L, holder.pendingPositionMs))
        if (hasConfirmedLyricsSeek(holder.pendingPositionMs, 15_150L, 60_000L)) holder.confirmSeek()
        assertNull(holder.pendingPositionMs)
    }
}
