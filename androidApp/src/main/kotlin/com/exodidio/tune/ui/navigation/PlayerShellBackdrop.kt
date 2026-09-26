package com.exodidio.tune.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.exodidio.tune.ui.theme.LocalTuneColors

// Full-bleed artwork + Tune blur backdrop for the shell. The art layer is
// edge-to-edge only when the stored setting is on AND the window is
// phone-narrow; every other configuration falls back to the Tune blur
// backdrop (Haze source + dominant-color gradient, never mesh). Lyrics
// controls auto-hide after [LyricsControlsIdleMs]; the screen stays on while
// lyrics are open via [keepScreenOn].

const val LyricsControlsIdleMs = 5_000L

internal fun isFullBleedEnabled(settingOn: Boolean, windowWidthDp: Int): Boolean =
    settingOn && playerFillsWindow(windowWidthDp) && !tabletSizedPlayer(windowWidthDp)

internal fun shouldAutoHideLyricsControls(
    lyricsOpen: Boolean,
    idleElapsed: Boolean,
    interacting: Boolean,
    pinned: Boolean,
): Boolean = lyricsOpen && idleElapsed && !interacting && !pinned

/**
 * Shell backdrop layer. Resolves the fullscreen artwork holder internally so
 * callers pass only the path; Tune theme tokens only.
 *
 * @param fullBleed edge-to-edge art when [isFullBleedEnabled] allows it,
 * precomputed by the caller from the stored setting and window width.
 * @param keepScreenOn held while lyrics are open (Task 5 panel wiring).
 */
@Composable
fun PlayerShellBackdrop(
    artworkPath: String?,
    fullBleed: Boolean,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    keepScreenOn: Boolean = false,
) {
    val colors = LocalTuneColors.current
    val artwork = rememberFullscreenArtwork(artworkPath)
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        val previous = view.keepScreenOn
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = previous }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.playerBackdrop)
            .then(if (hazeState == null) Modifier else Modifier.hazeSource(hazeState)),
    ) {
        if (fullBleed && artwork != null) {
            Image(
                bitmap = artwork.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(colors.playerBackdrop.copy(alpha = 0.24f)))
        } else {
            FullScreenPlayerBackground(
                artwork = artwork,
                outgoingArtwork = null,
                incomingArtwork = null,
                crossfadeProgress = 1f,
                isArtworkCrossfading = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
