package com.exodidio.tune.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.lyrics.RomanizationUiState
import com.exodidio.tune.ui.theme.LocalTuneColors

const val LyricsScrollLeadMinMs: Long = 350L
const val LyricsScrollLeadMaxMs: Long = 500L

fun activeLyricIndex(lines: List<PlayerLyricLine>, positionMs: Long): Int =
    lines.indexOfLast { (it.timestampSeconds ?: Float.MAX_VALUE) <= positionMs / 1_000f }

fun lyricsScrollLeadMs(gapMs: Long): Long =
    gapMs.coerceIn(LyricsScrollLeadMinMs, LyricsScrollLeadMaxMs)

fun applyLyricsOffset(positionMs: Long, offsetMs: Int): Long =
    (positionMs - offsetMs.toLong()).coerceAtLeast(0L)

fun lyricsSeekTargetMs(timestampSeconds: Float, offsetMs: Int): Long =
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
    val secondary = if (romanizationAllowed && romanization.input == primary && romanization.enabled) romanization.secondary else emptyList()
    val displayed = displayedLyricsPositionMs(currentPositionMs, pendingSeekPositionMs)
    val adjusted = applyLyricsOffset(displayed, lyricsOffsetMs)
    val activeIndex = remember(synced, adjusted) { activeLyricIndex(synced, adjusted) }
    val listState = rememberLazyListState()
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
    LaunchedEffect(synced, activeIndex, isBrowsing) {
        if (activeIndex < 0 || isBrowsing) return@LaunchedEffect
        val gapMs = if (activeIndex + 1 < synced.size) {
            ((synced[activeIndex + 1].timestampSeconds!! - synced[activeIndex].timestampSeconds!!) * 1_000).toLong()
        } else LyricsScrollLeadMinMs
        lyricsScrollLeadMs(gapMs)
        listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
    }
    when {
        loading -> Text(stringResource(R.string.player_lyrics), modifier = modifier.padding(top = 24.dp))
        lyrics.isNullOrBlank() -> Text(stringResource(R.string.player_lyrics_not_available), modifier = modifier.padding(top = 24.dp))
        synced.isNotEmpty() -> LazyColumn(state = listState, modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 72.dp)) {
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
        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
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
}
