package com.exodidio.tune.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.components.MaterialSymbol
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.components.TrackContextArtist
import com.exodidio.tune.ui.components.TrackContextBottomSheetRequest
import com.exodidio.tune.ui.components.TrackContextMenu
import com.exodidio.tune.ui.components.TrackContextMenuActions
import com.exodidio.tune.ui.components.TunePlayingIndicator
import com.exodidio.tune.ui.components.sliderFilledTrackColor
import com.exodidio.tune.ui.theme.LocalTuneColors
import kotlin.math.abs

internal const val PlayerShellQueueHeaderTestTag = "player_shell_queue_header"

// Fix wave (C1): pure repeat-cycle for the queue header toggle, relocated from
// the deleted FullScreenPlayerQueuePanel.kt (was private RepeatMode.next()).
// Off -> All -> One -> Off; pinned by PlayerShellQueueTogglesTest.
internal fun nextRepeatMode(mode: RepeatMode): RepeatMode = when (mode) {
    RepeatMode.Off -> RepeatMode.All
    RepeatMode.All -> RepeatMode.One
    RepeatMode.One -> RepeatMode.Off
}

// Fix wave (C1): pure shuffle-toggle mapping used by the queue header button.
internal fun toggledShuffle(shuffle: Boolean): Boolean = !shuffle

fun reorderWithinSection(
    fullOrder: List<String>,
    sectionIds: List<String>,
    fromInSection: Int,
    toInSection: Int
): List<String> {
    if (fromInSection !in sectionIds.indices || toInSection !in sectionIds.indices || fromInSection == toInSection) return fullOrder
    val fromId = sectionIds[fromInSection]
    val toId = sectionIds[toInSection]
    val fromGlobal = fullOrder.indexOf(fromId)
    val toGlobal = fullOrder.indexOf(toId)
    if (fromGlobal < 0 || toGlobal < 0) return fullOrder
    return moveQueueTrack(fullOrder, fromGlobal, toGlobal)
}

fun clearNextInQueue(
    activeTrackIds: List<String>,
    currentIndex: Int,
    autoplayIds: Set<String>
): List<String> {
    if (activeTrackIds.isEmpty() || currentIndex !in activeTrackIds.indices) return activeTrackIds
    val now = activeTrackIds[currentIndex]
    val autoplayTail = activeTrackIds.drop(currentIndex + 1).filter { it in autoplayIds }
    return listOf(now) + autoplayTail
}

fun shouldResetQueueScroll(previousTrackId: String?, currentTrackId: String?): Boolean =
    previousTrackId != null && currentTrackId != null && previousTrackId != currentTrackId

fun edgeScrollSpeed(
    top: Float,
    bottom: Float,
    viewportStart: Int,
    viewportEnd: Int,
    zone: Float,
    speed: Float
): Float {
    if (zone <= 0f) return 0f
    val intoStart = (viewportStart + zone) - top
    val intoEnd = bottom - (viewportEnd - zone)
    val reach = when {
        intoStart > 0f && intoEnd <= 0f -> -intoStart
        intoEnd > 0f && intoStart <= 0f -> intoEnd
        else -> return 0f
    }
    val ramp = speed * (0.2f + 0.8f * (abs(reach) / zone).coerceAtMost(1f))
    return if (reach < 0f) -ramp else ramp
}

@Composable
internal fun PlayerQueueSectionPanel(
    queue: PlaybackQueueSnapshot,
    tracks: List<LibraryTrack>,
    autoplayEnabled: Boolean,
    currentTrackId: String = queue.currentTrackId ?: "",
    isPlaying: Boolean = false,
    onTrackSelected: (String) -> Unit = {},
    onTrackRemoved: (String) -> Unit = {},
    onTrackPlayNext: (String) -> Unit = {},
    onReorder: (List<String>) -> Unit = {},
    onClearNext: (List<String>) -> Unit = {},
    onShuffleChange: (Boolean) -> Unit = {},
    onRepeatModeChange: (RepeatMode) -> Unit = {},
    onFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onTrackGoToAlbum: (String) -> Unit = {},
    onTrackGoToArtist: (String) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
    moodRadioEligibleTrackIds: Set<String> = emptySet(),
    onStartMoodRadio: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalTuneColors.current
    val listState = rememberLazyListState()
    val tracksById = remember(tracks) { tracks.associateBy(LibraryTrack::id) }
    val sections = remember(queue.activeTrackIds, queue.currentIndex) {
        splitQueue(
            activeTrackIds = queue.activeTrackIds,
            currentIndex = queue.currentIndex,
            userIds = queue.activeTrackIds.drop((queue.currentIndex + 1).coerceAtLeast(0)).toSet(),
            autoplayIds = emptySet()
        )
    }
    // Reset to the top when the track changes; the panel owns no drag state so
    // this never interrupts an in-flight reorder (drag lives in FullScreenQueuePanel).
    var previousTrackId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentTrackId) {
        if (shouldResetQueueScroll(previousTrackId, currentTrackId)) listState.scrollToItem(0)
        previousTrackId = currentTrackId
    }
    Column(modifier = modifier) {
        // Fix wave (C1): working shuffle + repeat toggles in the queue header
        // (old FullScreenQueuePanel location), calling the live mount
        // callbacks. The queue-toggle status badge in now-playing stays
        // display-only.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .semantics { testTag = PlayerShellQueueHeaderTestTag },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.player_queue),
                color = colors.onPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            PlayerShellModeButton(
                MaterialSymbols.Shuffle,
                stringResource(if (queue.shuffle) R.string.player_shuffle_on else R.string.player_shuffle),
                queue.shuffle,
            ) {
                onShuffleChange(toggledShuffle(queue.shuffle))
            }
            Spacer(Modifier.width(8.dp))
            PlayerShellModeButton(
                if (queue.repeatMode == RepeatMode.One) MaterialSymbols.RepeatOne else MaterialSymbols.Repeat,
                stringResource(
                    when (queue.repeatMode) {
                        RepeatMode.Off -> R.string.player_repeat_off
                        RepeatMode.All -> R.string.player_repeat_all
                        RepeatMode.One -> R.string.player_repeat_one
                    },
                ),
                queue.repeatMode != RepeatMode.Off,
            ) {
                onRepeatModeChange(nextRepeatMode(queue.repeatMode))
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            sections.nowPlayingId?.let { nowId ->
                item(key = "header-now-playing") {
                    QueueSectionHeader(title = "Now playing", modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                }
                item(key = "now-$nowId") {
                    QueueSectionRow(
                        trackId = nowId,
                        title = tracksById[nowId]?.title ?: "Unavailable track",
                        artist = tracksById[nowId]?.artists ?: "",
                        track = tracksById[nowId],
                        isCurrent = true,
                        isPlaying = isPlaying,
                        queue = queue,
                        onTrackPlayNext = onTrackPlayNext,
                        onTrackRemoved = onTrackRemoved,
                        onFavoriteToggle = onFavoriteToggle,
                        onTrackGoToAlbum = onTrackGoToAlbum,
                        onTrackGoToArtist = onTrackGoToArtist,
                        onTrackContextBottomSheet = onTrackContextBottomSheet,
                        moodRadioEligibleTrackIds = moodRadioEligibleTrackIds,
                        onStartMoodRadio = onStartMoodRadio,
                        onClick = { onTrackSelected(nowId) },
                        onRemove = null
                    )
                }
            }
            if (sections.userIds.isNotEmpty()) {
                item(key = "header-user-queue") {
                    QueueSectionHeader(title = "Next in queue", modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                }
                // Clear-next keeps now-playing + autoplay tail (append-only invariant);
                // autoplayIds are empty in this panel today, so this drops user/context upcoming.
                item(key = "clear-next") {
                    Text(
                        text = stringResource(R.string.player_queue_clear_next),
                        color = colors.foregroundSubtle,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clickable {
                                val cleared = clearNextInQueue(
                                    queue.activeTrackIds,
                                    queue.currentIndex,
                                    sections.autoplayIds.toSet()
                                )
                                onClearNext(cleared)
                                onReorder(cleared)
                            }
                    )
                }
                items(sections.userIds, key = { "user-$it" }) { id ->
                    QueueSectionRow(
                        trackId = id,
                        title = tracksById[id]?.title ?: "Unavailable track",
                        artist = tracksById[id]?.artists ?: "",
                        track = tracksById[id],
                        isCurrent = id == currentTrackId,
                        isPlaying = isPlaying,
                        queue = queue,
                        onTrackPlayNext = onTrackPlayNext,
                        onTrackRemoved = onTrackRemoved,
                        onFavoriteToggle = onFavoriteToggle,
                        onTrackGoToAlbum = onTrackGoToAlbum,
                        onTrackGoToArtist = onTrackGoToArtist,
                        onTrackContextBottomSheet = onTrackContextBottomSheet,
                        moodRadioEligibleTrackIds = moodRadioEligibleTrackIds,
                        onStartMoodRadio = onStartMoodRadio,
                        onClick = { onTrackSelected(id) },
                        onRemove = { onTrackRemoved(id) }
                    )
                }
            }
            if (sections.contextIds.isNotEmpty()) {
                item(key = "header-context") {
                    QueueSectionHeader(title = "Next from context", modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                }
                items(sections.contextIds, key = { "ctx-$it" }) { id ->
                    QueueSectionRow(
                        trackId = id,
                        title = tracksById[id]?.title ?: "Unavailable track",
                        artist = tracksById[id]?.artists ?: "",
                        track = tracksById[id],
                        isCurrent = id == currentTrackId,
                        isPlaying = isPlaying,
                        queue = queue,
                        onTrackPlayNext = onTrackPlayNext,
                        onTrackRemoved = onTrackRemoved,
                        onFavoriteToggle = onFavoriteToggle,
                        onTrackGoToAlbum = onTrackGoToAlbum,
                        onTrackGoToArtist = onTrackGoToArtist,
                        onTrackContextBottomSheet = onTrackContextBottomSheet,
                        moodRadioEligibleTrackIds = moodRadioEligibleTrackIds,
                        onStartMoodRadio = onStartMoodRadio,
                        onClick = { onTrackSelected(id) },
                        onRemove = { onTrackRemoved(id) }
                    )
                }
            }
            if (shouldShowAutoplayHeader(sections.autoplayIds, autoplayEnabled)) {
                item(key = "autoplay-heading") {
                    QueueSectionHeader(title = "Autoplay", modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
                }
            }
        }
    }
}

// Section drag mapping (no gesture fork here): a drag from->to inside one section
// commits via onReorder(reorderWithinSection(queue.activeTrackIds, sectionIds, from, to)),
// which delegates to the existing moveQueueTrack path. Edge auto-scroll during a drag
// uses edgeScrollSpeed(top, bottom, viewportStart, viewportEnd, zone, speed).

@Composable
private fun QueueSectionHeader(title: String, modifier: Modifier = Modifier) {
    val colors = LocalTuneColors.current
    Text(text = title, color = colors.foregroundSubtle, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
private fun QueueSectionRow(
    trackId: String,
    title: String,
    artist: String,
    track: LibraryTrack? = null,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    queue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
    onTrackPlayNext: (String) -> Unit = {},
    onTrackRemoved: (String) -> Unit = {},
    onFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onTrackGoToAlbum: (String) -> Unit = {},
    onTrackGoToArtist: (String) -> Unit = {},
    onTrackContextBottomSheet: (TrackContextBottomSheetRequest) -> Unit = {},
    moodRadioEligibleTrackIds: Set<String> = emptySet(),
    onStartMoodRadio: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRemove: (() -> Unit)? = null
) {
    val colors = LocalTuneColors.current
    val moreLabel = stringResource(R.string.player_more)
    // Fix wave (C3): per-row track menu restoring the old queue long-press
    // entry points (play-next/favorite/go-to-album/go-to-artist/track-info)
    // as a more-button; remove stays a direct affordance as before.
    var menuExpanded by remember(trackId) { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, color = colors.onPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(text = artist, color = colors.foregroundSubtle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        if (isCurrent) {
            TunePlayingIndicator(
                isPlaying = isPlaying,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        if (onRemove != null) {
            Text(
                text = stringResource(R.string.track_context_remove_from_queue),
                color = colors.foregroundSubtle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(onClick = onRemove)
            )
        }
        if (track != null) {
            TrackContextMenu(
                track = track,
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false },
                actions = queueTrackContextMenuActions(isCurrent).copy(
                    moodRadio = track.id in moodRadioEligibleTrackIds,
                ),
                playbackQueue = queue,
                onRemoveFromQueue = { onTrackRemoved(it.id) },
                onPlayNext = { onTrackPlayNext(it.id) },
                onStartMoodRadio = { onStartMoodRadio(it.id) },
                onFavoriteChange = { contextTrack, favorite -> onFavoriteToggle(contextTrack.id, favorite) },
                onGoToAlbum = { onTrackGoToAlbum(it.albumId) },
                onGoToArtist = { artist: TrackContextArtist -> onTrackGoToArtist(artist.id) },
                onBottomSheetRequested = { onTrackContextBottomSheet(it) },
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = moreLabel }
                        .clickable(
                            onClick = { menuExpanded = true },
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    MaterialSymbol(
                        MaterialSymbols.MoreVert,
                        contentDescription = null,
                        tint = colors.foregroundSubtle,
                        size = 20.dp,
                    )
                }
            }
        }
    }
}

// Fix wave (C1): shuffle/repeat toggle buttons, relocated from the deleted
// FullScreenPlayerQueuePanel.kt (was PlayerModeButton) with identical visuals.
@Composable
internal fun PlayerShellModeButton(symbol: String, label: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalTuneColors.current
    val backgroundColor by animateColorAsState(
        targetValue = if (active) {
            sliderFilledTrackColor(colors, isInteracting = false)
        } else {
            fullScreenSecondaryControlBackground(colors)
        },
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "queue-mode-background",
    )
    val iconColor by animateColorAsState(
        if (active) colors.playerBackdrop.copy(alpha = 0.72f) else colors.onPrimary,
        tween(220, easing = FastOutSlowInEasing),
        label = "queue-mode-icon"
    )
    Box(
        Modifier
            .width(72.dp)
            .height(48.dp)
            .semantics { contentDescription = label; selected = active }
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ), contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(72.dp)
                .height(36.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(1.dp, colors.borderGlass, CircleShape), contentAlignment = Alignment.Center
        ) {
            MaterialSymbol(
                symbol = symbol,
                contentDescription = null,
                tint = iconColor,
                size = 22.dp
            )
        }
    }
}

// Relocated from the deleted FullScreenPlayerQueuePanel.kt: section drags
// commit through the existing moveQueueTrack path, and the context-menu
// policy helpers stay pinned by FullScreenQueuePanelDragTest.
internal fun moveQueueTrack(trackIds: List<String>, fromIndex: Int, toIndex: Int): List<String> =
    trackIds.toMutableList().apply {
        if (fromIndex in indices && toIndex in indices && fromIndex != toIndex) {
            add(toIndex, removeAt(fromIndex))
        }
    }

/** Commits the latest Compose-backed local order when a reorder drag ends. */
internal fun commitQueueReorder(
    latestOrderedIds: androidx.compose.runtime.State<List<String>>,
    latestOnReorder: androidx.compose.runtime.State<(List<String>) -> Unit>,
) = latestOnReorder.value(latestOrderedIds.value)

internal fun queueTrackContextMenuActions(isCurrent: Boolean) = TrackContextMenuActions(
    removeFromQueue = !isCurrent,
    addToQueue = false,
)

internal fun shouldOpenQueueTrackContextMenu(
    longPressX: Float,
    rowWidthPx: Int,
    dragHandleWidthPx: Float,
): Boolean = longPressX < rowWidthPx - dragHandleWidthPx
