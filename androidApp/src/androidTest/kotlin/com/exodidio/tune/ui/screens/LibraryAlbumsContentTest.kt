package com.exodidio.tune.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.sync.LibraryAlbum
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.components.AnchoredPopupMenuHost
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryAlbumsContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun displaysEmptyStateWhenNoAlbums() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) { LibraryAlbumsContent(LibraryAlbumsUiState()) }
        }
        composeTestRule.onNodeWithText("No albums in library").assertExists()
    }

    @Test
    fun displaysAlbumsInVirtualizedRowsAndUsesUnknownArtistFallback() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryAlbumsContent(
                    LibraryAlbumsUiState(
                        albums = listOf(
                            LibraryAlbum(id = "a", title = "Album A", artist = "Artist A"),
                            LibraryAlbum(id = "b", title = "Album B"),
                        ),
                    ),
                )
            }
        }
        composeTestRule.onNodeWithText("Album A").assertExists()
        composeTestRule.onNodeWithText("Artist A").assertExists()
        composeTestRule.onNodeWithText("Unknown artist").assertExists()
        composeTestRule.onAllNodesWithTag("album-row-divider").assertCountEquals(1)
    }

    @Test
    fun playbackActionsAppearForAlbumsAndDispatchTheirMode() {
        var playMode: Boolean? = null
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryAlbumsContent(
                    LibraryAlbumsUiState(albums = listOf(LibraryAlbum(id = "album", title = "Album"))),
                    onPlayAll = { playMode = it },
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Play")).performClick()
        assertEquals(false, playMode)
        composeTestRule.onNode(hasContentDescription("Shuffle")).performClick()
        assertEquals(true, playMode)
    }

    @Test
    fun holdingAnAlbumOpensItsContextMenu() {
        var request: TrackContextBottomSheetRequest? = null
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = null) {
                    LibraryAlbumsContent(
                        LibraryAlbumsUiState(
                            albums = listOf(LibraryAlbum(id = "album", title = "Album A", artist = "Artist A")),
                            tracks = listOf(LibraryTrack("track", "Track", "Artist A", albumId = "album")),
                        ),
                        onTrackContextBottomSheet = { request = it },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Album A").performTouchInput { longClick() }

        composeTestRule.onNodeWithText("Play next").assertExists()
        composeTestRule.onNode(hasContentDescription("Add to favourites")).assertExists()
        composeTestRule.onNodeWithText("Add to playlist").performClick()
        assertEquals(
            TrackContextBottomSheetRequest.Playlist(listOf("track"), addOnly = true),
            request,
        )
    }

    @Test
    fun displaysAlbumsInTwoColumnGridWithoutListDividers() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryAlbumsContent(
                    LibraryAlbumsUiState(
                        layoutMode = AlbumLayoutMode.Grid,
                        albums = listOf(
                            LibraryAlbum(id = "a", title = "Album A", artist = "Artist A"),
                            LibraryAlbum(id = "b", title = "Album B"),
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Album A").assertExists()
        composeTestRule.onNodeWithText("Album B").assertExists()
        composeTestRule.onNodeWithText("Unknown artist").assertExists()
        composeTestRule.onAllNodesWithTag("album-row-divider").assertCountEquals(0)
    }
}
