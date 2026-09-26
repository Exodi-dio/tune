package com.exodidio.tune.ui.screens

import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import com.exodidio.tune.lastfm.LastFmStatus
import com.exodidio.tune.lyrics.LyricsSettings
import com.exodidio.tune.lyrics.LyricsSource
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IntegrationContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun connectedAccountShowsUsernameAndDisconnects() {
        var disconnected = false
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LastFmContent(
                    status = LastFmStatus(connected = true, username = "listener"),
                    onConnect = {},
                    onDisconnect = { disconnected = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Connected as listener").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lastfm-icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disconnect").performClick()
        assertTrue(disconnected)
    }

    @Test
    fun connectedAccountUsesAvatarInsteadOfIcon() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val avatar = File(context.filesDir, "lastfm-test-avatar.png")
        avatar.outputStream().use {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LastFmContent(
                    status = LastFmStatus(connected = true, username = "listener", avatarPath = avatar.absolutePath),
                    onConnect = {},
                    onDisconnect = {},
                )
            }
        }

        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag("lastfm-avatar").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("lastfm-avatar").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("lastfm-icon").assertCountEquals(0)
        avatar.delete()
    }

    @Test
    fun lyricsSourceSelectionReportsAutoFetch() {
        var source = LyricsSource.AutoFetch
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LyricsContent(
                    settings = LyricsSettings(),
                    onSourceChanged = { source = it },
                    onLrclibChanged = {},
                    onKugouChanged = {},
                    onRomanizationEnabledChanged = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Auto fetch").performClick()
        assertEquals(LyricsSource.AutoFetch, source)
    }

    @Test
    fun romanizationSettingReportsItsNewValue() {
        var enabled = false
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LyricsContent(
                    settings = LyricsSettings(),
                    onSourceChanged = {},
                    onLrclibChanged = {},
                    onKugouChanged = {},
                    onRomanizationEnabledChanged = { enabled = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Enable romanization").performClick()
        assertTrue(enabled)
    }
}
