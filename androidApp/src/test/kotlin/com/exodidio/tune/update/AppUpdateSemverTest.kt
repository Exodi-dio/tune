package com.exodidio.tune.update

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
        val json = """{"tag_name":"v1.0.1","body":"Fixes and improvements","assets":[{"name":"tune-1.0.1.apk","browser_download_url":"https://github.com/Exodi-dio/tune/releases/download/v1.0.1/tune-1.0.1.apk"},{"name":"notes.txt","browser_download_url":"https://github.com/Exodi-dio/tune/releases/download/v1.0.1/notes.txt"}]}"""
        val release = parseLatestReleaseJson(json)
        assertNotNull(release)
        assertEquals("1.0.1", release!!.version)
        assertEquals("https://github.com/Exodi-dio/tune/releases/download/v1.0.1/tune-1.0.1.apk", release.apkUrl)
        assertTrue(release.notes.contains("Fixes"))
    }

    @Test fun parsesReleaseWithoutApkAsset() {
        val json = """{"tag_name":"v1.0.2","body":"Notes","assets":[{"name":"notes.txt","browser_download_url":"https://github.com/Exodi-dio/tune/releases/download/v1.0.2/notes.txt"}]}"""
        val release = parseLatestReleaseJson(json)
        assertNotNull(release)
        assertNull(release!!.apkUrl)
    }

    @Test fun allowsOnlyGitHubReleaseHosts() {
        assertTrue(isAllowedApkUrl("https://github.com/Exodi-dio/tune/releases/download/v1.0.1/tune.apk"))
        assertTrue(isAllowedApkUrl("https://objects.githubusercontent.com/abc/tune.apk"))
        assertTrue(isAllowedApkUrl("https://release-assets.githubusercontent.com/abc/tune.apk"))
        assertFalse(isAllowedApkUrl("https://example.com/tune.apk"))
        assertFalse(isAllowedApkUrl("http://github.com/Exodi-dio/tune/releases/download/v1.0.1/tune.apk"))
        assertFalse(isAllowedApkUrl("file:///data/data/com.exodidio.tune/cache/tune.apk"))
        assertFalse(isAllowedApkUrl("content://provider/tune.apk"))
        assertFalse(isAllowedApkUrl("not a url"))
    }

    @Test fun rejectsNonAllowlistedApkAsset() {
        val json = """{"tag_name":"v1.0.3","body":"Notes","assets":[{"name":"tune-1.0.3.apk","browser_download_url":"https://example.com/tune.apk"}]}"""
        val release = parseLatestReleaseJson(json)
        assertNotNull(release)
        assertNull(release!!.apkUrl)
    }

    @Test fun externalCacheDestinationLivesUnderExternalCacheDir() {
        val base = File("/tmp/externalCache")
        val file = updateApkExternalFile(base, "v1.0.1")
        val parent = File(base, UpdateApkSubdir).absolutePath
        assertTrue(file.absolutePath.startsWith(parent))
        assertEquals("tune-1.0.1.apk", file.name)
        assertEquals("updates/tune-1.0.1.apk", updateApkRelativePath("v1.0.1"))
        assertEquals("tune-1.0.1.apk", updateApkFileName("v1.0.1"))
    }

    @Test fun boundedReadEnforcesCap() {
        val small = ByteArrayInputStream("hello".toByteArray(Charsets.UTF_8))
        assertEquals("hello", readBoundedText(small, UpdateMaxResponseBytes))
        val big = ByteArrayInputStream(ByteArray(UpdateMaxResponseBytes + 1) { 97 })
        try {
            readBoundedText(big, UpdateMaxResponseBytes)
            fail("expected response-too-large error")
        } catch (_: IllegalStateException) {
        }
    }
}
