package com.exodidio.tune.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateSemverTest {
    @Test fun stripsLeadingV() {
        assertEquals("1.0.0", normalizeVersionTag("v1.0.0"))
        assertEquals("1.0.0", normalizeVersionTag("V1.0.0"))
        assertEquals("1.0.0", normalizeVersionTag("  v1.0.0  "))
        assertEquals("1.0.0", normalizeVersionTag("1.0.0"))
    }

    @Test fun comparesNumericSemver() {
        assertTrue(compareSemver("1.0.1", "1.0.0") > 0)
        assertTrue(compareSemver("1.0.0", "1.0.1") < 0)
        assertEquals(0, compareSemver("1.0.0", "v1.0.0"))
        assertTrue(compareSemver("1.10.0", "1.9.9") > 0)
        assertTrue(compareSemver("2.0.0", "1.99.99") > 0)
    }

    @Test fun prereleaseIsOlderThanRelease() {
        assertTrue(compareSemver("1.0.0-beta", "1.0.0") < 0)
        assertTrue(compareSemver("1.0.0", "1.0.0-beta") > 0)
        assertTrue(compareSemver("1.0.0-alpha", "1.0.0-beta") < 0)
    }

    @Test fun malformedInputFallsBackGracefully() {
        assertEquals("not-a-version", normalizeVersionTag("not-a-version"))
        // Malformed versions must not crash; comparison falls back to lexical order.
        assertEquals(0, compareSemver("not-a-version", "not-a-version"))
        assertFalse(isUpdateAvailable("not-a-version", "also-bad"))
    }

    @Test fun detectsUpdateAvailable() {
        assertTrue(isUpdateAvailable("1.0.0", "v1.0.1"))
        assertFalse(isUpdateAvailable("1.0.0", "1.0.0"))
        assertFalse(isUpdateAvailable("1.0.1", "1.0.0"))
        assertFalse(isUpdateAvailable("1.0.0", "1.0.0-beta"))
    }

    @Test fun parsesLatestReleaseJson() {
        val json = """{"tag_name":"v1.0.1","body":"Fixes and improvements","assets":[{"name":"tune-1.0.1.apk","browser_download_url":"https://example.com/tune.apk"},{"name":"notes.txt","browser_download_url":"https://example.com/notes.txt"}]}"""
        val release = parseLatestReleaseJson(json)
        assertNotNull(release)
        assertEquals("1.0.1", release!!.version)
        assertEquals("https://example.com/tune.apk", release.apkUrl)
        assertTrue(release.notes.contains("Fixes"))
    }

    @Test fun parsesReleaseWithoutApkAsset() {
        val json = """{"tag_name":"v1.0.2","body":"Notes","assets":[{"name":"notes.txt","browser_download_url":"https://example.com/notes.txt"}]}"""
        val release = parseLatestReleaseJson(json)
        assertNotNull(release)
        assertNull(release!!.apkUrl)
    }
}
