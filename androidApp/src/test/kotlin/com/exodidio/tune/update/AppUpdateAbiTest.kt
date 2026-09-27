package com.exodidio.tune.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateAbiTest {
    @Test fun prefersFirstSupportedAbi() {
        val assets = listOf(
            "Tune-v1.0.0.apk",
            "Tune-v1.0.0_armeabi-v7a.apk",
            "Tune-v1.0.0_x86_64.apk",
            "Tune-v1.0.0_arm64-v8a.apk"
        )
        assertEquals(
            "Tune-v1.0.0_arm64-v8a.apk",
            selectAbiAsset(assets, listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        )
    }

    @Test fun respectsOsReportedOrder() {
        val assets = listOf(
            "Tune-v1.0.0_arm64-v8a.apk",
            "Tune-v1.0.0_armeabi-v7a.apk"
        )
        assertEquals(
            "Tune-v1.0.0_armeabi-v7a.apk",
            selectAbiAsset(assets, listOf("armeabi-v7a", "arm64-v8a"))
        )
    }

    @Test fun fallsBackToUniversalWhenNoAbiMatch() {
        val assets = listOf(
            "Tune-v1.0.0.apk",
            "Tune-v1.0.0_x86_64.apk"
        )
        assertEquals(
            "Tune-v1.0.0.apk",
            selectAbiAsset(assets, listOf("arm64-v8a"))
        )
    }

    @Test fun returnsNullWhenNoMatch() {
        assertNull(selectAbiAsset(listOf("Tune-v1.0.0_x86_64.apk"), listOf("arm64-v8a")))
        assertNull(selectAbiAsset(emptyList(), listOf("arm64-v8a")))
        assertNull(selectAbiAsset(listOf("notes.txt"), listOf("arm64-v8a")))
    }

    @Test fun allowlistStillAppliedByCaller() {
        // ABI-matching asset is off-allowlist: caller must skip it and fall back to allowlisted universal.
        val json = """{"tag_name":"v1.0.1","body":"Notes","assets":[{"name":"Tune-v1.0.1_arm64-v8a.apk","browser_download_url":"https://example.com/Tune-v1.0.1_arm64-v8a.apk"},{"name":"Tune-v1.0.1.apk","browser_download_url":"https://github.com/Exodi-dio/tune/releases/download/v1.0.1/Tune-v1.0.1.apk"}]}"""
        val release = parseLatestReleaseJson(json, listOf("arm64-v8a"))
        assertEquals("https://github.com/Exodi-dio/tune/releases/download/v1.0.1/Tune-v1.0.1.apk", release!!.apkUrl)
    }

    @Test fun allowlistRejectsOnlyOffAllowlistAbiAsset() {
        val json = """{"tag_name":"v1.0.1","body":"Notes","assets":[{"name":"Tune-v1.0.1_arm64-v8a.apk","browser_download_url":"https://example.com/Tune-v1.0.1_arm64-v8a.apk"}]}"""
        val release = parseLatestReleaseJson(json, listOf("arm64-v8a"))
        assertNull(release!!.apkUrl)
    }
}
