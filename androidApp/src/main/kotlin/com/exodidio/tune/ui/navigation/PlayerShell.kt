package com.exodidio.tune.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.lyrics.RomanizationUiState
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.theme.LocalTuneColors
import dev.chrisbanes.haze.HazeState

internal const val PlayerShellDismissStripTestTag = "player_shell_dismiss_strip"
internal const val PlayerShellContentTestTag = "player_shell_content"

private val PlayerShellDragHandleShape = RoundedCornerShape(2.dp)
private const val PlayerShellDragHandleTestTag = "player_shell_drag_handle"

// Relocated verbatim from the deleted FullScreenPlayerGestures.kt: the shell
// dismiss strip keeps the same handle pill.
@Composable
internal fun PlayerShellDragHandle() {
    val colors = LocalTuneColors.current
    Box(Modifier.fillMaxWidth().height(4.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.width(48.dp).height(4.dp).semantics { testTag = PlayerShellDragHandleTestTag }
                .clip(PlayerShellDragHandleShape).background(colors.foregroundSubtle),
        )
    }
}

/**
 * Final queue mount: renders the sectioned queue inside the shell. Backed by
 * PlayerQueueSectionPanel (Tasks 1/3/4); shuffle/repeat callbacks stay in the
 * contract for stability while the section panel owns no toggles.
 */
@Composable
fun PlayerShellQueueMount(
    queue: PlaybackQueueSnapshot,
    tracks: List<LibraryTrack>,
    currentTrackId: String,
    isPlaying: Boolean,
    onTrackSelected: (String) -> Unit,
    onTrackRemoved: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onRepeatModeChange: (RepeatMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerQueueSectionPanel(
        queue = queue,
        tracks = tracks,
        autoplayEnabled = true,
        currentTrackId = currentTrackId,
        onTrackSelected = onTrackSelected,
        onTrackRemoved = onTrackRemoved,
        onReorder = onReorder,
        onClearNext = { cleared -> onReorder(cleared) },
        modifier = modifier,
    )
}

/**
 * Final lyrics mount: renders the current track lyrics inside the shell.
 * Backed by PlayerLyricsPanel (Tasks 3/4) with the live romanization state;
 * the host composes this only while the lyrics panel is selected.
 */
@Composable
internal fun PlayerShellLyricsMount(
    trackId: String,
    lyrics: String?,
    loading: Boolean,
    visible: Boolean,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    pendingSeekPositionMs: Long? = null,
    seekRequestId: Long = 0L,
    romanization: RomanizationUiState = RomanizationUiState(),
    romanizationAllowed: Boolean = false,
    onRomanizationInput: (List<String>, Boolean) -> Unit = { _, _ -> },
    onRomanizationToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    PlayerLyricsPanel(
        trackId = trackId,
        lyrics = lyrics,
        loading = loading,
        currentPositionMs = currentPositionMs,
        pendingSeekPositionMs = pendingSeekPositionMs,
        seekRequestId = seekRequestId,
        romanization = romanization,
        romanizationAllowed = romanizationAllowed,
        onRomanizationInput = onRomanizationInput,
        onRomanizationToggle = onRomanizationToggle,
        onSeek = onSeek,
        modifier = modifier,
    )
}

/**
 * Shell scaffold: bottom-sheet container with a dismiss strip and the
 * width-gated sheet (phones fill the window, wider layouts centre a capped
 * sheet). Now-playing content arrives via [content]; [selectedPanel] and
 * [onPanelSelected] drive the panel switcher. The Tune blur backdrop and the
 * lyrics > queue > dismiss back ordering mount here (W2); full-bleed art is
 * precomputed by the host via [isFullBleedEnabled].
 */
@Composable
fun PlayerShell(
    visible: Boolean,
    onDismiss: () -> Unit,
    windowWidthDp: Int,
    selectedPanel: PlayerShellPanel?,
    onPanelSelected: OnPlayerShellPanelSelected,
    content: @Composable () -> Unit,
    artworkPath: String? = null,
    fullBleed: Boolean = false,
    keepScreenOn: Boolean = false,
    hazeState: HazeState? = null,
) {
    if (!visible) return
    PlayerShellBackHandlers(
        selectedPanel = selectedPanel,
        onPanelSelected = onPanelSelected,
        onDismiss = onDismiss,
    )
    val colors = LocalTuneColors.current
    val dismissDescription = stringResource(R.string.bottom_sheet_dismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.playerBackdrop.copy(alpha = 0.72f)),
    ) {
        PlayerShellBackdrop(
            artworkPath = artworkPath,
            fullBleed = fullBleed,
            modifier = Modifier.fillMaxSize(),
            hazeState = hazeState,
            keepScreenOn = keepScreenOn,
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = PlayerShellMaxWidthDp.dp)
                .fillMaxWidth()
                .background(colors.playerBackdrop)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .semantics { testTag = PlayerShellContentTestTag },
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .semantics {
                        testTag = PlayerShellDismissStripTestTag
                        contentDescription = dismissDescription
                    }
                    .clickable(
                        onClick = onDismiss,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PlayerShellDragHandle()
            }
            content()
        }
    }
}
