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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.exodidio.tune.R
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.components.AnimatedPlayPauseSymbol
import com.exodidio.tune.ui.components.AnimatedSkipSymbol
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.TrackAudioQuality
import com.exodidio.tune.ui.components.TrackContextArtist
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import com.exodidio.tune.ui.components.TrackContextMenu
import com.exodidio.tune.ui.components.TrackContextMenuActions
import com.exodidio.tune.ui.components.TrackInfoValue
import com.exodidio.tune.ui.components.TuneMarqueeText
import com.exodidio.tune.ui.components.TunePillButton
import com.exodidio.tune.ui.components.TunePillButtonVariant
import com.exodidio.tune.ui.components.TuneTrackSlider
import com.exodidio.tune.ui.components.trackAudioQuality
import com.exodidio.tune.ui.components.trackInfoValues
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

internal const val PlayerShellQualityBadgeTestTag = "player_shell_quality_badge"

// Fix wave (C2): quality badge mapping mirroring the old
// FullScreenPlayerControls behavior — Lossless/HiRes/Dsd render with the
// track-info dialog on tap; Lossy/Unknown/absent tracks stay hidden (and keep
// the time-row layout stable). Pinned by PlayerShellQualityBadgeTest.
internal fun shellQualityBadge(track: LibraryTrack?): Pair<Int, String>? {
    val quality = track?.let(::trackAudioQuality) ?: return null
    return when (quality) {
        TrackAudioQuality.Lossless -> R.string.track_info_quality_lossless to MaterialSymbols.GraphicEq
        TrackAudioQuality.HiRes -> R.string.track_info_quality_hi_res to MaterialSymbols.Bolt
        TrackAudioQuality.Dsd -> R.string.track_info_quality_dsd to MaterialSymbols.Crown
        else -> null
    }
}

// Fix wave (C2): dialog details mirror the old filter (sample rate, bit
// depth, codec only).
internal fun shellQualityDetails(track: LibraryTrack): List<TrackInfoValue> =
    trackInfoValues(track).filter {
        it.labelRes == R.string.track_info_sample_rate ||
            it.labelRes == R.string.track_info_bit_depth ||
            it.labelRes == R.string.track_info_codec
    }

// Fix wave (C6): scrubber-finish path emitting BOTH the lyrics seek request
// and onSeek with the same target, so pending lyrics seeks clear on confirm.
// Returns the target, or null when there is nothing to seek. Pinned by
// PlayerShellScrubberSeekTest.
internal fun finishShellScrubberSeek(
    durationMs: Long,
    fraction: Float?,
    onSeekRequested: (Long) -> Unit,
    onSeek: (Long) -> Unit,
): Long? {
    if (fraction == null || durationMs <= 0L) return null
    val targetMs = (durationMs * fraction.coerceIn(0f, 1f)).toLong()
    onSeekRequested(targetMs)
    onSeek(targetMs)
    return targetMs
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
 * symmetry with the fullscreen controls. Fix wave: the credits row gains the
 * quality badge (C2, [showQualityBadge] + [contextTrack] with the track-info
 * dialog on tap) and the more-button track menu (C3, play-next/add-to-queue/
 * mood/favorite/go-to-album/go-to-artist/bottom-sheet); the scrubber finish
 * path emits both seek callbacks via [finishShellScrubberSeek] (C6).
 */
@Composable
internal fun PlayerShellNowPlaying(
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
    contextTrack: LibraryTrack? = null,
    showQualityBadge: Boolean = true,
    moodRadioEligibleTrackIds: Set<String> = emptySet(),
    onTrackPlayNext: (String) -> Unit = {},
    onTrackAddToQueue: (String) -> Unit = {},
    onStartMoodRadio: (String) -> Unit = {},
    onTrackGoToAlbum: (String) -> Unit = {},
    onTrackGoToArtist: (String) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
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
    var qualityDialogVisible by remember(trackId) { mutableStateOf(false) }
    var trackMenuExpanded by remember(trackId) { mutableStateOf(false) }
    LaunchedEffect(selectedPanel) {
        if (selectedPanel == PlayerShellPanel.QUEUE) queueBadgeVisible = false
        else {
            delay(QueueStatusBadgeRevealDelayMs.toLong())
            queueBadgeVisible = true
        }
    }
    // Fix wave (C2): badge slot mirrors the old controls — hidden unless the
    // setting is on and the track reports a displayable quality.
    val qualityBadge = shellQualityBadge(contextTrack)
    val qualitySlot = qualityBadge ?: (R.string.track_info_quality_lossless to MaterialSymbols.GraphicEq)
    val qualityVisible = showQualityBadge && qualityBadge != null
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
            // Fix wave (C3): more-button restoring the old metadata track menu
            // (play-next/add-to-queue/mood/favorite/go-to-album/go-to-artist/
            // track-info); inert when the library track is unknown.
            @Composable fun moreButton(onClick: () -> Unit) = FullScreenTransportButton(
                symbol = MaterialSymbols.MoreVert,
                label = stringResource(R.string.player_more),
                onClick = onClick,
                iconSize = 24.dp,
                tint = colors.foregroundSubtle,
                filled = false,
            )
            if (contextTrack == null) moreButton({}) else TrackContextMenu(
                track = contextTrack,
                expanded = trackMenuExpanded,
                onDismiss = { trackMenuExpanded = false },
                playbackQueue = queue,
                onPlayNext = { onTrackPlayNext(it.id) },
                onAddToQueue = { onTrackAddToQueue(it.id) },
                actions = TrackContextMenuActions(
                    moodRadio = contextTrack.id in moodRadioEligibleTrackIds,
                ),
                onStartMoodRadio = { onStartMoodRadio(it.id) },
                onFavoriteChange = { menuTrack, favorite -> onFavoriteToggle(menuTrack.id, favorite) },
                onGoToAlbum = { onTrackGoToAlbum(it.albumId) },
                onGoToArtist = { artist: TrackContextArtist -> onTrackGoToArtist(artist.id) },
                onBottomSheetRequested = { onTrackContextBottomSheet(it) },
            ) { moreButton({ trackMenuExpanded = true }) }
        }
        Spacer(Modifier.height(8.dp))
        TuneTrackSlider(
            value = seekFraction,
            onValueChange = { pendingSeekFraction = it },
            onValueChangeFinished = {
                finishShellScrubberSeek(durationMs, pendingSeekFraction, onSeekRequested, onSeek)
                pendingSeekFraction = null
            },
            enabled = durationMs > 0L && !isPreparing,
            trackHeight = 7.dp,
            modifier = Modifier.semantics { contentDescription = seekLabel },
        )
        // Fix wave (C2): centered quality badge between the time labels (old
        // controls floated it above the slider); tap opens the track-info
        // dialog. Hidden badges keep the elapsed/duration geometry stable.
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = formatPlaybackTime(
                    pendingSeekFraction?.let { (durationMs * it).toLong() } ?: currentPositionMs,
                ),
                color = colors.foregroundSubtle,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.align(Alignment.Center)
                    .alpha(if (qualityVisible) 1f else 0f)
                    .then(
                        if (qualityVisible) Modifier.semantics { testTag = PlayerShellQualityBadgeTestTag }
                        else Modifier.clearAndSetSemantics {},
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .background(fullScreenSecondaryControlBackground(colors))
                    .then(
                        if (qualityVisible) Modifier.clickable(
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { qualityDialogVisible = true }
                        else Modifier,
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                MaterialSymbol(qualitySlot.second, null, size = 12.dp, tint = colors.foregroundSubtle)
                Text(
                    stringResource(qualitySlot.first),
                    color = colors.foregroundSubtle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                )
            }
            Text(
                text = displayedDurationMs?.let(::formatPlaybackTime) ?: "--:--",
                color = colors.foregroundSubtle,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            if (qualityDialogVisible && qualityBadge != null && contextTrack != null) {
                PlayerShellQualityDialog(
                    qualityBadge.first,
                    qualityBadge.second,
                    shellQualityDetails(contextTrack),
                ) { qualityDialogVisible = false }
            }
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

// Fix wave (C2): track-info dialog on quality-badge tap, relocated from the
// deleted FullScreenPlayerMetadata.kt (was FullScreenQualityDialog) with the
// same sample-rate/bit-depth/codec rows.
@Composable
internal fun PlayerShellQualityDialog(labelRes: Int, symbol: String, details: List<TrackInfoValue>, onDismiss: () -> Unit) {
    val colors = LocalTuneColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(colors.card, RoundedCornerShape(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MaterialSymbol(symbol, null, size = 32.dp, tint = colors.textMain)
                    Text(
                        stringResource(labelRes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textMain,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    details.forEach { detail ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(detail.labelRes), style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                            Text(
                                detail.value,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = colors.textMain,
                            )
                        }
                    }
                }
            }
            TunePillButton(
                stringResource(R.string.ok),
                onDismiss,
                TunePillButtonVariant.Primary,
                Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}
