package com.exodidio.tune.ui.navigation

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.exodidio.tune.player.PlaybackItem
import com.exodidio.tune.player.PlaybackState
import com.exodidio.tune.settings.PlayerTheme
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.screens.AppearanceContent
import com.exodidio.tune.ui.theme.TuneTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class AdaptiveScreenshotsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun shotDir(): File {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(ctx.getExternalFilesDir(null), "shots")
        dir.mkdirs()
        return dir
    }

    private fun saveShot(name: String) {
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File(shotDir(), name)
        out.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun solidBitmap(argb: Int): Bitmap {
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(argb)
        return bmp
    }

    @Test
    fun captureAdaptiveShots() {
        val item = PlaybackItem(
            trackId = "shot-1",
            title = "Midnight Reef",
            artist = "Tune House Band",
            audioPath = "/audio/shot-1.flac",
        )

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                AppearanceContent(
                    themeMode = ThemeMode.Dark,
                    onThemeModeSelected = {},
                    reduceTransparency = false,
                    onReduceTransparencyChanged = {},
                    playerTheme = PlayerTheme.Adaptive,
                    onPlayerThemeSelected = {},
                    showVolumeSlider = true,
                    onShowVolumeSliderChanged = {},
                    fullscreenArtwork = true,
                    onFullscreenArtworkChanged = {},
                    fullscreenLyrics = true,
                    onFullscreenLyricsChanged = {},
                )
            }
        }
        saveShot("01-appearance-adaptive.png")

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                FullScreenPlayer(
                    visible = true,
                    dragProgress = 0f,
                    isDragging = false,
                    openingFromMiniPlayerSwipe = false,
                    playbackState = PlaybackState.Playing(item, 30_000L, 180_000L),
                    showQualityBadge = false,
                    playerTheme = PlayerTheme.Standard,
                    showVolumeSlider = true,
                    fullscreenArtwork = false,
                    fullscreenLyrics = true,
                    volume = 0.6f,
                    onSeek = {},
                    onVolumeChange = {},
                    onPrevious = {},
                    onPlayPause = {},
                    onNext = {},
                    onOpenMediaOutputSwitcher = {},
                    onDismiss = {},
                )
            }
        }
        saveShot("02-player-standard.png")

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                FullScreenPlayer(
                    visible = true,
                    dragProgress = 0f,
                    isDragging = false,
                    openingFromMiniPlayerSwipe = false,
                    playbackState = PlaybackState.Playing(item, 30_000L, 180_000L),
                    showQualityBadge = false,
                    playerTheme = PlayerTheme.Adaptive,
                    showVolumeSlider = true,
                    fullscreenArtwork = true,
                    fullscreenLyrics = true,
                    volume = 0.6f,
                    onSeek = {},
                    onVolumeChange = {},
                    onPrevious = {},
                    onPlayPause = {},
                    onNext = {},
                    onOpenMediaOutputSwitcher = {},
                    onDismiss = {},
                )
            }
        }
        saveShot("03-player-adaptive.png")

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                FullScreenPlayer(
                    visible = true,
                    dragProgress = 0f,
                    isDragging = false,
                    openingFromMiniPlayerSwipe = false,
                    playbackState = PlaybackState.Playing(item, 30_000L, 180_000L),
                    showQualityBadge = false,
                    playerTheme = PlayerTheme.Adaptive,
                    showVolumeSlider = false,
                    fullscreenArtwork = true,
                    fullscreenLyrics = false,
                    volume = 0.6f,
                    onSeek = {},
                    onVolumeChange = {},
                    onPrevious = {},
                    onPlayPause = {},
                    onNext = {},
                    onOpenMediaOutputSwitcher = {},
                    onDismiss = {},
                )
            }
        }
        saveShot("04-player-adaptive-no-volume.png")

        val red = solidBitmap(0xFFC0392B.toInt()).extractAdaptivePlayerColors()
        val blue = solidBitmap(0xFF2471A3.toInt()).extractAdaptivePlayerColors()
        val sand = solidBitmap(0xFFD9C7A7.toInt()).extractAdaptivePlayerColors()
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                Column(
                    Modifier.fillMaxSize().background(red.background).padding(24.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(120.dp).background(red.background))
                    Text("Adaptive red — accent on background", color = red.accent)
                    Text("Dim line mirrors lyrics secondary", color = red.dim)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).background(blue.background))
                    Text("Blue adaptive preview", color = blue.accent)
                    Text("Sand sample", color = sand.accent)
                }
            }
        }
        saveShot("05-adaptive-swatches.png")
    }
}
