package com.exodidio.tune.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalLrcResolverTest {
    private val parsableBasename = "[00:01.00]Basename line\n[00:02.00]Second"
    private val parsableFallback = "[00:01.00]Fallback line"

    @Test
    fun basenameMatchWinsOverArtistTitleFallback() {
        val files = mapOf(
            "/music/song.lrc" to parsableBasename,
            "/music/Artist - Title.lrc" to parsableFallback,
        )
        val result = resolveLocalLrcContent("/music/song.mp3", "Artist", "Title") { files[it] }
        assertEquals(parsableBasename, result)
    }

    @Test
    fun artistTitleFallbackUsedWhenBasenameMissing() {
        val files = mapOf(
            "/music/Artist - Title.lrc" to parsableFallback,
        )
        val result = resolveLocalLrcContent("/music/song.mp3", "Artist", "Title") { files[it] }
        assertEquals(parsableFallback, result)
    }

    @Test
    fun ttmlSiblingUsedWhenNoLrcPresent() {
        val ttml = "<tt><body><div><p begin=\"00:00:01.000\">Hello</p></div></body></tt>"
        val files = mapOf("/music/song.ttml" to ttml)
        val result = resolveLocalLrcContent("/music/song.mp3", "Artist", "Title") { files[it] }
        assertEquals(ttml, result)
    }

    @Test
    fun lrcStillPreferredOverTtml() {
        val ttml = "<tt><body><div><p begin=\"00:00:01.000\">Hello</p></div></body></tt>"
        val files = mapOf(
            "/music/song.lrc" to parsableBasename,
            "/music/song.ttml" to ttml,
        )
        val result = resolveLocalLrcContent("/music/song.mp3", "Artist", "Title") { files[it] }
        assertEquals(parsableBasename, result)
    }

    @Test
    fun missingFilesResolveToNullSoCallerFallsBackOnline() {
        val result = resolveLocalLrcContent("/music/song.mp3", "Artist", "Title") { null }
        assertNull(result)
    }

    @Test
    fun unparsableBasenameFallsBackToNextCandidateOrOnline() {
        val files = mapOf(
            "/music/song.lrc" to "   \n  ",
            "/music/Artist - Title.lrc" to parsableFallback,
        )
        val result = resolveLocalLrcContent("/music/song.mp3", "Artist", "Title") { files[it] }
        assertEquals(parsableFallback, result)

        val allBad = mapOf("/music/song.lrc" to "   ")
        val none = resolveLocalLrcContent("/music/song.mp3", "Artist", "Title") { allBad[it] }
        assertNull(none)
    }
}
