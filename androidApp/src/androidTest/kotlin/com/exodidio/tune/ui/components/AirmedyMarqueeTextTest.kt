package com.exodidio.tune.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Rule
import org.junit.Test

class TuneMarqueeTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTextContentCorrectly() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                TuneMarqueeText(
                    text = "Very Long Track Title That Will Overflow The Container Width",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        composeTestRule.onNodeWithText("Very Long Track Title That Will Overflow The Container Width")
            .assertIsDisplayed()
    }
}
