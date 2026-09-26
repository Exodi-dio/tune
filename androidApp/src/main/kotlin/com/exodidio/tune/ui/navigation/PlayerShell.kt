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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.player.PlaybackQueueSnapshot
import com.exodidio.tune.player.RepeatMode
import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.ui.theme.LocalTuneColors

internal const val PlayerShellDismissStripTestTag = "player_shell_dismiss_strip"
internal const val PlayerShellContentTestTag = "player_shell_content"

/**
 * Task 1 queue mount: renders the current queue inside the shell. Delegates to
 * the existing FullScreenQueuePanel until the sibling queue plan replaces the
 * section content; the signature below is the contract the sibling consumes.
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
    FullScreenQueuePanel(
        queue = queue,
        tracks = tracks,
        currentTrackId = currentTrackId,
        isPlaying = isPlaying,
        onTrackSelected = onTrackSelected,
        onTrackRemoved = onTrackRemoved,
        onReorder = onReorder,
        onShuffleChange = onShuffleChange,
        onRepeatModeChange = onRepeatModeChange,
        modifier = modifier,
    )
}

/**
 * Task 1 lyrics mount: renders the current track lyrics inside the shell.
 * Delegates to the existing FullScreenPlayerLyricsPanel until the sibling
 * lyrics plan replaces the line content; the signature below is the contract
 * the sibling consumes.
 */
@Composable
fun PlayerShellLyricsMount(
    trackId: String,
    lyrics: String?,
    loading: Boolean,
    visible: Boolean,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    pendingSeekPositionMs: Long? = null,
    seekRequestId: Long = 0L,
    modifier: Modifier = Modifier,
) {
    FullScreenPlayerLyricsPanel(
        trackId = trackId,
        lyrics = lyrics,
        loading = loading,
        visible = visible,
        currentPositionMs = currentPositionMs,
        pendingSeekPositionMs = pendingSeekPositionMs,
        seekRequestId = seekRequestId,
        onSeek = onSeek,
        modifier = modifier,
    )
}

/**
 * Task 1 shell scaffold: bottom-sheet container with a dismiss strip and the
 * width-gated sheet (phones fill the window, wider layouts centre a capped
 * sheet). Now-playing content arrives via [content]; [selectedPanel] and
 * [onPanelSelected] are accepted for contract stability and get wired to the
 * panel switcher by later tasks. Backdrop and full-bleed artwork layering are
 * owned by Task 4, gestures by Task 2.
 */
@Composable
fun PlayerShell(
    visible: Boolean,
    onDismiss: () -> Unit,
    windowWidthDp: Int,
    selectedPanel: PlayerShellPanel?,
    onPanelSelected: OnPlayerShellPanelSelected,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    val colors = LocalTuneColors.current
    val dismissDescription = stringResource(R.string.bottom_sheet_dismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.playerBackdrop.copy(alpha = 0.72f)),
    ) {
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
                FullScreenPlayerDragHandle()
            }
            content()
        }
    }
}
