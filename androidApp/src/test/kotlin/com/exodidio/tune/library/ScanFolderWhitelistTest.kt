package com.exodidio.tune.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFolderWhitelistTest {
    @Test
    fun emptyWhitelistAllowsEverything() {
        assertTrue(isScanPathAllowed("/storage/emulated/0/Music/song.mp3", emptySet()))
        assertTrue(isScanPathAllowed(null, emptySet()))
    }

    @Test
    fun whitelistedDirMatchesChildren() {
        val whitelist = setOf("/storage/emulated/0/Music")
        assertTrue(isScanPathAllowed("/storage/emulated/0/Music/song.mp3", whitelist))
        assertTrue(isScanPathAllowed("/storage/emulated/0/Music/Album/song.flac", whitelist))
    }

    @Test
    fun siblingsExcluded() {
        val whitelist = setOf("/storage/emulated/0/Music")
        assertFalse(isScanPathAllowed("/storage/emulated/0/Download/song.mp3", whitelist))
        assertFalse(isScanPathAllowed(null, whitelist))
    }

    @Test
    fun prefixBoundaryRequiresDirectorySeparator() {
        val whitelist = setOf("/storage/emulated/0/Music")
        assertFalse(isScanPathAllowed("/storage/emulated/0/Music2/song.mp3", whitelist))
        assertFalse(isScanPathAllowed("/storage/emulated/0/MusicExtra/song.mp3", whitelist))
    }

    @Test
    fun unmappableWhitelistDenies() {
        // F1 fail-closed: non-empty whitelist with zero convertible prefixes denies.
        val whitelist = setOf("content://com.example.documents/document/noid")
        assertTrue(scanFilePrefixes(whitelist).isEmpty())
        assertFalse(isScanPathAllowed("/storage/emulated/0/Music/song.mp3", whitelist))
        assertFalse(isScanPathAllowed(null, whitelist))
    }

    @Test
    fun plusFolderPreserved() {
        // F2: literal '+' must survive tree-URI decoding (not become a space).
        val uri = "content://com.android.externalstorage.documents/tree/primary%3AMy+Music"
        assertEquals("/storage/emulated/0/My+Music", scanPathForTreeUriString(uri))
        val encoded = "content://com.android.externalstorage.documents/tree/primary%3AMy%2BMusic"
        assertEquals("/storage/emulated/0/My+Music", scanPathForTreeUriString(encoded))
        val whitelist = setOf(uri)
        assertTrue(isScanPathAllowed("/storage/emulated/0/My+Music/song.mp3", whitelist))
        assertFalse(isScanPathAllowed("/storage/emulated/0/My Music/song.mp3", whitelist))
    }

    @Test
    fun dataNullFallbackComposesRelativePath() {
        // F3: DATA null on scoped storage -> RELATIVE_PATH + DISPLAY_NAME fallback.
        assertEquals(
            "/storage/emulated/0/Music/Album/song.mp3",
            fallbackScanPathFor("Music/Album/", "song.mp3"),
        )
        assertEquals("/storage/emulated/0/Music/song.mp3", fallbackScanPathFor("Music", "song.mp3"))
        assertNull(fallbackScanPathFor("Music/", null))
        val row = mediaRow(dataPath = null, relativePath = "Music/", displayName = "song.mp3")
        assertEquals("/storage/emulated/0/Music/song.mp3", scanPathForRow(row))
        val whitelist = setOf("/storage/emulated/0/Music")
        assertEquals(listOf(row), filterAllowedMediaRows(listOf(row), whitelist))
        val missing = mediaRow(dataPath = null, relativePath = null, displayName = null)
        assertNull(scanPathForRow(missing))
        assertFalse(isScanPathAllowed(scanPathForRow(missing), whitelist))
    }

    @Test
    fun aliasesAndSlashesNormalized() {
        // F4: /sdcard alias, duplicate slashes.
        val whitelist = setOf("/storage/emulated/0/Music")
        assertTrue(isScanPathAllowed("/sdcard/Music/song.mp3", whitelist))
        assertTrue(isScanPathAllowed("/storage//emulated/0//Music//song.mp3", whitelist))
        assertTrue(isScanPathAllowed("/mnt/sdcard/Music/song.mp3", whitelist))
        assertEquals("/storage/emulated/0/Music/Album", scanPathForTreeUriString("content://com.android.externalstorage.documents/tree/primary%3AMusic%2F%2FAlbum"))
    }

    @Test
    fun contentRowDeniedWhenConstrained() {
        // F6: content:// rows cannot be verified -> excluded when constrained.
        val row = mediaRow(dataPath = "content://media/external/audio/1", relativePath = null, displayName = null)
        val whitelist = setOf("/storage/emulated/0/Music")
        assertFalse(isScanPathAllowed(row.dataPath, whitelist))
        assertTrue(filterAllowedMediaRows(listOf(row), whitelist).isEmpty())
        assertEquals(listOf(row), filterAllowedMediaRows(listOf(row), emptySet()))
    }

    @Test
    fun fileSchemeHandling() {
        // F6: file:// entries and paths resolve like plain paths.
        val whitelist = setOf("file:///storage/emulated/0/Music")
        assertTrue(isScanPathAllowed("file:///storage/emulated/0/Music/song.mp3", whitelist))
        assertTrue(isScanPathAllowed("/storage/emulated/0/Music/song.mp3", whitelist))
        assertFalse(isScanPathAllowed("file:///storage/emulated/0/Download/song.mp3", whitelist))
        val encodedWhitelist = setOf("file:///storage/emulated/0/My%20Music")
        assertTrue(isScanPathAllowed("/storage/emulated/0/My Music/song.mp3", encodedWhitelist))
    }

    @Test
    fun caseAndTrimInsensitive() {
        // F6: surrounding whitespace and letter case do not affect matching.
        val whitelist = setOf("  /STORAGE/emulated/0/music  ")
        assertTrue(isScanPathAllowed("/storage/emulated/0/Music/song.mp3", whitelist))
        assertTrue(isScanPathAllowed("  /storage/emulated/0/MUSIC/song.mp3  ", setOf("/storage/emulated/0/Music")))
    }

    private fun mediaRow(dataPath: String?, relativePath: String?, displayName: String?): MediaRow =
        MediaRow(
            mediaId = 1L,
            uri = "content://media/external/audio/1",
            displayName = displayName,
            title = null,
            artist = null,
            album = null,
            albumArtist = null,
            genre = null,
            composer = null,
            year = null,
            trackNo = null,
            discNo = null,
            totalTracks = null,
            totalDiscs = null,
            durationMs = null,
            bitrate = null,
            sizeBytes = null,
            dateAddedSec = null,
            mimeType = null,
            label = null,
            copyright = null,
            dataPath = dataPath,
            relativePath = relativePath,
        )
}
