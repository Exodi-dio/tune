package com.exodidio.tune.ui.screens

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.sync.LibraryAlbum
import com.exodidio.tune.sync.LibraryArtist
import com.exodidio.tune.ui.theme.TuneTheme
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArtistDetailsContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun displaysArtistSummaryAndOpensSelectedAlbum() {
        var selectedAlbumId: String? = null
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                ArtistDetailsContent(
                    uiState = ArtistDetailsUiState(
                        artist = LibraryArtist("artist", "Muse"),
                        albums = listOf(LibraryAlbum("album", "Absolution", "Muse"), LibraryAlbum("album-2", "Origin of Symmetry", "Muse")),
                        tracks = List(3) { index -> com.exodidio.tune.sync.LibraryTrack("$index", "Track $index", "Muse") },
                    ),
                    listState = rememberLazyListState(),
                    onAlbumClick = { selectedAlbumId = it.id },
                )
            }
        }

        composeTestRule.onNodeWithText("Muse").assertExists()
        composeTestRule.onNodeWithText("2 albums · 3 tracks").assertExists()
        composeTestRule.onNodeWithText("Absolution").performClick()
        composeTestRule.onAllNodesWithTag("artist-detail-album-divider").assertCountEquals(3)
        assertEquals("album", selectedAlbumId)
    }

    @Test
    fun displaysUnavailableStateWhenArtistIsMissing() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                ArtistDetailsContent(ArtistDetailsUiState(), listState = rememberLazyListState())
            }
        }

        composeTestRule.onNodeWithText("Artist unavailable").assertExists()
    }

    @Test
    fun doesNotDisplayAnAlbumDividerForOneAlbum() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                ArtistDetailsContent(
                    uiState = ArtistDetailsUiState(
                        artist = LibraryArtist("artist", "Muse"),
                        albums = listOf(LibraryAlbum("album", "Absolution", "Muse")),
                    ),
                    listState = rememberLazyListState(),
                )
            }
        }

        composeTestRule.onAllNodesWithTag("artist-detail-album-divider").assertCountEquals(2)
    }

    @Test
    fun artistMoreMenuUsesOrderedTracksForPlayNextAndPlaylist() {
        var nextIds: List<String>? = null
        var playlistIds: List<String>? = null
        var addOnly = false
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                ArtistDetailsContent(
                    uiState = ArtistDetailsUiState(
                        artist = LibraryArtist("artist", "Muse"),
                        tracks = listOf(
                            com.exodidio.tune.sync.LibraryTrack("first", "First", "Muse"),
                            com.exodidio.tune.sync.LibraryTrack("second", "Second", "Muse"),
                        ),
                    ),
                    listState = rememberLazyListState(),
                    onPlayNext = { nextIds = it },
                    onTrackContextBottomSheet = { request ->
                        val playlistRequest = request as TrackContextBottomSheetRequest.Playlist
                        playlistIds = playlistRequest.trackIds
                        addOnly = playlistRequest.addOnly
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Artist options").performClick()
        composeTestRule.onNodeWithText("Play next").performClick()
        assertEquals(listOf("first", "second"), nextIds)

        composeTestRule.onNodeWithText("Artist options").performClick()
        composeTestRule.onNodeWithText("Add to playlist").performClick()
        assertEquals(listOf("first", "second"), playlistIds)
        assertEquals(true, addOnly)
    }
}
