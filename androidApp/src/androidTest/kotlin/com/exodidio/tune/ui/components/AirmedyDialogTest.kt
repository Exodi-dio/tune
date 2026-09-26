package com.exodidio.tune.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TuneDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confirmActionUsesTheProvidedCallback() {
        var confirmed by mutableStateOf(false)
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                TuneDialog(
                    title = "Delete this item?",
                    description = "This permanently removes the selected item.",
                    dismissLabel = "Cancel",
                    onDismiss = {},
                    confirmLabel = "Delete",
                    onConfirm = { confirmed = true },
                    confirmVariant = TunePillButtonVariant.Destructive,
                )
            }
        }

        composeTestRule.onNodeWithText("Delete").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun alertUsesItsSingleDismissAction() {
        var dismissed by mutableStateOf(false)
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                TuneDialog(
                    title = "Something went wrong",
                    description = "Please try again later.",
                    dismissLabel = "Close",
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Close").performClick()
        assertTrue(dismissed)
    }
}
