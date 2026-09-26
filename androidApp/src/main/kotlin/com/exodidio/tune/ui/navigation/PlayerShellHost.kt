package com.exodidio.tune.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import com.exodidio.tune.PlaybackModel
import com.exodidio.tune.player.PlaybackItem
import com.exodidio.tune.player.PlaybackState
import com.exodidio.tune.sync.metadataObject
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Queue Task 5 mount state: null = both panels closed. The host owns one
// instance and drives PlayerShellQueueMount / PlayerShellLyricsMount from the
// live selectedPanel.
data class PlayerPanelsHostState(val selectedPanel: PlayerShellPanel? = null)

private const val SeekConfirmationToleranceMs = 250L

// Relocated from the deleted FullScreenPlayerControls.kt: the scrubber preview
// clears once playback reaches the requested target (±250ms).
internal fun hasConfirmedSeekPosition(
    seekFraction: Float,
    playbackPositionMs: Long,
    durationMs: Long,
): Boolean {
    if (durationMs <= 0L) return false
    val targetPositionMs = (durationMs * seekFraction.coerceIn(0f, 1f)).toLong()
    return kotlin.math.abs(playbackPositionMs - targetPositionMs) <= SeekConfirmationToleranceMs
}

// W1: maps a pending lyrics seek onto the confirmation tolerance above, so the
// host clears the shared holder exactly when playback confirms the seek.
internal fun hasConfirmedLyricsSeek(
    pendingPositionMs: Long?,
    currentPositionMs: Long,
    durationMs: Long,
): Boolean {
    if (pendingPositionMs == null || durationMs <= 0L) return false
    return hasConfirmedSeekPosition(
        seekFraction = pendingPositionMs.toFloat() / durationMs,
        playbackPositionMs = currentPositionMs,
        durationMs = durationMs,
    )
}

// Relocated from the deleted FullScreenPlayerControls.kt: controls stay
// visible for a normal queue view (pins FullScreenQueuePanelDragTest).
internal fun areFullScreenPlayerControlsVisible(isQueueReordering: Boolean): Boolean = !isQueueReordering

/**
 * Final shell host: replaces FullScreenPlayer. Owns the panel selection
 * state, the shared lyrics pending-seek holder (W1), the Tune blur backdrop +
 * back ordering via PlayerShell (W2), the full-bleed artwork toggle input
 * (W3), and mounts queue/lyrics content from the live selectedPanel (queue
 * Task 5). All playback behavior flows through the identical PlaybackModel
 * callbacks; the offline autoplay refill lives in MainActivity and appends
 * only (W4).
 */
@Composable
internal fun PlayerShellHost(
    visible: Boolean,
    playback: PlaybackModel,
    isFavorite: Boolean = false,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
) {
    val queue = playback.queue
    val item = playback.state.shellItemOrNull() ?: return
    val windowWidthDp = LocalConfiguration.current.screenWidthDp
    var panelsState by remember { mutableStateOf(PlayerPanelsHostState()) }
    val selectedPanel = panelsState.selectedPanel
    val holder = rememberShellLyricsSeekHolder()
    val currentPositionMs = playback.state.shellPositionMsOrZero()
    val durationMs = playback.state.shellDurationMsOrZero()
    // W1: mirror the old FullScreenPlayerControls confirmation path — a shell
    // seek emits a lyrics seek request alongside onSeek, and the pending
    // position clears once playback confirms the seek.
    LaunchedEffect(currentPositionMs, durationMs, holder.requestId) {
        if (hasConfirmedLyricsSeek(holder.pendingPositionMs, currentPositionMs, durationMs)) {
            holder.confirmSeek()
        }
    }
    // Freeze the currently visible cover when the service announces the visual
    // transition before promoting the playback item (mirrors FullScreenPlayer).
    val artwork = rememberFullscreenArtwork(item.artworkPath)
    val activeArtworkCrossfade = playback.artworkCrossfade.takeIf { playback.blendArtworkDuringCrossfade }
    val incomingArtwork = rememberFullscreenArtwork(activeArtworkCrossfade?.toArtworkPath, keepPrevious = false)
    val crossfadeProgress = rememberArtworkCrossfadeProgress(playback.artworkCrossfade)
    val outgoingArtwork = remember(activeArtworkCrossfade?.id) {
        activeArtworkCrossfade?.let { artwork }
    }
    val isArtworkCrossfading = activeArtworkCrossfade != null &&
        outgoingArtwork != null && incomingArtwork != null
    val contextTrack = playback.queueTracks.firstOrNull { it.id == item.trackId }
    val contextMetadata = remember(contextTrack?.metadataJson) { contextTrack?.metadataObject() }
    val metadataDurationMs = (contextMetadata?.get("duration") as? JsonPrimitive)
        ?.contentOrNull
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?.times(1_000L)
    val displayedDurationMs = durationMs.takeIf { it > 0L } ?: metadataDurationMs
    val isPreparing = playback.state is PlaybackState.Preparing
    val isPlaying = playback.state is PlaybackState.Playing
    // The shell panel moves independently from the persistent chrome, so its
    // blur source stays isolated (mirrors the fullscreen panel discipline).
    val shellHazeState = rememberHazeState()
    val glassHazeState = shellHazeState.takeIf { hazeState != null }
    PlayerShell(
        visible = visible,
        onDismiss = onDismiss,
        windowWidthDp = windowWidthDp,
        selectedPanel = selectedPanel,
        onPanelSelected = { panelsState = panelsState.copy(selectedPanel = it) },
        artworkPath = item.artworkPath,
        fullBleed = isFullBleedEnabled(playback.fullBleedArtwork, windowWidthDp),
        keepScreenOn = selectedPanel == PlayerShellPanel.LYRICS,
        hazeState = glassHazeState,
        content = {
            PlayerShellNowPlaying(
                trackId = item.trackId,
                title = item.title,
                artist = item.artist,
                artworkPath = item.artworkPath,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                displayedDurationMs = displayedDurationMs,
                isPreparing = isPreparing,
                isPlaying = isPlaying,
                canNavigatePrevious = queue.canNavigatePrevious(),
                canNavigateNext = queue.canNavigateNext(),
                volume = playback.systemVolume,
                selectedPanel = selectedPanel,
                onPanelSelected = { panelsState = panelsState.copy(selectedPanel = it) },
                queue = queue,
                queueSlide = if (selectedPanel == PlayerShellPanel.QUEUE) 1f else 0f,
                animatedCollapse = if (selectedPanel != null) 1f else 0f,
                queueDragging = false,
                onSeek = playback.onSeek,
                onSeekRequested = { holder.requestSeek(it) },
                onSeekConfirmed = { holder.confirmSeek() },
                onVolumeChange = playback.onSystemVolumeChange,
                onPrevious = playback.onPrevious,
                onPlayPause = playback.onPlayPause,
                onNext = playback.onNext,
                onOpenMediaOutputSwitcher = playback.onOpenMediaOutputSwitcher,
                isFavorite = isFavorite,
                onFavoriteToggle = playback.onFavoriteToggle,
                outgoingArtworkPath = activeArtworkCrossfade?.fromArtworkPath,
                incomingArtworkPath = activeArtworkCrossfade?.toArtworkPath,
                crossfadeProgress = crossfadeProgress,
                isArtworkCrossfading = isArtworkCrossfading,
            )
            if (selectedPanel == PlayerShellPanel.QUEUE) {
                PlayerShellQueueMount(
                    queue = queue,
                    tracks = playback.queueTracks,
                    currentTrackId = item.trackId,
                    isPlaying = isPlaying,
                    onTrackSelected = playback.onQueueTrackSelected,
                    onTrackRemoved = playback.onQueueTrackRemoved,
                    onReorder = playback.onQueueReordered,
                    onShuffleChange = playback.onShuffleChange,
                    onRepeatModeChange = playback.onRepeatModeChange,
                )
            }
            if (selectedPanel == PlayerShellPanel.LYRICS) {
                PlayerShellLyricsMount(
                    trackId = item.trackId,
                    lyrics = playback.lyrics,
                    loading = playback.lyricsLoading,
                    visible = true,
                    currentPositionMs = currentPositionMs,
                    onSeek = playback.onSeek,
                    pendingSeekPositionMs = holder.pendingPositionMs,
                    seekRequestId = holder.requestId,
                    romanization = playback.romanization,
                    romanizationAllowed = playback.romanizationAllowed,
                    onRomanizationInput = playback.onRomanizationInput,
                    onRomanizationToggle = playback.onRomanizationToggle,
                )
            }
        },
    )
}

private fun PlaybackState.shellItemOrNull(): PlaybackItem? = when (this) {
    PlaybackState.Idle, is PlaybackState.Failed -> null
    is PlaybackState.Preparing -> item
    is PlaybackState.Playing -> item
    is PlaybackState.Paused -> item
}

private fun PlaybackState.shellPositionMsOrZero(): Long = when (this) {
    is PlaybackState.Playing -> positionMs
    is PlaybackState.Paused -> positionMs
    else -> 0L
}

private fun PlaybackState.shellDurationMsOrZero(): Long = when (this) {
    is PlaybackState.Playing -> durationMs
    is PlaybackState.Paused -> durationMs
    else -> 0L
}
