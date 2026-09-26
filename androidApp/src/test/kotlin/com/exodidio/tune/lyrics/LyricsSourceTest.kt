package com.exodidio.tune.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsSourceTest {
    @Test
    fun defaultSourceIsAutoFetch() {
        assertEquals(LyricsSource.AutoFetch, LyricsSettings().preferredSource)
        assertEquals(LyricsSource.AutoFetch, LyricsSource.fromStorage(null))
        assertEquals(LyricsSource.AutoFetch, LyricsSource.fromStorage("desktop"))
        assertEquals(LyricsSource.AutoFetch, LyricsSource.fromStorage("auto_fetch"))
    }

    @Test
    fun preferredLyricsReturnsProviderLyrics() {
        assertEquals("provider", preferredLyrics("provider"))
        assertNull(preferredLyrics(null))
    }
}
