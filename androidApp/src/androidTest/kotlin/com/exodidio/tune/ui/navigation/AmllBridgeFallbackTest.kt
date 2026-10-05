package com.exodidio.tune.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.exodidio.tune.lyrics.RomanizationUiState
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Rule
import org.junit.Test

class AmllBridgeFallbackTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun usesNativeLyricsWhenAmllBridgeFallsBack() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                FullScreenPlayerLyricsPanel(
                    trackId = "fallback-track",
                    lyrics = """<tt><body><p begin="oops">Broken""",
                    romanization = RomanizationUiState(),
                    forcedFallbackReason = "forced fallback",
                    currentPositionMs = 0L,
                    onSeek = {},
                    modifier = Modifier.height(400.dp),
                )
            }
        }

        composeTestRule.onNodeWithTag("plain_lyrics_list").assertExists()
    }
}
