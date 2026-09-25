package com.exodidio.tune.ui.components

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.chrisbanes.haze.rememberHazeState
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.screens.SortOrder
import com.exodidio.tune.ui.screens.AlbumLayoutMode
import com.exodidio.tune.ui.screens.TrackSortOption
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibrarySortHeaderButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun opensMenuAndHandlesOptionSelection() {
        var selectedOption: TrackSortOption? = null
        var orderToggled = false

        composeTestRule.setContent {
            val hazeState = rememberHazeState()
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = hazeState) {
                    LibrarySortHeaderButton(
                        hazeState = hazeState,
                        options = listOf(
                            LibrarySortOption(TrackSortOption.Name, com.exodidio.tune.R.string.sort_name),
                            LibrarySortOption(TrackSortOption.Artist, com.exodidio.tune.R.string.sort_artist),
                        ),
                        selectedOption = TrackSortOption.Name,
                        sortOrder = SortOrder.Ascending,
                        onSortOptionSelected = { selectedOption = it },
                        onToggleSortOrder = { orderToggled = true },
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Sort by")).performClick()

        composeTestRule.onNodeWithText("Artist").assertExists()
        composeTestRule.onNodeWithText("Artist").performClick()
        assertTrue(selectedOption == TrackSortOption.Artist)
    }

    @Test
    fun albumDisplayOptionsSelectGridLayout() {
        var selectedLayout: AlbumLayoutMode? = null

        composeTestRule.setContent {
            val hazeState = rememberHazeState()
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = hazeState) {
                    LibrarySortHeaderButton(
                        hazeState = hazeState,
                        options = listOf(LibrarySortOption(TrackSortOption.Name, com.exodidio.tune.R.string.sort_name)),
                        selectedOption = TrackSortOption.Name,
                        sortOrder = SortOrder.Ascending,
                        onSortOptionSelected = {},
                        onToggleSortOrder = {},
                        layoutMode = AlbumLayoutMode.List,
                        onLayoutModeSelected = { selectedLayout = it },
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Album display options")).performClick()
        composeTestRule.onNodeWithText("Grid").performClick()
        assertTrue(selectedLayout == AlbumLayoutMode.Grid)
    }
}
