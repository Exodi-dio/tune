package com.exodidio.tune.player

import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerAutoplayEngineTest {
    private fun track(id: String, artists: String, albumId: String = "al-$id", playCount: Int = 0): LibraryTrack =
        LibraryTrack(id = id, title = "Title $id", artists = artists, album = "Album $albumId", albumId = albumId, playCount = playCount)

    @Test
    fun picksSameArtistBeforeSameGenreBeforeFillAndCapsAtTen() {
        val current = track("cur", "Adele")
        val library = listOf(
            track("a1", "Adele"),
            track("a2", "Adele"),
            track("g1", "Other", playCount = 50),
            track("f1", "Far", playCount = 5),
            track("f2", "Far", playCount = 4),
            track("cur", "Adele")
        )
        val picked = pickAutoplay(library, current, recentIds = emptySet(), limit = 10)
        assertTrue(picked.indexOf("a1") < picked.indexOf("g1"))
        assertTrue(picked.indexOf("g1") < picked.indexOf("f1") || !picked.contains("g1") || picked.contains("f1"))
        assertFalse(picked.contains("cur"))
        assertTrue(picked.size <= 10)
    }

    @Test
    fun excludesRecentIdsAndNeverRepeatsCurrent() {
        val current = track("cur", "Adele")
        val library = listOf(track("a1", "Adele"), track("a2", "Adele"), track("cur", "Adele"))
        val picked = pickAutoplay(library, current, recentIds = setOf("a1"), limit = 10)
        assertFalse(picked.contains("a1"))
        assertFalse(picked.contains("cur"))
        assertEquals(listOf("a2"), picked)
    }

    @Test
    fun appendsUpToTenWithoutTouchingUserOrder() {
        val userOrder = listOf("now", "u1", "u2")
        val picked = pickAutoplay(
            library = (1..20).map { track("s$it", "Same Artist", playCount = it) } + track("now", "Same Artist"),
            currentTrack = track("now", "Same Artist"),
            recentIds = emptySet(),
            limit = 10
        )
        assertEquals(10, picked.size)
        val appended = userOrder + picked
        assertEquals(listOf("now", "u1", "u2"), appended.take(3))
        assertEquals(userOrder.size + 10, appended.size)
    }

    @Test
    fun repeatOneSuppressesAutoplayRefill() {
        assertTrue(shouldSuppressAutoplay(RepeatMode.One))
        assertFalse(shouldSuppressAutoplay(RepeatMode.Off))
        assertFalse(shouldSuppressAutoplay(RepeatMode.All))
        assertFalse(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.One, autoplayEnabled = true))
        assertFalse(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.Off, autoplayEnabled = false))
        assertTrue(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        assertTrue(shouldRefillAutoplay(upcomingCount = 1, repeatMode = RepeatMode.All, autoplayEnabled = true))
    }

    @Test
    fun refillTriggersOnlyWhenQueueDrainsTowardEmpty() {
        assertTrue(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        assertTrue(shouldRefillAutoplay(upcomingCount = 1, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        assertFalse(shouldRefillAutoplay(upcomingCount = 2, repeatMode = RepeatMode.Off, autoplayEnabled = true))
    }

    @Test
    fun malformedGenreMetadataIsIgnoredNeverThrows() {
        val bad = "{\"genres\":[{\"name\":{}}],\"raw_genre_names\":[null,42]}"
        val current = track("cur", "Adele").copy(metadataJson = bad)
        val library = listOf(
            track("a1", "Adele").copy(metadataJson = bad),
            track("f1", "Far"),
            current
        )
        val picked = pickAutoplay(library, current, recentIds = emptySet(), limit = 10)
        assertFalse(picked.contains("cur"))
        assertTrue(picked.contains("a1"))
    }
}
