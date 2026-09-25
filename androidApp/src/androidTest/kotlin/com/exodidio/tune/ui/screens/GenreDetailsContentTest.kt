package com.exodidio.tune.ui.screens

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.sync.LibraryAlbum
import com.exodidio.tune.sync.LibraryGenre
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.theme.TuneTheme
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GenreDetailsContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun displaysGenreSummaryAndOpensSelectedAlbum() {
        var selectedAlbumId: String? = null
        var playCount = 0
        var shuffleCount = 0
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                GenreDetailsContent(
                    uiState = GenreDetailsUiState(
                        genre = LibraryGenre("electronic", "Electronic"),
                        albums = listOf(LibraryAlbum("album", "Untrue", "Burial"), LibraryAlbum("album-2", "Archangel", "Burial")),
                        tracks = List(3) { index -> LibraryTrack("$index", "Track $index", "Burial") },
                    ),
                    listState = rememberLazyListState(),
                    onPlay = { playCount++ },
                    onShuffle = { shuffleCount++ },
                    onAlbumClick = { selectedAlbumId = it.id },
                )
            }
        }

        composeTestRule.onNodeWithText("Electronic").assertExists()
        composeTestRule.onNodeWithText("2 albums · 3 tracks").assertExists()
        composeTestRule.onNodeWithText("Play").performClick()
        composeTestRule.onNodeWithText("Shuffle").performClick()
        composeTestRule.onNodeWithText("Untrue").performClick()
        composeTestRule.onAllNodesWithTag("genre-detail-album-divider").assertCountEquals(3)
        assertEquals(1, playCount)
        assertEquals(1, shuffleCount)
        assertEquals("album", selectedAlbumId)
    }

    @Test
    fun displaysUnavailableStateWhenGenreIsMissing() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                GenreDetailsContent(GenreDetailsUiState(), listState = rememberLazyListState())
            }
        }

        composeTestRule.onNodeWithText("Genre unavailable").assertExists()
    }

    @Test
    fun doesNotDisplayAnAlbumDividerForOneAlbum() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                GenreDetailsContent(
                    uiState = GenreDetailsUiState(
                        genre = LibraryGenre("electronic", "Electronic"),
                        albums = listOf(LibraryAlbum("album", "Untrue", "Burial")),
                    ),
                    listState = rememberLazyListState(),
                )
            }
        }

        composeTestRule.onAllNodesWithTag("genre-detail-album-divider").assertCountEquals(2)
    }

    @Test
    fun genreMoreMenuUsesOrderedTracksForPlayNextAndPlaylist() {
        var nextIds: List<String>? = null
        var playlistIds: List<String>? = null
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                GenreDetailsContent(
                    uiState = GenreDetailsUiState(
                        genre = LibraryGenre("electronic", "Electronic"),
                        tracks = listOf(
                            LibraryTrack("first", "First", "Artist"),
                            LibraryTrack("second", "Second", "Artist"),
                        ),
                    ),
                    listState = rememberLazyListState(),
                    onPlayNext = { nextIds = it },
                    onTrackContextBottomSheet = { request ->
                        playlistIds = (request as TrackContextBottomSheetRequest.Playlist).trackIds
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Genre options").performClick()
        composeTestRule.onNodeWithText("Play next").performClick()
        assertEquals(listOf("first", "second"), nextIds)

        composeTestRule.onNodeWithText("Genre options").performClick()
        composeTestRule.onNodeWithText("Add to playlist").performClick()
        assertEquals(listOf("first", "second"), playlistIds)
    }
}
