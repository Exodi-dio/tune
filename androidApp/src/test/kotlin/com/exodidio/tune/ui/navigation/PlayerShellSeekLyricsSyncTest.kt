package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Carried item C1: pins that a shell seek routed through the shared
// pending-seek holder moves the active lyrics line, and that confirming the
// seek clears the pending position back to live playback.
class PlayerShellSeekLyricsSyncTest {
    private val lines = listOf(
        PlayerLyricLine(primary = "first", timestampSeconds = 1f),
        PlayerLyricLine(primary = "second", timestampSeconds = 10f),
        PlayerLyricLine(primary = "third", timestampSeconds = 20f),
    )

    @Test
    fun `shell seek request moves the active lyrics line`() {
        val holder = ShellLyricsSeekHolder()
        val before = activeLyricIndex(lines, displayedLyricsPositionMs(0L, holder.pendingPositionMs))
        holder.requestSeek(15_000L)
        assertEquals(15_000L, displayedLyricsPositionMs(0L, holder.pendingPositionMs))
        val after = activeLyricIndex(lines, displayedLyricsPositionMs(0L, holder.pendingPositionMs))
        assertTrue(after > before)
    }

    @Test
    fun `confirmed seek clears the pending lyrics position`() {
        val holder = ShellLyricsSeekHolder()
        val firstRequest = holder.requestId
        holder.requestSeek(15_000L)
        assertTrue(holder.requestId > firstRequest)
        holder.confirmSeek()
        assertNull(holder.pendingPositionMs)
        assertEquals(-1, activeLyricIndex(lines, displayedLyricsPositionMs(0L, holder.pendingPositionMs)))
    }
}
