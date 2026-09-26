package com.exodidio.tune.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.lyrics.RomanizationUiState
import com.exodidio.tune.ui.theme.LocalTuneColors

const val LyricsScrollLeadMinMs: Long = 350L
const val LyricsScrollLeadMaxMs: Long = 500L

internal fun activeLyricIndex(lines: List<PlayerLyricLine>, positionMs: Long): Int =
    lines.indexOfLast { (it.timestampSeconds ?: Float.MAX_VALUE) <= positionMs / 1_000f }

internal fun lyricsScrollLeadMs(gapMs: Long): Long =
    gapMs.coerceIn(LyricsScrollLeadMinMs, LyricsScrollLeadMaxMs)

// Pre-merge fix (F1): toggle visibility mirroring the deleted
// FullScreenPlayerLyricsPanel block (allowed + input-current + supported).
// Pinned by PlayerLyricsPanelTest.
internal fun shouldShowRomanizationToggle(
    romanizationAllowed: Boolean,
    current: Boolean,
    supported: Boolean,
): Boolean = romanizationAllowed && current && supported

internal fun applyLyricsOffset(positionMs: Long, offsetMs: Int): Long =
    (positionMs - offsetMs.toLong()).coerceAtLeast(0L)

internal fun lyricsSeekTargetMs(timestampSeconds: Float, offsetMs: Int): Long =
    (timestampSeconds * 1_000).toLong().plus(offsetMs.toLong()).coerceAtLeast(0L)

@Composable
internal fun PlayerLyricsPanel(
    trackId: String,
    lyrics: String?,
    loading: Boolean = false,
    currentPositionMs: Long,
    pendingSeekPositionMs: Long? = null,
    seekRequestId: Long = 0L,
    lyricsOffsetMs: Int = 0,
    romanization: RomanizationUiState = RomanizationUiState(),
    romanizationAllowed: Boolean = false,
    onRomanizationInput: (List<String>, Boolean) -> Unit = { _, _ -> },
    onRomanizationToggle: () -> Unit = {},
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = remember(lyrics) { lyrics?.let(::parsePlayerLyrics).orEmpty() }
    val synced = remember(parsed) { parsed.filter { it.timestampSeconds != null } }
    val primary = remember(parsed, synced) { (synced.ifEmpty { parsed }).map { it.primary } }
    LaunchedEffect(trackId, primary) { onRomanizationInput(primary, true) }
    // F1: mirror the deleted panel's current/toggle inputs using the threaded
    // romanizationAllowed/onRomanizationToggle (secondary swaps only when enabled).
    val current = romanization.input == primary && !loading
    val secondary = if (romanizationAllowed && current && romanization.enabled) romanization.secondary else emptyList()
    val showToggle = shouldShowRomanizationToggle(romanizationAllowed, current, romanization.supported)
    val displayed = displayedLyricsPositionMs(currentPositionMs, pendingSeekPositionMs)
    val adjusted = applyLyricsOffset(displayed, lyricsOffsetMs)
    val activeIndex = remember(synced, adjusted) { activeLyricIndex(synced, adjusted) }
    val listState = rememberLazyListState()
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    val latestOnSeek by rememberUpdatedState(onSeek)
    val lyricTapSlopPx = with(LocalDensity.current) { 20.dp.toPx() }
    var isBrowsing by remember(trackId) { mutableStateOf(false) }
    LaunchedEffect(seekRequestId) {
        if (seekRequestId > 0L && activeIndex >= 0) {
            isBrowsing = false
            // Brief drift fixed: animateScrollToItem takes (index, scrollOffset);
            // no tween overload exists, so jump directly (matches the follower below).
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(trackId) { isBrowsing = false }
    LaunchedEffect(isUserDragging) {
        if (shouldEnterLyricsBrowseMode(isUserDragging, false)) isBrowsing = true
    }
    LaunchedEffect(synced, activeIndex, isBrowsing) {
        if (activeIndex < 0 || isBrowsing) return@LaunchedEffect
        // F5: pre-scroll lead helper is pure and pinned by unit tests; the
        // follower jumps directly (no tween overload), so the discarded call
        // is removed here with no behavior change.
        listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
    }
    // F1: Box mirrors the deleted panel (content + BottomEnd toggle).
    Box(modifier = modifier.padding(top = 8.dp)) {
    when {
        loading -> Text(stringResource(R.string.player_lyrics), modifier = Modifier.padding(top = 24.dp))
        lyrics.isNullOrBlank() -> Text(stringResource(R.string.player_lyrics_not_available), modifier = Modifier.padding(top = 24.dp))
        synced.isNotEmpty() -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 72.dp)) {
            itemsIndexed(synced, key = { index, _ -> index }) { index, line ->
                val distance = if (activeIndex >= 0) kotlin.math.abs(index - activeIndex) else Int.MAX_VALUE
                val targetOpacity = if (isBrowsing) 1f else when (distance) { 0 -> 1f; 1 -> 0.25f; 2 -> 0.15f; else -> 0.10f }
                val opacity by animateFloatAsState(targetOpacity, tween(300, easing = FastOutSlowInEasing), label = "lyric-opacity")
                val targetBlur = if (isBrowsing) 0.dp else syncedLyricBlurRadius(distance)
                val blur by animateDpAsState(targetBlur, tween(300, easing = FastOutSlowInEasing), label = "lyric-blur")
                val scale by animateFloatAsState(if (!isBrowsing && distance == 0) 1.04f else 1f, tween(300, easing = FastOutSlowInEasing), label = "lyric-scale")
                Column(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp, end = 16.dp)
                        .blur(blur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0.5f); clip = false }
                        .pointerInput(line.timestampSeconds, lyricsOffsetMs) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPosition = down.position
                                var dragDistancePx = 0f
                                var pressed = true
                                while (pressed) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    dragDistancePx = maxOf(dragDistancePx, (change.position - startPosition).getDistance())
                                    pressed = change.pressed
                                }
                                if (shouldSeekFromLyricTap(dragDistancePx, lyricTapSlopPx)) {
                                    isBrowsing = false
                                    latestOnSeek(lyricsSeekTargetMs(line.timestampSeconds!!, lyricsOffsetMs))
                                }
                            }
                        }
                        .semantics {
                            role = Role.Button
                            onClick {
                                isBrowsing = false
                                latestOnSeek(lyricsSeekTargetMs(line.timestampSeconds!!, lyricsOffsetMs))
                                true
                            }
                        }
                ) {
                    Text(
                        text = line.primary,
                        color = LocalTuneColors.current.onPrimary.copy(alpha = opacity),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    (secondary.getOrNull(index) ?: line.secondary)?.let {
                        Text(text = it, color = LocalTuneColors.current.foregroundSubtle.copy(alpha = opacity), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(parsed, key = { index, _ -> index }) { index, line ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(text = line.primary, color = LocalTuneColors.current.onPrimary, style = MaterialTheme.typography.bodyLarge)
                    (secondary.getOrNull(index) ?: line.secondary)?.let {
                        Text(text = it, color = LocalTuneColors.current.foregroundSubtle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    if (showToggle) {
        RomanizationToggle(
            state = romanization,
            onClick = onRomanizationToggle,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
    }
}

// Relocated from the deleted FullScreenPlayerLyricsPanel.kt: lyric parsing,
// seek/follow/browse policy, and active-line blur — pinned by
// PlayerLyricsParserTest / PlayerLyricsPanelTest / PlayerShellSeekLyricsSyncTest.
internal data class PlayerLyricLine(
    val primary: String,
    val secondary: String? = null,
    val timestampSeconds: Float? = null,
)

private val TimestampedLyricLine = Regex("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)$")
private val BilingualSeparator = Regex("\\s*\\^\\s*|\\s*/\\s*")

internal enum class LyricsSeekDirection { Backward, Forward }

internal fun lyricsSeekDirection(targetIndex: Int, firstVisibleIndex: Int): LyricsSeekDirection =
    if (targetIndex < firstVisibleIndex) LyricsSeekDirection.Backward else LyricsSeekDirection.Forward

internal fun parsePlayerLyrics(content: String): List<PlayerLyricLine> = content.lineSequence()
    .mapNotNull { rawLine ->
        val match = TimestampedLyricLine.matchEntire(rawLine)
        val timestamp = match?.let { it.groupValues[1].toFloat() * 60f + it.groupValues[2].toFloat() }
        val text = match?.groupValues?.get(3) ?: rawLine
        text.trim().takeIf(String::isNotEmpty)?.let { parsePlayerLyricText(it, timestamp) }
    }
    .toList()

internal fun hasSyncedPlayerLyrics(content: String?): Boolean = content != null && parsePlayerLyrics(content).any { it.timestampSeconds != null }

/** Resume automatic following once playback reaches the tapped lyric or passes it. */
internal fun shouldResumeLyricsAutoScroll(
    selectedLineIndex: Int?,
    activeIndex: Int,
    activeIndexWhenLineSelected: Int?,
    selectedLineAnimationComplete: Boolean,
): Boolean = selectedLineAnimationComplete && selectedLineIndex != null &&
    activeIndex >= selectedLineIndex && activeIndex != activeIndexWhenLineSelected

/** The active line may advance beyond the visible viewport while the app is backgrounded. */
internal fun shouldFollowLyricsActiveLine(previousActiveLineInViewport: Boolean, returnedToForeground: Boolean): Boolean =
    previousActiveLineInViewport || returnedToForeground

/** A repeat/replay restarts the same track near zero without changing its track ID. */
internal fun shouldResetLyricsForReplay(previousPositionMs: Long, currentPositionMs: Long): Boolean =
    previousPositionMs > 1_000L && currentPositionMs <= 1_000L

/** Prefer a slider's requested position until playback confirms the seek. */
internal fun displayedLyricsPositionMs(playbackPositionMs: Long, pendingSeekPositionMs: Long?): Long =
    pendingSeekPositionMs ?: playbackPositionMs

/** Programmatic lyric positioning must not be interpreted as manual browsing. */
internal fun shouldEnterLyricsBrowseMode(isUserDragging: Boolean, isFollowingSelectedLine: Boolean = false): Boolean =
    isUserDragging && !isFollowingSelectedLine

/** Small finger drift on a lyric row is still a seek, not a manual browse. */
internal fun shouldSeekFromLyricTap(dragDistancePx: Float, tapSlopPx: Float): Boolean = dragDistancePx <= tapSlopPx

internal fun syncedLyricBlurRadius(distance: Int) = when (distance) {
    0 -> 0.dp
    1 -> 0.35.dp
    2 -> 1.25.dp
    else -> 2.dp
}

private fun parsePlayerLyricText(text: String, timestampSeconds: Float?): PlayerLyricLine {
    val parts = BilingualSeparator.split(text, limit = 2)
    val secondary = parts.getOrNull(1)?.trim()?.takeIf(String::isNotEmpty)
    return PlayerLyricLine(primary = parts.first().trim(), secondary = secondary, timestampSeconds = timestampSeconds)
}

