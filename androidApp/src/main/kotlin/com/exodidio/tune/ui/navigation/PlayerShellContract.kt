package com.exodidio.tune.ui.navigation

// Adapted from BitChord NowPlayingScreen (GPL-3.0): PLAYER_MAX_WIDTH 560dp,
// PLAYER_GUTTER 30dp, TABLET_PLAYER_MIN_WIDTH 700dp, playerFillsWindow /
// tabletSizedPlayer / fullBleedArtworkAvailable. Re-expressed on Int dp for
// JVM tests; Tune blur/theme applied at call sites, never mesh.

internal const val PlayerShellMaxWidthDp = 560
internal const val PlayerShellGutterDp = 30
internal const val PlayerShellTabletMinWidthDp = 700

enum class PlayerShellPanel { LYRICS, QUEUE }

// Relocated verbatim from the deleted FullScreenPlayerControls.kt: the shell
// contract keeps bridging to the legacy panel names.
internal enum class FullScreenPlayerPanel { Lyrics, Queue }

typealias OnPlayerShellPanelSelected = (PlayerShellPanel?) -> Unit

internal fun PlayerShellPanel.toLegacy(): FullScreenPlayerPanel = when (this) {
    PlayerShellPanel.LYRICS -> FullScreenPlayerPanel.Lyrics
    PlayerShellPanel.QUEUE -> FullScreenPlayerPanel.Queue
}

internal fun FullScreenPlayerPanel.toShell(): PlayerShellPanel = when (this) {
    FullScreenPlayerPanel.Lyrics -> PlayerShellPanel.LYRICS
    FullScreenPlayerPanel.Queue -> PlayerShellPanel.QUEUE
}

internal fun playerFillsWindow(windowWidthDp: Int): Boolean =
    windowWidthDp <= PlayerShellMaxWidthDp + PlayerShellGutterDp * 2

internal fun tabletSizedPlayer(windowWidthDp: Int): Boolean =
    windowWidthDp >= PlayerShellTabletMinWidthDp

internal fun fullBleedArtworkAvailable(windowWidthDp: Int): Boolean =
    playerFillsWindow(windowWidthDp) || tabletSizedPlayer(windowWidthDp)
