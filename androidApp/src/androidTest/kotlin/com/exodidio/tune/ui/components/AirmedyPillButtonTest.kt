package com.exodidio.tune.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TunePillButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun destructiveVariantExposesItsActionAndInvokesCallback() {
        var clicked by mutableStateOf(false)
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                TunePillButton(
                    label = "Revoke",
                    onClick = { clicked = true },
                    variant = TunePillButtonVariant.Destructive,
                )
            }
        }

        composeTestRule.onNodeWithText("Revoke").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }

    @Test
    fun darkThemeUsesWhitePrimaryForegroundForPrimaryAndDestructiveActions() {
        var onPrimary: Color? = null

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                onPrimary = MaterialTheme.colorScheme.onPrimary
                TunePillButton(
                    label = "Continue",
                    onClick = {},
                    variant = TunePillButtonVariant.Primary,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertEquals(Color.White, onPrimary)
        }
    }
}
