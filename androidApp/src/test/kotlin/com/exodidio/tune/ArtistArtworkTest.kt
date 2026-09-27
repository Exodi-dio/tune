package com.exodidio.tune

import com.exodidio.tune.sync.ArtistArtworkOperation
import com.exodidio.tune.sync.ArtistArtworkPayload
import com.exodidio.tune.sync.ArtistMutation
import com.exodidio.tune.sync.applyPendingArtistArtworkMutations
import com.exodidio.tune.sync.validationError
import com.exodidio.tune.ui.screens.artistPickerSampleSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistArtworkTest {
    private val sha = "a".repeat(64)

    @Test
    fun sampleSizeCapsTwelveMpTo336() {
        // 4000x3000 (12 MP) capped to 336 px target: sample 8 -> 500x375.
        assertEquals(8, artistPickerSampleSize(4000, 3000, 336))
    }

    @Test
    fun sampleSizeIsOneForSmall() {
        assertEquals(1, artistPickerSampleSize(300, 300, 336))
    }

    @Test
    fun setArtworkProjectsCustomKey() {
        val pending = listOf(
            ArtistMutation("m1", "artist-a", ArtistArtworkOperation.SET_ARTWORK, 2L, ArtistArtworkPayload(sha)),
        )

        assertEquals(mapOf("artist-a" to sha), applyPendingArtistArtworkMutations(emptyMap(), pending))
    }

    @Test
    fun removeArtworkClearsCustomKey() {
        val pending = listOf(
            ArtistMutation("m1", "artist-a", ArtistArtworkOperation.SET_ARTWORK, 2L, ArtistArtworkPayload(sha)),
            ArtistMutation("m2", "artist-a", ArtistArtworkOperation.REMOVE_ARTWORK, 3L),
        )

        assertEquals(emptyMap<String, String>(), applyPendingArtistArtworkMutations(emptyMap(), pending))
    }

    @Test
    fun setArtworkRequiresAValidHash() {
        assertNotNull(
            ArtistMutation("m1", "artist-a", ArtistArtworkOperation.SET_ARTWORK, 2L, ArtistArtworkPayload(null)).validationError(),
        )
        assertNull(
            ArtistMutation("m1", "artist-a", ArtistArtworkOperation.SET_ARTWORK, 2L, ArtistArtworkPayload(sha)).validationError(),
        )
    }
}
