package com.exodidio.tune.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.components.PlaylistRow
import com.exodidio.tune.ui.components.AnchoredPopupMenuHost
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class LibraryPlaylistsContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun displaysFavoritesWhenTheSyncManifestHasNoPlaylists() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryPlaylistsContent(
                    LibraryPlaylistsUiState(listOf(PlaylistListItem("favorites", ""))),
                    listState = LazyListState(),
                )
            }
        }
        composeTestRule.onNodeWithText("Favorites").assertExists()
    }

    @Test
    fun displaysPlaylistWithLargerArtworkAndChevron() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlaylistRow("p", "Night Drive", listOf("a", "b", "c", "d"), Modifier.testTag("playlist-row"))
            }
        }
        composeTestRule.onNodeWithText("Night Drive").assertExists()
        composeTestRule.onNodeWithContentDescription("Open playlist").assertExists()
        composeTestRule.onNodeWithTag("playlist-artwork-mosaic").assertHeightIsEqualTo(110.dp)
    }

    @Test
    fun createSheetRequiresANameAndReturnsTrimmedName() {
        var created: String? = null
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                CreatePlaylistBottomSheet(onDismiss = {}, onCreate = { name, _ -> created = name })
            }
        }
        composeTestRule.onNodeWithTag("playlist-create-button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("playlist-name-input").performTextInput("  Night Drive  ")
        composeTestRule.onNodeWithTag("playlist-create-button").performClick()
        assertEquals("Night Drive", created)
    }

    @Test
    fun createSheetExposesAnOptionalArtworkPicker() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                CreatePlaylistBottomSheet(onDismiss = {}, onCreate = { _, _ -> })
            }
        }
        composeTestRule.onNodeWithTag("playlist-artwork-picker").assertExists()
        composeTestRule.onNodeWithContentDescription("Choose playlist artwork").assertExists()
    }

    @Test
    fun editSheetCanClearAnExistingPlaylistArtwork() {
        var clearArtwork = false
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                EditPlaylistBottomSheet(
                    initialName = "Night Drive",
                    artworkPath = "cover.jpg",
                    onDismiss = {},
                    onSave = { _, _, clear -> clearArtwork = clear },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Clear playlist artwork").performClick()
        composeTestRule.onNodeWithTag("playlist-create-button").performClick()
        assertEquals(true, clearArtwork)
    }

    @Test
    fun favoritesEditorHidesThePlaylistName() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                EditPlaylistBottomSheet(
                    initialName = "",
                    artworkPath = null,
                    showNameInput = false,
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onAllNodesWithTag("playlist-name-input").assertCountEquals(0)
        composeTestRule.onNodeWithTag("playlist-create-button").assertIsEnabled()
    }

    @Test
    fun longPressingPlaylistRowShowsPlaylistMenu() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = null) {
                    LibraryPlaylistsContent(
                        LibraryPlaylistsUiState(listOf(PlaylistListItem("p", "Night Drive", trackIds = listOf("one")))),
                        listState = LazyListState(),
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Night Drive").performSemanticsAction(SemanticsActions.OnLongClick)
        composeTestRule.onNodeWithText("Play next").assertExists()
        composeTestRule.onNodeWithText("Edit playlist").assertExists()
        composeTestRule.onNodeWithText("Delete playlist").assertExists()
    }
}
