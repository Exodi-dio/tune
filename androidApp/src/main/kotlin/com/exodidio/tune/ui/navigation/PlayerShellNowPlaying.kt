package com.exodidio.tune.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.TuneMarqueeText
import com.exodidio.tune.ui.components.TuneTrackSlider
import com.exodidio.tune.ui.theme.LocalTuneColors

// Now-playing order port: credits + scrubber + transport + volume + bottom row
// (lyrics/output/queue toggles). Pause shrinks the sleeve to 0.86x with a 500ms
// ease-out; an open panel collapses the sleeve via Task 2 shellCollapseProgress.
// Tune theme tokens only; queue sections/autoplay and lyric lines excluded.

internal fun shellArtworkScale(isPlaying: Boolean): Float =
    if (isPlaying) 1.0f else 0.86f

internal fun shellNowPlayingOrder(): List<String> =
    listOf("credits", "scrubber", "transport", "volume", "bottomRow")

// Sleeve footprint: expanded now-playing cover collapsing to the compact
// panel-open size (previously inline 288f/80f magic).
internal const val ShellSleeveExpandedDp = 288f
internal const val ShellSleeveCollapsedDp = 80f

/**
 * Now-playing column in reference order. Keeps every existing now-playing
 * callback (seek, volume, previous/play-pause/next, favorite, panel selection,
 * media-output); queue-section/autoplay and lyric-line logic excluded.
 */
@Composable
fun PlayerShellNowPlaying(
    trackId: String,
    title: String,
    artist: String,
    artworkPath: String?,
    currentPositionMs: Long,
    durationMs: Long,
    displayedDurationMs: Long?,
    isPreparing: Boolean,
    isPlaying: Boolean,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    volume: Float,
    selectedPanel: PlayerShellPanel?,
    onPanelSelected: OnPlayerShellPanelSelected,
    queueSlide: Float,
    animatedCollapse: Float,
    queueDragging: Boolean,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenMediaOutputSwitcher: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: (String, Boolean) -> Unit,
    outgoingArtworkPath: String? = null,
    incomingArtworkPath: String? = null,
    crossfadeProgress: Float = 1f,
    isArtworkCrossfading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
    val collapse = shellCollapseProgress(
        queueOpen = selectedPanel == PlayerShellPanel.QUEUE,
        lyricsOpen = selectedPanel == PlayerShellPanel.LYRICS,
        queueSlide = queueSlide,
        animatedCollapse = animatedCollapse,
        queueDragging = queueDragging,
    )
    val sleeveScale by animateFloatAsState(
        targetValue = shellArtworkScale(isPlaying),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "shell-artwork-scale",
    )
    val artwork = rememberFullscreenArtwork(artworkPath)
    val outgoingArtwork = rememberFullscreenArtwork(outgoingArtworkPath, keepPrevious = false)
    val incomingArtwork = rememberFullscreenArtwork(incomingArtworkPath, keepPrevious = false)
    val artworkSize = (ShellSleeveExpandedDp * (1f - collapse) + ShellSleeveCollapsedDp * collapse).dp
    val seekLabel = stringResource(R.string.player_seek)
    val volumeLabel = stringResource(R.string.player_volume)
    var pendingSeekFraction by remember(trackId) { mutableStateOf<Float?>(null) }
    val seekFraction = pendingSeekFraction
        ?: if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FullScreenPlayerArtwork(
                artwork = artwork,
                outgoingArtwork = outgoingArtwork,
                incomingArtwork = incomingArtwork,
                crossfadeProgress = crossfadeProgress,
                isArtworkCrossfading = isArtworkCrossfading,
                modifier = Modifier
                    .size(artworkSize)
                    .graphicsLayer {
                        scaleX = sleeveScale
                        scaleY = sleeveScale
                    },
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TuneMarqueeText(
                    text = title,
                    color = colors.onPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                TuneMarqueeText(
                    text = artist,
                    color = colors.foregroundSubtle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(4.dp))
            FullScreenTransportButton(
                symbol = if (isFavorite) MaterialSymbols.Favorite else MaterialSymbols.FavoriteBorder,
                label = stringResource(R.string.player_heart),
                onClick = { onFavoriteToggle(trackId, !isFavorite) },
                iconSize = 24.dp,
                filled = false,
            )
        }
        Spacer(Modifier.height(8.dp))
        TuneTrackSlider(
            value = seekFraction,
            onValueChange = { pendingSeekFraction = it },
            onValueChangeFinished = {
                pendingSeekFraction?.let { onSeek((durationMs * it.coerceIn(0f, 1f)).toLong()) }
                pendingSeekFraction = null
            },
            enabled = durationMs > 0L && !isPreparing,
            trackHeight = 7.dp,
            modifier = Modifier.semantics { contentDescription = seekLabel },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatPlaybackTime(
                    pendingSeekFraction?.let { (durationMs * it).toLong() } ?: currentPositionMs,
                ),
                color = colors.foregroundSubtle,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = displayedDurationMs?.let(::formatPlaybackTime) ?: "--:--",
                color = colors.foregroundSubtle,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FullScreenTransportButton(
                symbol = MaterialSymbols.SkipPrevious,
                label = stringResource(R.string.player_previous),
                onClick = onPrevious,
                enabled = canNavigatePrevious,
                iconSize = 36.dp,
                skipForward = false,
            )
            FullScreenTransportButton(
                label = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                onClick = onPlayPause,
                enabled = !isPreparing,
                iconSize = 48.dp,
                isPlaying = isPlaying,
            )
            FullScreenTransportButton(
                symbol = MaterialSymbols.SkipNext,
                label = stringResource(R.string.player_next),
                onClick = onNext,
                enabled = canNavigateNext,
                iconSize = 36.dp,
                skipForward = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MaterialSymbol(MaterialSymbols.VolumeDown, null, tint = colors.foregroundSubtle, size = 20.dp, filled = true)
            Spacer(Modifier.width(10.dp))
            TuneTrackSlider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = onVolumeChange,
                trackHeight = 7.dp,
                modifier = Modifier.weight(1f).semantics { contentDescription = volumeLabel },
            )
            Spacer(Modifier.width(10.dp))
            MaterialSymbol(MaterialSymbols.VolumeUp, null, tint = colors.foregroundSubtle, size = 20.dp, filled = true)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val lyricsSelected = selectedPanel == PlayerShellPanel.LYRICS
            val queueSelected = selectedPanel == PlayerShellPanel.QUEUE
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FullScreenTransportButton(
                    symbol = MaterialSymbols.Chat,
                    label = stringResource(R.string.player_lyrics),
                    onClick = { onPanelSelected(if (lyricsSelected) null else PlayerShellPanel.LYRICS) },
                    iconSize = 24.dp,
                    tint = if (lyricsSelected) colors.onPrimary else colors.foregroundSubtle,
                    filled = false,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FullScreenTransportButton(
                    symbol = MaterialSymbols.Airplay,
                    label = stringResource(R.string.player_cast),
                    onClick = onOpenMediaOutputSwitcher,
                    iconSize = 24.dp,
                    tint = colors.foregroundSubtle,
                    filled = false,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FullScreenTransportButton(
                    symbol = MaterialSymbols.QueueMusic,
                    label = stringResource(R.string.player_queue),
                    onClick = { onPanelSelected(if (queueSelected) null else PlayerShellPanel.QUEUE) },
                    iconSize = 24.dp,
                    tint = if (queueSelected) colors.onPrimary else colors.foregroundSubtle,
                    filled = false,
                )
            }
        }
    }
}
