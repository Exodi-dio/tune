package com.exodidio.tune.ui.components

import com.exodidio.tune.ui.screens.playlistPickerSampleSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistArtworkDecodeTest {
    @Test
    fun sampleSizeCapsTwelveMpTo336() {
        // 4000x3000 (12 MP) capped to 336 px target: sample 8 -> 500x375.
        assertEquals(8, playlistPickerSampleSize(4000, 3000, 336))
    }

    @Test
    fun sampleSizeIsOneForSmall() {
        assertEquals(1, playlistPickerSampleSize(300, 300, 336))
    }

    @Test
    fun sharedKeyFormat() {
        assertEquals("content://x:336", ArtworkThumbnailCache.cacheKey("content://x", 336))
    }
}
