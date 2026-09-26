package com.exodidio.tune.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.exodidio.tune.PlaybackModel
import com.exodidio.tune.player.PlaybackItem
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.PlaybackState
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Review-gated only: androidTest is NOT compiled by CI. A reviewer runs this
 * locally with a connected device or emulator. Rewritten for the shell port:
 * every case mounts PlayerShell* composables (the deleted FullScreenPlayer
 * panels are gone).
 */
class FullScreenPlayerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun queueMountShowsTracksAndDispatchesSelection() {
        var selectedTrack: String? = null
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlayerShellQueueMount(
                    queue = PlaybackQueueSnapshot(
                        originalTrackIds = listOf("track-1", "track-2"),
                        activeTrackIds = listOf("track-1", "track-2"),
                        currentIndex = 0,
                    ),
                    tracks = listOf(
                        LibraryTrack(id = "track-1", title = "Test title", artists = "Test artist"),
                        LibraryTrack(id = "track-2", title = "Next track", artists = "Next artist"),
                    ),
                    currentTrackId = "track-1",
                    isPlaying = true,
                    onTrackSelected = { selectedTrack = it },
                    onTrackRemoved = {},
                    onReorder = {},
                    onShuffleChange = {},
                    onRepeatModeChange = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Next track").performClick()
        composeTestRule.runOnIdle { assertEquals("track-2", selectedTrack) }
    }

    @Test
    fun queueHeaderTogglesDispatchShuffleAndRepeat() {
        var shuffle: Boolean? = null
        var repeat: RepeatMode? = null
        var queue by mutableStateOf(
            PlaybackQueueSnapshot(
                originalTrackIds = listOf("track-1", "track-2"),
                activeTrackIds = listOf("track-1", "track-2"),
                currentIndex = 0,
            ),
        )
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlayerShellQueueMount(
                    queue = queue,
                    tracks = listOf(
                        LibraryTrack(id = "track-1", title = "Test title", artists = "Test artist"),
                        LibraryTrack(id = "track-2", title = "Next track", artists = "Next artist"),
                    ),
                    currentTrackId = "track-1",
                    isPlaying = true,
                    onTrackSelected = {},
                    onTrackRemoved = {},
                    onReorder = {},
                    onShuffleChange = {
                        shuffle = it
                        queue = queue.copy(shuffle = it)
                    },
                    onRepeatModeChange = {
                        repeat = it
                        queue = queue.copy(repeatMode = it)
                    },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Shuffle").performClick()
        composeTestRule.onNodeWithContentDescription("Repeat off").performClick()
        composeTestRule.onNodeWithContentDescription("Shuffle on").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Repeat all").assertIsSelected()
        composeTestRule.runOnIdle {
            assertEquals(true, shuffle)
            assertEquals(RepeatMode.All, repeat)
        }
    }

    @Test
    fun qualityBadgeShowsSupportedFormatsAndRespectsSetting() {
        var showQualityBadge by mutableStateOf(true)
        var track by mutableStateOf(
            LibraryTrack(
                id = item.trackId,
                title = item.title,
                artists = item.artist,
                metadataJson = "{\"format\":\"flac\",\"bit_depth\":16,\"sample_rate\":44100}",
            ),
        )
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlayerShellNowPlaying(
                    trackId = item.trackId,
                    title = item.title,
                    artist = item.artist,
                    artworkPath = null,
                    currentPositionMs = 0L,
                    durationMs = 120_000L,
                    displayedDurationMs = 120_000L,
                    isPreparing = false,
                    isPlaying = true,
                    canNavigatePrevious = false,
                    canNavigateNext = true,
                    volume = 0.5f,
                    selectedPanel = null,
                    onPanelSelected = {},
                    queue = PlaybackQueueSnapshot(),
                    queueSlide = 0f,
                    animatedCollapse = 0f,
                    queueDragging = false,
                    onSeek = {},
                    onVolumeChange = {},
                    onPrevious = {},
                    onPlayPause = {},
                    onNext = {},
                    onOpenMediaOutputSwitcher = {},
                    isFavorite = false,
                    onFavoriteToggle = { _, _ -> },
                    contextTrack = track,
                    showQualityBadge = showQualityBadge,
                )
            }
        }

        composeTestRule.onNodeWithTag(PlayerShellQualityBadgeTestTag).assertExists()
        composeTestRule.onNodeWithText("Lossless").assertExists()
        composeTestRule.onNodeWithTag(PlayerShellQualityBadgeTestTag).performClick()
        composeTestRule.onNodeWithText("Sample rate").assertExists()
        composeTestRule.onNodeWithText("44.1 kHz").assertExists()
        composeTestRule.onNodeWithContentDescription("OK").performClick()
        composeTestRule.onNodeWithText("Sample rate").assertDoesNotExist()

        composeTestRule.runOnIdle {
            track = track.copy(metadataJson = "{\"format\":\"mp3\"}")
        }
        composeTestRule.onNodeWithText("Lossless").assertDoesNotExist()

        composeTestRule.runOnIdle {
            track = track.copy(metadataJson = "{\"format\":\"dsf\"}")
            showQualityBadge = false
        }
        composeTestRule.onNodeWithTag(PlayerShellQualityBadgeTestTag).assertDoesNotExist()
    }

    @Test
    fun shellHostTogglesLyricsAndQueuePanels() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                PlayerShellHost(
                    visible = true,
                    playback = PlaybackModel(
                        state = PlaybackState.Playing(item, 0L, 120_000L),
                        queue = PlaybackQueueSnapshot(
                            activeTrackIds = listOf(item.trackId),
                            currentIndex = 0,
                        ),
                        queueTracks = listOf(
                            LibraryTrack(id = item.trackId, title = item.title, artists = item.artist),
                        ),
                    ),
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Queue").performClick()
        composeTestRule.onNodeWithText("Now playing").assertExists()

        composeTestRule.onNodeWithContentDescription("Lyrics").performClick()
        composeTestRule.onNodeWithText("Lyrics are not available for this track.").assertExists()
    }

    private companion object {
        val item = PlaybackItem(
            trackId = "track-1",
            title = "Test title",
            artist = "Test artist",
            audioPath = "/audio/track-1.flac",
        )
    }
}
