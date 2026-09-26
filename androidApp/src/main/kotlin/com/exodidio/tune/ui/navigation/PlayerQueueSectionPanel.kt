package com.exodidio.tune.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.theme.LocalTuneColors
import kotlin.math.abs

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
    onTrackSelected: (String) -> Unit = {},
    onTrackRemoved: (String) -> Unit = {},
    onReorder: (List<String>) -> Unit = {},
    onClearNext: (List<String>) -> Unit = {},
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
        Text(
            text = stringResource(R.string.player_queue),
            color = colors.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
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
                        text = "Clear next",
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRemove: (() -> Unit)? = null
) {
    val colors = LocalTuneColors.current
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
    }
}
