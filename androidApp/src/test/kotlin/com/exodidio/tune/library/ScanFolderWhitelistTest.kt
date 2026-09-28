package com.exodidio.tune.library

import org.junit.Assert.assertFalse
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
}
