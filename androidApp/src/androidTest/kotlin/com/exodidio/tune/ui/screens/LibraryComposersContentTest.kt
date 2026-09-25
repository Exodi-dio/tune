package com.exodidio.tune.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.sync.LibraryComposer
import com.exodidio.tune.ui.theme.TuneTheme
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryComposersContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysEmptyStateWhenNoComposers() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryComposersContent(LibraryComposersUiState())
            }
        }

        composeTestRule.onNodeWithText("No composers in library").assertExists()
    }

    @Test
    fun displaysComposersInVirtualizedRows() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryComposersContent(
                    LibraryComposersUiState(
                        composers = listOf(
                            LibraryComposer(id = "c1", name = "Beethoven"),
                            LibraryComposer(id = "c2", name = "Mozart"),
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Beethoven").assertExists()
        composeTestRule.onNodeWithText("Mozart").assertExists()
        composeTestRule.onAllNodesWithTag("composer-row-divider").assertCountEquals(1)
    }

    @Test
    fun composerOverflowUsesProvidedOrderedTrackIds() {
        var nextIds: List<String>? = null
        var addOnly = false
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryComposersContent(
                    LibraryComposersUiState(composers = listOf(LibraryComposer("c", "Bach"))),
                    orderedTrackIdsForComposer = { listOf("track-1", "track-2") },
                    onComposerPlayNext = { nextIds = it },
                    onTrackContextBottomSheet = { request ->
                        addOnly = (request as TrackContextBottomSheetRequest.Playlist).addOnly
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Bach").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Play next").performClick()
        assertEquals(listOf("track-1", "track-2"), nextIds)

        composeTestRule.onNodeWithText("Bach").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Add to playlist").performClick()
        assertEquals(true, addOnly)
    }
}
