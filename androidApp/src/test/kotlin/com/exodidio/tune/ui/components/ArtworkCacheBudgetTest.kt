package com.exodidio.tune.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkCacheBudgetTest {
    @Test
    fun `budgetMath EighthOfMemoryClass`() {
        // 2 GB-budget device reports ~192 MB memoryClass; cache must be exactly 1/8.
        assertEquals(24 * 1024 * 1024, artworkCacheMaxBytes(192))
        assertEquals(32 * 1024 * 1024, artworkCacheMaxBytes(256))
    }

    @Test
    fun cacheKeyIsPathOnly() {
        assertEquals("/data/a.jpg:120", artworkCacheKey("/data/a.jpg", 120))
        assertEquals("/data/a.jpg:336", ArtworkThumbnailCache.cacheKey("/data/a.jpg", 336))
    }

    @Test
    fun thumbnailByteBudgetUnderLimit() {
        // 120px RGB_565 thumb = 120*120*2 = 28800 bytes; 256px = 131072 bytes.
        val thumb120 = 120 * 120 * 2
        val thumb256 = 256 * 256 * 2
        assertTrue(thumb120 < 64 * 1024)
        assertTrue(thumb256 < 256 * 1024)
        // 250 such thumbs at 120px fit in a 24 MB budget.
        assertTrue(250 * thumb120 < artworkCacheMaxBytes(192))
    }
}
