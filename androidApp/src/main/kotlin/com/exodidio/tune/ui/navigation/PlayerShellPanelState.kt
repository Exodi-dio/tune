package com.exodidio.tune.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

// Shell panel state: back ordering for the sibling panels.
//
// Scope cut (v0.3 pre-merge, F5): last-open panel restore via DataStore
// `last_player_panel` (restoreShellPanel/shellPanelStorageKey) + prewarm
// staging (prewarmStageFor) are cut — panels open on demand and mounts land
// immediately. Wiring a global DataStore key would not satisfy the spec's
// per-track restore, and staged delays do not fit this small wave. Helpers
// deleted; back ordering kept.

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

/**
 * Back closes the open panel before the player itself: the shell collapses
 * to now-playing first, dismissal happens only once no panel is selected.
 */
internal fun nextShellPanelOnBack(current: PlayerShellPanel?): PlayerShellPanel? = null

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

