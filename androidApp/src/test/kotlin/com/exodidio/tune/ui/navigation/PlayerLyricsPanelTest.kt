package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLyricsPanelTest {
    private val lines = parsePlayerLyrics("[00:10.00]One\n[00:20.00]Two\n[00:30.00]Three")

    @Test
    fun activeLineIsLastTimestampAtOrBeforePosition() {
        assertEquals(-1, activeLyricIndex(lines, positionMs = 5_000L))
        assertEquals(0, activeLyricIndex(lines, positionMs = 10_000L))
        assertEquals(1, activeLyricIndex(lines, positionMs = 25_000L))
        assertEquals(2, activeLyricIndex(lines, positionMs = 30_000L))
    }

    @Test
    fun scrollLeadIsClampedTo350To500Ms() {
        assertEquals(350L, lyricsScrollLeadMs(gapMs = 0L))
        assertEquals(350L, lyricsScrollLeadMs(gapMs = 100L))
        assertEquals(420L, lyricsScrollLeadMs(gapMs = 420L))
        assertEquals(500L, lyricsScrollLeadMs(gapMs = 5_000L))
    }

    @Test
    fun tapLyricSeeksToTimestampPlusOffset() {
        assertEquals(62_500L, lyricsSeekTargetMs(timestampSeconds = 62.5f, offsetMs = 0))
        assertEquals(62_000L, applyLyricsOffset(positionMs = 62_500L, offsetMs = 500))
        assertEquals(63_000L, lyricsSeekTargetMs(timestampSeconds = 62.5f, offsetMs = 500))
        assertEquals(0L, applyLyricsOffset(positionMs = 100L, offsetMs = 500))
    }

    @Test
    fun browseSuspendsFollowUntilReplay() {
        assertEquals(true, shouldEnterLyricsBrowseMode(isUserDragging = true, isFollowingSelectedLine = false))
        assertEquals(false, shouldEnterLyricsBrowseMode(isUserDragging = true, isFollowingSelectedLine = true))
        assertEquals(true, shouldResetLyricsForReplay(previousPositionMs = 84_000L, currentPositionMs = 0L))
        assertEquals(72_000L, displayedLyricsPositionMs(playbackPositionMs = 12_000L, pendingSeekPositionMs = 72_000L))
    }

    // F1: toggle presence mirrors the deleted panel (allowed + current + supported).
    @Test
    fun romanizationToggleShowsOnlyWhenAllowedCurrentAndSupported() {
        assertEquals(true, shouldShowRomanizationToggle(romanizationAllowed = true, current = true, supported = true))
        assertEquals(false, shouldShowRomanizationToggle(romanizationAllowed = false, current = true, supported = true))
        assertEquals(false, shouldShowRomanizationToggle(romanizationAllowed = true, current = false, supported = true))
        assertEquals(false, shouldShowRomanizationToggle(romanizationAllowed = true, current = true, supported = false))
    }
}

