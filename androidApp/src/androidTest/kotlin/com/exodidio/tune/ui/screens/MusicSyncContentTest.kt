package com.exodidio.tune.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.exodidio.tune.R
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Rule
import org.junit.Test

class MusicSyncContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun musicSyncContentDisplaysTheScanAction() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                MusicSyncContent()
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.onNodeWithText(context.getString(R.string.library_scan_local)).assertIsDisplayed()
    }

    @Test
    fun musicSyncContentShowsTheMusicSyncTitleString() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assert(context.getString(R.string.music_sync_title).isNotBlank())
        assert(context.getString(R.string.settings_music_sync).isNotBlank())
    }
}
