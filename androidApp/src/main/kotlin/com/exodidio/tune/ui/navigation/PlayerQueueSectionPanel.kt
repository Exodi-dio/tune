package com.exodidio.tune.ui.navigation

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.theme.LocalTuneColors

@Composable
internal fun PlayerQueueSectionPanel(
    queue: PlaybackQueueSnapshot,
    tracks: List<LibraryTrack>,
    autoplayEnabled: Boolean,
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
                        artist = tracksById[nowId]?.artists ?: ""
                    )
                }
            }
            if (sections.userIds.isNotEmpty()) {
                item(key = "header-user-queue") {
                    QueueSectionHeader(title = "Next in queue", modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                }
                items(sections.userIds, key = { "user-$it" }) { id ->
                    QueueSectionRow(
                        trackId = id,
                        title = tracksById[id]?.title ?: "Unavailable track",
                        artist = tracksById[id]?.artists ?: ""
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
                        artist = tracksById[id]?.artists ?: ""
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

@Composable
private fun QueueSectionHeader(title: String, modifier: Modifier = Modifier) {
    val colors = LocalTuneColors.current
    Text(text = title, color = colors.foregroundSubtle, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
private fun QueueSectionRow(trackId: String, title: String, artist: String, modifier: Modifier = Modifier) {
    val colors = LocalTuneColors.current
    Column(modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(text = title, color = colors.onPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Text(text = artist, color = colors.foregroundSubtle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
