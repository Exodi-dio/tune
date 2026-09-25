package com.exodidio.tune.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import com.exodidio.tune.ui.components.AnchoredPopupMenuHost
import com.exodidio.tune.ui.components.trackInfoArtworkSize
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.sync.LibraryPlaylist
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class PlaylistDetailsContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun heroArtworkMatchesAlbumDetailsSize() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlaylistDetailsContent(
                    PlaylistDetailsUiState(
                        playlist = LibraryPlaylist("mix", "Night drive", emptyList(), "{}"),
                        artworkPaths = listOf("one", "two", "three", "four"),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithTag("playlist-artwork-mosaic").assertHeightIsEqualTo(trackInfoArtworkSize)
    }

    @Test
    fun displaysTrackCountAndDesktopStyleDurationWithoutArtistOrCopyright() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlaylistDetailsContent(
                    PlaylistDetailsUiState(
                        playlist = LibraryPlaylist("mix", "Night drive", listOf("one", "two"), "{}"),
                        tracks = listOf(
                            LibraryTrack("one", "One", "Artist", metadataJson = """{"duration":60}"""),
                            LibraryTrack("two", "Two", "Artist", metadataJson = """{"duration":120}"""),
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Night drive").assertExists()
        composeTestRule.onNodeWithText("2 tracks · 3 min").assertExists()
        composeTestRule.onAllNodesWithTag("playlist-detail-track-divider").assertCountEquals(3)
    }

    @Test
    fun trackMenuEndsWithDestructiveRemoveFromPlaylistAction() {
        val track = LibraryTrack("one", "One", "Artist")
        var removedTrackId: String? = null
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = null) {
                    PlaylistDetailsContent(
                        PlaylistDetailsUiState(
                            playlist = LibraryPlaylist("mix", "Night drive", listOf(track.id), "{}"),
                            tracks = listOf(track),
                        ),
                        playbackQueue = PlaybackQueueSnapshot(activeTrackIds = listOf(track.id)),
                        onTrackRemoveFromPlaylist = { removedTrackId = it },
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Track options")).performClick()
        composeTestRule.onNodeWithText("Remove from playlist").performClick()
        assertEquals(track.id, removedTrackId)
    }

    @Test
    fun heroMoreShowsPlaylistPlaybackAndEditingActions() {
        val track = LibraryTrack("one", "One", "Artist")
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = null) {
                    PlaylistDetailsContent(
                        PlaylistDetailsUiState(
                            playlist = LibraryPlaylist("mix", "Night drive", listOf(track.id), "{}"),
                            tracks = listOf(track),
                        ),
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("More options")).performClick()
        composeTestRule.onNodeWithText("Play next").assertExists()
        composeTestRule.onNodeWithText("Add to queue").assertExists()
        composeTestRule.onNodeWithText("Edit playlist").assertExists()
        composeTestRule.onNodeWithText("Delete playlist").assertExists()
    }

    @Test
    fun reorderModeReplacesTrackOptionsWithDragHandlesAndDoneAction() {
        val tracks = listOf(LibraryTrack("one", "One", "Artist"), LibraryTrack("two", "Two", "Artist"))
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = null) {
                    PlaylistDetailsContent(PlaylistDetailsUiState(LibraryPlaylist("mix", "Night drive", tracks.map { it.id }, "{}"), tracks))
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("More options")).performClick()
        composeTestRule.onNodeWithText("Reorder playlist").performClick()
        composeTestRule.onAllNodes(hasContentDescription("Drag to reorder track")).assertCountEquals(2)
        composeTestRule.onNode(hasContentDescription("Done reordering")).performClick()
        composeTestRule.onNode(hasContentDescription("Track options")).assertExists()
    }

    @Test
    fun favoritesMenuOffersEditButNotDelete() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AnchoredPopupMenuHost(hazeState = null) {
                    PlaylistDetailsContent(
                        PlaylistDetailsUiState(playlist = LibraryPlaylist(FavoritesPlaylistId, "", emptyList(), "{}")),
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("More options")).performClick()
        composeTestRule.onNodeWithText("Edit playlist").assertExists()
        composeTestRule.onAllNodesWithText("Delete playlist").assertCountEquals(0)
    }
}
