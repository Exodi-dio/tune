package com.exodidio.tune.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Review-gated only: androidTest is NOT compiled by CI. A reviewer runs this
 * locally with a connected device or emulator.
 */
class PlayerShellNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dismissStripIsDisplayedInsideMaxWidthSheet() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlayerShell(
                    visible = true,
                    onDismiss = {},
                    windowWidthDp = 800,
                    selectedPanel = null,
                    onPanelSelected = {},
                    content = { Text("now-playing-content") },
                )
            }
        }
        composeTestRule.onNodeWithTag(PlayerShellDismissStripTestTag).assertIsDisplayed()
        composeTestRule.onNodeWithText("now-playing-content").assertIsDisplayed()
        val maxWidthPx = with(composeTestRule.density) { 560.dp.roundToPx() }
        val sheetWidthPx = composeTestRule
            .onNodeWithTag(PlayerShellContentTestTag)
            .fetchSemanticsNode().size.width
        assertTrue(
            "PlayerShell sheet width ${sheetWidthPx}px exceeds widthIn(max = 560.dp) ($maxWidthPx px)",
            sheetWidthPx <= maxWidthPx,
        )
    }

    @Test
    fun clickingDismissStripCallsOnDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlayerShell(
                    visible = true,
                    onDismiss = { dismissed = true },
                    windowWidthDp = 360,
                    selectedPanel = null,
                    onPanelSelected = {},
                    content = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(PlayerShellDismissStripTestTag).performClick()
        assertTrue(dismissed)
    }

    @Test
    fun hiddenShellEmitsNoDismissStrip() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlayerShell(
                    visible = false,
                    onDismiss = {},
                    windowWidthDp = 360,
                    selectedPanel = null,
                    onPanelSelected = {},
                    content = { Text("now-playing-content") },
                )
            }
        }
        composeTestRule.onNodeWithTag(PlayerShellDismissStripTestTag).assertDoesNotExist()
    }
}
