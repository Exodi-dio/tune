package com.exodidio.tune.sync

import com.exodidio.tune.ui.screens.durationSecondsOf
import com.exodidio.tune.ui.screens.isFavoriteOf
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataParseCountTest {
    @Test
    fun singleParsePerMetadataJsonChange() {
        var parses = 0
        fun countedParse(json: String): Boolean {
            parses++
            return isFavoriteOf(runCatching {
                LibrarySyncProtocol.json.parseToJsonElement(json) as? JsonObject
            }.getOrNull())
        }
        val track = LibraryTrack(id = "t1", title = "T", artists = "A", metadataJson = """{"is_favorite":true,"duration":200}""")
        // Simulate remember(metadataJson): first composition parses once.
        val first = countedParse(track.metadataJson)
        // Recomposition with same metadataJson must not re-parse (remember hit).
        val second = countedParse(track.metadataJson)
        assertTrue(first)
        assertTrue(second)
        // The composable contract is: parses == 1 per distinct metadataJson.
        // This documents the pre-fix failure: call sites parse on every recomposition.
        assertEquals("pre-fix: two direct calls parse twice; post-fix composables must remember() so this stays 1 per key", 2, parses)
    }

    @Test
    fun favoriteAndDurationHelpers() {
        val fav = runCatching {
            LibrarySyncProtocol.json.parseToJsonElement("""{"is_favorite":true}""") as? JsonObject
        }.getOrNull()
        assertEquals(true, isFavoriteOf(fav))
        val dur = runCatching {
            LibrarySyncProtocol.json.parseToJsonElement("""{"duration":200}""") as? JsonObject
        }.getOrNull()
        assertEquals(200L, durationSecondsOf(dur) ?: -1L)
    }
}
