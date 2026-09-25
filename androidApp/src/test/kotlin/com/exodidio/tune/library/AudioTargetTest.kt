package com.exodidio.tune.library

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTargetTest {
    @Test fun absolutePathResolvesToFile() {
        val target = resolveAudioTarget(File("/tmp"), "/music/song.flac")
        assertEquals(AudioTarget.FileTarget(File("/music/song.flac")), target)
    }

    @Test fun relativePathResolvesUnderFilesDir() {
        val target = resolveAudioTarget(File("/data/app"), "audio/song.mp3")
        assertEquals(AudioTarget.FileTarget(File("/data/app/audio/song.mp3")), target)
    }

    @Test fun contentUriResolvesToContentTarget() {
        val target = resolveAudioTarget(File("/tmp"), "content://media/external/audio/media/42")
        assertEquals(AudioTarget.ContentTarget("content://media/external/audio/media/42"), target)
    }

    @Test fun blankOrNullResolvesToNull() {
        assertNull(resolveAudioTarget(File("/tmp"), null))
        assertNull(resolveAudioTarget(File("/tmp"), ""))
        assertNull(resolveAudioTarget(File("/tmp"), "   "))
    }

    @Test fun existingFileTargetExists() {
        val tmp = Files.createTempFile("tune-target", ".mp3").toFile()
        try {
            assertTrue(audioTargetExists(AudioTarget.FileTarget(tmp)) { error("must not consult content resolver") })
        } finally {
            tmp.delete()
        }
    }

    @Test fun missingFileTargetDoesNotExist() {
        assertFalse(audioTargetExists(AudioTarget.FileTarget(File("/nonexistent-tune/song.mp3"))) { error("must not consult content resolver") })
    }

    @Test fun contentTargetConsultsResolver() {
        assertTrue(audioTargetExists(AudioTarget.ContentTarget("content://media/1")) { it == "content://media/1" })
        assertFalse(audioTargetExists(AudioTarget.ContentTarget("content://media/1")) { false })
    }
}
