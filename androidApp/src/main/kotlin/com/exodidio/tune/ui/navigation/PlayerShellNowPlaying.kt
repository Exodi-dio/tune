package com.exodidio.tune.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.ui.components.AnimatedPlayPauseSymbol
import com.exodidio.tune.ui.components.AnimatedSkipSymbol
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.TuneMarqueeText
import com.exodidio.tune.ui.components.TuneTrackSlider
import com.exodidio.tune.ui.theme.LocalTuneColors
import com.exodidio.tune.ui.theme.TuneColors
import kotlinx.coroutines.delay

// Now-playing order port: credits + scrubber + transport + volume + bottom row
// (lyrics/output/queue toggles). Pause shrinks the sleeve to 0.86x with a 500ms
// ease-out; an open panel collapses the sleeve via Task 2 shellCollapseProgress.
// Tune theme tokens only; queue sections/autoplay and lyric lines excluded.

internal fun shellArtworkScale(isPlaying: Boolean): Float =
    if (isPlaying) 1.0f else 0.86f

internal fun shellNowPlayingOrder(): List<String> =
    listOf("credits", "scrubber", "transport", "volume", "bottomRow")

// Relocated from the deleted FullScreenPlayerControls.kt: the shell queue
// toggle keeps the shuffle/repeat status badge, and the scrubber keeps the
// shared playback time formatter.
internal const val QueueButtonSelectionTransitionDurationMs = 220
internal const val QueueStatusBadgeRevealDelayMs = QueueButtonSelectionTransitionDurationMs + 16
internal const val FullScreenQueueStatusBadgeTestTag = "full_screen_queue_status_badge"

internal fun queueStatusBadgeSymbol(queue: PlaybackQueueSnapshot): String? = when {
    queue.shuffle -> MaterialSymbols.Shuffle
    queue.repeatMode == RepeatMode.One -> MaterialSymbols.RepeatOne
    queue.repeatMode == RepeatMode.All -> MaterialSymbols.Repeat
    else -> null
}

internal fun fullScreenSecondaryControlBackground(colors: TuneColors): Color = colors.sliderInactive.copy(alpha = 0.06f)

@Composable
internal fun BoxScope.QueueStatusBadge(symbol: String) {
    val colors = LocalTuneColors.current
    Box(
        Modifier.align(Alignment.TopEnd).padding(2.dp).size(20.dp)
            .semantics { testTag = FullScreenQueueStatusBadgeTestTag }
            .background(fullScreenSecondaryControlBackground(colors), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol(symbol, null, tint = colors.onPrimary, size = 13.dp)
    }
}

@Composable
internal fun FullScreenTransportButton(
    symbol: String? = null,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconSize: Dp = 32.dp,
    tint: Color? = null,
    containerColor: Color? = null,
    filled: Boolean = true,
    isPlaying: Boolean? = null,
    skipForward: Boolean? = null,
) {
    val colors = LocalTuneColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        Modifier.size(64.dp).then(if (containerColor == null) Modifier else Modifier.padding(8.dp).background(containerColor, CircleShape))
            .semantics { contentDescription = label }
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val iconTint = tint ?: if (enabled || isPlaying != null) colors.onPrimary else colors.foregroundSubtle
        when {
            isPlaying != null -> AnimatedPlayPauseSymbol(isPlaying, !enabled, isPressed, iconTint, iconSize, 64.dp)
            skipForward != null -> AnimatedSkipSymbol(skipForward, isPressed, iconTint, iconSize, 64.dp)
            else -> MaterialSymbol(requireNotNull(symbol), null, tint = iconTint, size = iconSize, filled = filled)
        }
    }
}

// Shared playback time formatter, reused by the shell now-playing scrubber.
internal fun formatPlaybackTime(timeMs: Long): String {
    val seconds = (timeMs.coerceAtLeast(0L) / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/**
 * Shared pending-seek holder between the shell scrubber and the lyrics mount.
 * A shell seek emits a lyrics seek request alongside [onSeek] so the lyrics
 * line follows scrubber seeks; the holder clears once playback confirms the
 * seek. The shell parent owns one instance and passes its
 * [pendingPositionMs]/[requestId] into [PlayerShellLyricsMount].
 */
class ShellLyricsSeekHolder {
    var pendingPositionMs: Long? by mutableStateOf(null)
        private set
    var requestId: Long by mutableLongStateOf(0L)
        private set

    fun requestSeek(positionMs: Long) {
        pendingPositionMs = positionMs
        requestId += 1
    }

    fun confirmSeek() {
        pendingPositionMs = null
    }
}

@Composable
fun rememberShellLyricsSeekHolder(): ShellLyricsSeekHolder =
    remember { ShellLyricsSeekHolder() }

// Sleeve footprint: expanded now-playing cover collapsing to the compact
// panel-open size (previously inline 288f/80f magic).
internal const val ShellSleeveExpandedDp = 288f
internal const val ShellSleeveCollapsedDp = 80f

/**
 * Now-playing column in reference order. Keeps every existing now-playing
 * callback (seek, volume, previous/play-pause/next, favorite, panel selection,
 * media-output); queue-section/autoplay and lyric-line logic excluded. Scrubber
 * seeks emit [onSeekRequested] alongside [onSeek] so the shared lyrics
 * pending-seek holder stays in sync; [onSeekConfirmed] is parent-owned for
 * symmetry with the fullscreen controls.
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
    queue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
    queueSlide: Float,
    animatedCollapse: Float,
    queueDragging: Boolean,
    onSeek: (Long) -> Unit,
    onSeekRequested: (Long) -> Unit = {},
    onSeekConfirmed: () -> Unit = {},
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
    var queueBadgeVisible by remember { mutableStateOf(true) }
    LaunchedEffect(selectedPanel) {
        if (selectedPanel == PlayerShellPanel.QUEUE) queueBadgeVisible = false
        else {
            delay(QueueStatusBadgeRevealDelayMs.toLong())
            queueBadgeVisible = true
        }
    }
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
                pendingSeekFraction?.let {
                    val targetMs = (durationMs * it.coerceIn(0f, 1f)).toLong()
                    onSeekRequested(targetMs)
                    onSeek(targetMs)
                }
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
                if (!queueSelected && queueBadgeVisible) queueStatusBadgeSymbol(queue)?.let { QueueStatusBadge(it) }
            }
        }
    }
}
