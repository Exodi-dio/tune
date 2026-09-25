package com.exodidio.tune.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.exodidio.tune.R
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppearanceContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appearanceContentDisplaysThemeAndReduceTransparencyItems() {
        var selectedThemeMode: ThemeMode? = null
        var reduceTransparencyState = false

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AppearanceContent(
                    themeMode = ThemeMode.Dark,
                    onThemeModeSelected = { selectedThemeMode = it },
                    reduceTransparency = reduceTransparencyState,
                    onReduceTransparencyChanged = { reduceTransparencyState = it },
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.onNodeWithText(context.getString(R.string.appearance_theme_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.appearance_reduce_transparency)).assertIsDisplayed().performClick()

        assertTrue(reduceTransparencyState)
    }
}
