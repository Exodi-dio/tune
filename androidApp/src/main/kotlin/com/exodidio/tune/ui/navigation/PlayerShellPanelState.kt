package com.exodidio.tune.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

// Shell panel state: restore/store round-trip, back ordering, and prewarm
// staging for the sibling panels. Restored panels mount immediately;
// otherwise the backdrop warms first, then lyrics, then the queue, so the
// expensive mounts never land on the same frame.

// Prewarm reference timing: backdrop first, lyrics and queue staggered after.
internal const val ShellBackdropPrewarmMs = 600
internal const val ShellLyricsPrewarmMs = 650
internal const val ShellQueuePrewarmMs = 650

internal const val ShellPrewarmBackdropStage = 1
internal const val ShellPrewarmLyricsStage = 2
internal const val ShellPrewarmQueueStage = 3

// The sibling queue mounts once its travel animation can carry it; distinct
// from the prewarm stagger above.
internal const val ShellQueueMountDelayMs = ShellQueueTravelMs

internal fun restoreShellPanel(stored: String?): PlayerShellPanel? = when (stored) {
    PlayerShellPanel.LYRICS.name -> PlayerShellPanel.LYRICS
    PlayerShellPanel.QUEUE.name -> PlayerShellPanel.QUEUE
    else -> null
}

internal fun shellPanelStorageKey(panel: PlayerShellPanel?): String =
    panel?.name ?: "MAIN"

/**
 * Back closes the open panel before the player itself: the shell collapses
 * to now-playing first, dismissal happens only once no panel is selected.
 */
internal fun nextShellPanelOnBack(current: PlayerShellPanel?): PlayerShellPanel? = null

internal fun prewarmStageFor(panel: PlayerShellPanel?, restored: Boolean, stage: Int): Boolean {
    if (restored) return true
    val required = when (panel) {
        null -> ShellPrewarmBackdropStage
        PlayerShellPanel.LYRICS -> ShellPrewarmLyricsStage
        PlayerShellPanel.QUEUE -> ShellPrewarmQueueStage
    }
    return stage >= required
}

/**
 * Shell back handling in lyrics > queue > dismiss order. Only one handler
 * is enabled at a time: an open panel returns to now-playing, and the player
 * dismisses only once no panel is selected.
 */
@Composable
fun PlayerShellBackHandlers(
    selectedPanel: PlayerShellPanel?,
    onPanelSelected: OnPlayerShellPanelSelected,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = selectedPanel == PlayerShellPanel.LYRICS) {
        onPanelSelected(null)
    }
    BackHandler(enabled = selectedPanel == PlayerShellPanel.QUEUE) {
        onPanelSelected(null)
    }
    BackHandler(enabled = selectedPanel == null) {
        onDismiss()
    }
}
