# Tune Player Shell Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the BitChord bottom-sheet player shell, gestures, now-playing layout, full-bleed backdrop, and shell-owned panel state into Tune alongside the current player with zero playback-engine changes.

**Architecture:** New `com.exodidio.tune.ui.navigation` shell files render the current `PlaybackModel`/`PlaybackQueueSnapshot`/`queueTracks: List<LibraryTrack>` data inside a width-gated bottom sheet; old `FullScreenPlayer*.kt` stay mounted until the port is green then are deleted. Pure gesture/panel/backdrop math lives in JVM-testable functions; Compose layers use only Tune theme/font/Haze tokens.

**Tech Stack:** Kotlin, Jetpack Compose, Media3 (read-only), Haze, DataStore, GitHub Actions, gh api

**Spec:** `docs/superpowers/specs/2026-09-26-player-port-design.md` (Sections 3, 4, shell parts of 5)

## Global Constraints

- GitHub-cloud-only: no local builds/tests/emulators; changes via `gh api` git blobs/trees/commits; verify via `gh api --method POST repos/Exodi-dio/tune/actions/workflows/366658414/dispatches -F ref='<branch>'` + `gh run watch` — ci.yml push=[main] only so task-branch pushes never trigger CI and new workflow files do NOT auto-register.
- JUnit `org.junit` only (no `kotlin.test`).
- `androidTest` NOT compiled by CI (review-gated only; quote exact lines in plan/review).
- Old player files (`FullScreenPlayer.kt`, `FullScreenPlayerArtwork.kt`, `FullScreenPlayerMetadata.kt`, `FullScreenPlayerControls.kt`, `MiniPlayer.kt`, `FullScreenPlayerQueuePanel.kt`, `FullScreenPlayerLyricsPanel.kt`) stay until Tasks 1-5 green, then deleted in Task 6.
- Tune theme/font/blur tokens only (`LocalTuneColors`, `liquidGlassBackground`, `DetailHero` 16dp language, `ThemePreferences.reduce_transparency` null-Haze); never BitChord mesh/palette.
- Full-bleed artwork behind Settings toggle default-on, phone-narrow gating only.
- TDD red-green where expressible; `player/PlaybackService.kt` 200ms `delay(200)` ticker is read-only.
- One conventional commit per task (`feat:`/`perf:`/`chore:`) on `impl/v0.3-player/task-N` stacked on `impl/v0.3-player` integration branch, NEVER `main` directly.

## Review Focus

- Fling left with 80px horizontal / 10px vertical at 2.0 px/ms skips to next without dismissing the sheet (Task 2 `shouldDispatchShellSwipe` test).
- Back with lyrics panel open closes panel not player; back with queue open closes queue; back with none dismisses player (Task 5 `nextShellPanelOnBack` + `PlayerBackHandler` order test).
- Full-bleed toggle off on 360dp phone shows Tune blur backdrop not edge-to-edge art; toggle on shows edge-to-edge art (Task 4 `isFullBleedEnabled` test).
- Pause shrinks sleeve to 0.86x with 500ms ease-out, play restores 1.0x; panel open collapses sleeve to thumbnail (Task 3 `shellArtworkScale` + collapse test).
- Reopen on same track restores last-open panel (lyrics/queue/none); track change keeps panel place not reset (Task 5 `restoreShellPanel` test).

---

## File Structure

New files (one-line responsibility each):

- `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellContract.kt` — shell panel enum, width/tablet gating, mount-contract signatures the sibling queue/lyrics plan consumes.
- `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShell.kt` — bottom-sheet container + dismiss strip + max-width/tablet gating rendering current now-playing content.
- `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellGestures.kt` — collapse progress, fling-to-skip, drag-to-panel pure math + threshold constants.
- `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellNowPlaying.kt` — credits+scrubber+transport+volume+bottom-row order, pause-shrink, panel-open collapse.
- `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellBackdrop.kt` — Tune-blur backdrop + full-bleed artwork layer.
- `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellPanelState.kt` — last-open restore, back ordering, prewarm/mount pure logic.
- `androidApp/src/main/kotlin/com/exodidio/tune/settings/PlayerPreferences.kt` — DataStore `full_bleed_artwork` default-on + `last_player_panel`.

Modified files:

- `androidApp/src/main/kotlin/com/exodidio/tune/AppDestinationModels.kt` — add `fullBleedArtwork: Boolean = true`, `onFullBleedArtworkChanged`, `lastPlayerPanel` persistence passthrough to `SettingsDestinationModel`.
- `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/AppDestinationContent.kt` + `MainActivity.kt` — thread `PlaybackModel` into `PlayerShell` (same callbacks as `FullScreenPlayer`).
- `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/SettingsPlaybackContent.kt` (or existing playback settings section) — full-bleed toggle with `fullBleedArtworkAvailable` gating.

Tests:

- `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellContractTest.kt`
- `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellGesturesTest.kt`
- `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellNowPlayingTest.kt`
- `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellBackdropTest.kt`
- `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellPanelStateTest.kt`
- `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/navigation/PlayerShellNavigationTest.kt` — review-gated only, NOT compiled by CI.

---

### Task 1: Shell scaffold

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellContract.kt`
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShell.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellContractTest.kt`
- Review-gated (not CI): `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/navigation/PlayerShellNavigationTest.kt`

**Interfaces:**
- Consumes: `com.exodidio.tune.player.PlaybackState`, `com.exodidio.tune.player.PlaybackQueueSnapshot`, `com.exodidio.tune.sync.LibraryTrack`, `com.exodidio.tune.lyrics.RomanizationUiState`, `com.exodidio.tune.player.ArtworkCrossfadeTransition`, `com.exodidio.tune.player.RepeatMode`, existing `FullScreenPlayerPanel`, `FullScreenPlayerMetadataTransition`, `FullScreenPlayerControls`, `FullScreenQueuePanel`, `FullScreenPlayerLyricsPanel`.
- Produces: `enum class PlayerShellPanel { LYRICS, QUEUE }`; `fun PlayerShellPanel.toLegacy(): FullScreenPlayerPanel`; `fun FullScreenPlayerPanel.toShell(): PlayerShellPanel`; `fun playerFillsWindow(windowWidthDp: Int): Boolean`; `fun tabletSizedPlayer(windowWidthDp: Int): Boolean`; `fun fullBleedArtworkAvailable(windowWidthDp: Int): Boolean`; `typealias OnPlayerShellPanelSelected = (PlayerShellPanel?) -> Unit`; `@Composable fun PlayerShellQueueMount(queue: PlaybackQueueSnapshot, tracks: List<LibraryTrack>, currentTrackId: String, isPlaying: Boolean, onTrackSelected: (String) -> Unit, onTrackRemoved: (String) -> Unit, onReorder: (List<String>) -> Unit, onShuffleChange: (Boolean) -> Unit, onRepeatModeChange: (RepeatMode) -> Unit, modifier: Modifier)`; `@Composable fun PlayerShellLyricsMount(trackId: String, lyrics: String?, loading: Boolean, visible: Boolean, currentPositionMs: Long, onSeek: (Long) -> Unit, modifier: Modifier)`; `@Composable fun PlayerShell(visible: Boolean, onDismiss: () -> Unit, windowWidthDp: Int, selectedPanel: PlayerShellPanel?, onPanelSelected: OnPlayerShellPanelSelected, content: @Composable () -> Unit)` — later tasks and the sibling queue/lyrics plan consume these names/signatures verbatim; no sibling queue-section/autoplay/lyric-line logic here.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellContractTest {
    @Test
    fun `phone width fills window and offers full bleed`() {
        assertTrue(playerFillsWindow(360))
        assertTrue(fullBleedArtworkAvailable(360))
    }
    @Test
    fun `wide phone loses full bleed but tablet regains availability`() {
        assertFalse(playerFillsWindow(800))
        assertTrue(tabletSizedPlayer(800))
        assertTrue(fullBleedArtworkAvailable(800))
    }
    @Test
    fun `mid width is neither phone nor tablet`() {
        assertFalse(playerFillsWindow(650))
        assertFalse(tabletSizedPlayer(650))
        assertFalse(fullBleedArtworkAvailable(650))
    }
    @Test
    fun `panel enum bridges to legacy without loss`() {
        assertTrue(PlayerShellPanel.LYRICS.toLegacy() == FullScreenPlayerPanel.Lyrics)
        assertTrue(PlayerShellPanel.QUEUE.toLegacy() == FullScreenPlayerPanel.Queue)
        assertTrue(FullScreenPlayerPanel.Lyrics.toShell() == PlayerShellPanel.LYRICS)
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish branch `impl/v0.3-player/task-1` via `gh api` blobs/trees/commits, then `gh api --method POST repos/Exodi-dio/tune/actions/workflows/366658414/dispatches -F ref='impl/v0.3-player/task-1'` + `gh run watch` — Expected: FAIL with `Unresolved reference: playerFillsWindow`.
- [ ] **Step 3: Write minimal implementation**
```kotlin
package com.exodidio.tune.ui.navigation

import com.exodidio.tune.player.RepeatMode

// Adapted from BitChord NowPlayingScreen (GPL-3.0): PLAYER_MAX_WIDTH 560dp,
// PLAYER_GUTTER 30dp, TABLET_PLAYER_MIN_WIDTH 700dp, playerFillsWindow /
// tabletSizedPlayer / fullBleedArtworkAvailable. Re-expressed on Int dp for
// JVM tests; Tune blur/theme applied at call sites, never mesh.

internal const val PlayerShellMaxWidthDp = 560
internal const val PlayerShellGutterDp = 30
internal const val PlayerShellTabletMinWidthDp = 700

enum class PlayerShellPanel { LYRICS, QUEUE }

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
```
(Task 1 `PlayerShell` + `PlayerShellQueueMount` + `PlayerShellLyricsMount` + dismiss strip per the contract signatures above; mount functions delegate to the existing `FullScreenQueuePanel` / `FullScreenPlayerLyricsPanel` until the sibling plan replaces their content.)
- [ ] **Step 4: Run test to verify it passes**
Run: publish + `gh api --method POST repos/Exodi-dio/tune/actions/workflows/366658414/dispatches -F ref='impl/v0.3-player/task-1'` + `gh run watch` — Expected: PASS (`:androidApp:testDevDebugUnitTest` green).
- [ ] **Step 5: Commit**
`feat(player): add shell scaffold with width gating and panel-mount contract`

Review-gated navigation check (NOT in CI, quote for reviewer): `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/navigation/PlayerShellNavigationTest.kt` asserts the dismiss strip inside `widthIn(max = 560.dp)`; reviewer runs locally only.

### Task 2: Collapse/gesture system

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellGestures.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellGesturesTest.kt`

**Interfaces:**
- Consumes: `PlayerShellPanel`, existing queue-transport availability helper.
- Produces: `ShellSwipeThresholdDp = 72`, `ShellQueueTravelMs = 420`, `ShellQueueCarryFraction = 0.3f`, `ShellQueueFlickVelocityPxPerSec = 450f`, `ShellDismissStripHeightDp = 32`; `shouldDispatchShellSwipe(...)`, `shellCollapseProgress(...)`, `shouldCarryQueueOpen(...)` — used verbatim by Tasks 3/5 and sibling panels.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellGesturesTest {
    @Test
    fun `horizontal fling past threshold dispatches`() {
        assertTrue(shouldDispatchShellSwipe(80f, 10f, 72f, 2.0f, 1.2f, 28f))
    }
    @Test
    fun `vertical-dominant drag never dispatches`() {
        assertFalse(shouldDispatchShellSwipe(40f, 60f, 72f, 2.0f, 1.2f, 28f))
    }
    @Test
    fun `fast short fling dispatches via velocity minimum`() {
        assertTrue(shouldDispatchShellSwipe(30f, 5f, 72f, 1.5f, 1.2f, 28f))
    }
    @Test
    fun `queue owns collapse while dragging`() {
        assertEquals(0.4f, shellCollapseProgress(true, false, 0.4f, 0.0f, true), 0.0001f)
        assertEquals(1.0f, shellCollapseProgress(false, true, 0.0f, 1.0f, false), 0.0001f)
        assertEquals(0.0f, shellCollapseProgress(false, false, 0.0f, 0.0f, false), 0.0001f)
    }
    @Test
    fun `carry fraction and flick decide queue release`() {
        assertTrue(shouldCarryQueueOpen(0.31f, 0f))
        assertFalse(shouldCarryQueueOpen(0.1f, 0f))
        assertTrue(shouldCarryQueueOpen(0.05f, 500f))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish branch `impl/v0.3-player/task-2` via `gh api`, dispatch + `gh run watch` — Expected: FAIL with `Unresolved reference: shouldDispatchShellSwipe`.
- [ ] **Step 3: Write minimal implementation**
```kotlin
package com.exodidio.tune.ui.navigation

import kotlin.math.abs

// Adapted from BitChord NowPlayingScreen (GPL-3.0): swipeThreshold 72dp,
// QUEUE_TRAVEL_MS 420, QUEUE_CARRY_FRACTION 0.3f, QUEUE_FLICK_VELOCITY 450f,
// DISMISS_STRIP_HEIGHT 32dp; horizontal-dominance rule 1.25x.

internal const val ShellSwipeThresholdDp = 72
internal const val ShellSwipeVelocityMinimumDp = 28
internal const val ShellSwipeVelocityThresholdPxPerMs = 1.2f
internal const val ShellQueueTravelMs = 420
internal const val ShellQueueCarryFraction = 0.3f
internal const val ShellQueueFlickVelocityPxPerSec = 450f
internal const val ShellDismissStripHeightDp = 32

internal fun shouldDispatchShellSwipe(
    horizontalDistancePx: Float,
    verticalDistancePx: Float,
    thresholdPx: Float,
    velocityPxPerMs: Float,
    velocityThresholdPxPerMs: Float,
    velocityMinimumPx: Float,
): Boolean {
    val horizontal = abs(horizontalDistancePx)
    val vertical = abs(verticalDistancePx)
    if (horizontal < vertical * 1.25f) return false
    return horizontal >= thresholdPx ||
        (horizontal >= velocityMinimumPx && abs(velocityPxPerMs) >= velocityThresholdPxPerMs)
}

internal fun shellCollapseProgress(
    queueOpen: Boolean,
    lyricsOpen: Boolean,
    queueSlide: Float,
    animatedCollapse: Float,
    queueDragging: Boolean,
): Float {
    val queueOwnsCollapse = !lyricsOpen &&
        (queueOpen || queueDragging || queueSlide > 0.001f)
    return if (queueOwnsCollapse) queueSlide.coerceIn(0f, 1f) else animatedCollapse.coerceIn(0f, 1f)
}

internal fun shouldCarryQueueOpen(slide: Float, velocityPxPerSec: Float): Boolean =
    slide >= ShellQueueCarryFraction || abs(velocityPxPerSec) >= ShellQueueFlickVelocityPxPerSec
```
- [ ] **Step 4: Run test to verify it passes**
Run: publish + dispatch + `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
`feat(player): add shell collapse and fling-to-skip gesture math`

### Task 3: Now-playing layout port

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellNowPlaying.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellNowPlayingTest.kt`

**Interfaces:**
- Consumes: `PlayerShellPanel`, `OnPlayerShellPanelSelected`, `shellCollapseProgress` (Task 2 verbatim).
- Produces: `shellArtworkScale(isPlaying: Boolean): Float` (1.0 playing, 0.86 paused); `shellNowPlayingOrder(): List<String>` (credits, scrubber, transport, volume, bottomRow); `PlayerShellNowPlaying(...)` keeping every existing callback; queue sections/autoplay and lyric lines excluded.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerShellNowPlayingTest {
    @Test
    fun `now playing order matches reference credits scrubber transport volume bottom row`() {
        assertEquals(
            listOf("credits", "scrubber", "transport", "volume", "bottomRow"),
            shellNowPlayingOrder(),
        )
    }
    @Test
    fun `pause shrinks sleeve and play restores`() {
        assertEquals(0.86f, shellArtworkScale(false), 0.0001f)
        assertEquals(1.0f, shellArtworkScale(true), 0.0001f)
    }
    @Test
    fun `panel open collapses sleeve progress to one`() {
        assertEquals(1.0f, shellCollapseProgress(true, false, 1.0f, 0.0f, false), 0.0001f)
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish branch `impl/v0.3-player/task-3` via `gh api`, dispatch + `gh run watch` — Expected: FAIL with `Unresolved reference: shellNowPlayingOrder`.
- [ ] **Step 3: Write minimal implementation**
Order credits + scrubber + transport + volume + bottom row (lyrics/output/queue toggles); Tune theme tokens and existing string resources for labels; marquee title/artist; seek + volume sliders; transport buttons reuse existing components and callbacks; pause-shrink 0.86f; collapse via Task 2 progress. (Exact composable code per the reference order; no queue-section/autoplay/lyric-line logic.)
- [ ] **Step 4: Run test to verify it passes**
Run: publish + dispatch + `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
`feat(player): port now-playing order with pause-shrink and collapse`

### Task 4: Full-bleed artwork + backdrop + lyrics controls behavior

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellBackdrop.kt`
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/settings/PlayerPreferences.kt`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/AppDestinationModels.kt` (add `fullBleedArtwork: Boolean = true` + `onFullBleedArtworkChanged` next to quality-badge fields)
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellBackdropTest.kt`

**Interfaces:**
- Consumes: `fullBleedArtworkAvailable(windowWidthDp: Int)` (Task 1), existing fullscreen-artwork loader, Tune Haze backdrop tokens.
- Produces: `LyricsControlsIdleMs = 5_000L`; `isFullBleedEnabled(settingOn: Boolean, windowWidthDp: Int): Boolean`; `shouldAutoHideLyricsControls(...): Boolean`; `PlayerShellBackdrop(...)` (Tune blur only, never mesh); `PlayerPreferences` DataStore (`fullBleedArtwork` default true + setter); keepScreenOn + auto-hide wiring for Task 5.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellBackdropTest {
    @Test
    fun `full bleed needs setting on and phone-narrow window`() {
        assertTrue(isFullBleedEnabled(true, 360))
        assertFalse(isFullBleedEnabled(false, 360))
        assertFalse(isFullBleedEnabled(true, 650))
    }
    @Test
    fun `lyrics controls hide only when idle and untouched`() {
        assertTrue(shouldAutoHideLyricsControls(true, true, false, false))
        assertFalse(shouldAutoHideLyricsControls(true, true, true, false))
        assertFalse(shouldAutoHideLyricsControls(true, true, false, true))
        assertFalse(shouldAutoHideLyricsControls(false, true, false, false))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish branch `impl/v0.3-player/task-4` via `gh api`, dispatch + `gh run watch` — Expected: FAIL with `Unresolved reference: isFullBleedEnabled`.
- [ ] **Step 3: Write minimal implementation**
Full-bleed edge-to-edge art layer when enabled else Tune blur backdrop (Haze + dominant-color gradient); DataStore `player` / `full_bleed_artwork` default true; 5s lyrics-controls idle hide; keepScreenOn while lyrics open.
- [ ] **Step 4: Run test to verify it passes**
Run: publish + dispatch + `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
`feat(player): add full-bleed backdrop with default-on toggle and lyrics idle hide`

### Task 5: Panel state

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerShellPanelState.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerShellPanelStateTest.kt`

**Interfaces:**
- Consumes: `PlayerShellPanel`, `OnPlayerShellPanelSelected`, `ShellQueueTravelMs`.
- Produces: `restoreShellPanel(stored: String?): PlayerShellPanel?`; `shellPanelStorageKey(panel: PlayerShellPanel?): String`; `nextShellPanelOnBack(current: PlayerShellPanel?): PlayerShellPanel?`; prewarm stage constants + `prewarmStageFor(...)`; `PlayerShellBackHandlers(...)` (lyrics > queue > dismiss order). Sibling panels consume restore + mount timing.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShellPanelStateTest {
    @Test
    fun `stored panel restores per track place`() {
        assertEquals(PlayerShellPanel.LYRICS, restoreShellPanel("LYRICS"))
        assertEquals(PlayerShellPanel.QUEUE, restoreShellPanel("QUEUE"))
        assertNull(restoreShellPanel("MAIN"))
        assertNull(restoreShellPanel(null))
    }
    @Test
    fun `storage round-trips`() {
        assertEquals("LYRICS", shellPanelStorageKey(PlayerShellPanel.LYRICS))
        assertEquals("QUEUE", shellPanelStorageKey(PlayerShellPanel.QUEUE))
        assertEquals("MAIN", shellPanelStorageKey(null))
    }
    @Test
    fun `back closes panel before player`() {
        assertEquals(null, nextShellPanelOnBack(PlayerShellPanel.LYRICS))
        assertEquals(null, nextShellPanelOnBack(PlayerShellPanel.QUEUE))
        assertEquals(null, nextShellPanelOnBack(null))
    }
    @Test
    fun `prewarm staggers expensive mounts`() {
        assertTrue(prewarmStageFor(null, false, 1))
        assertFalse(prewarmStageFor(PlayerShellPanel.LYRICS, false, 1))
        assertTrue(prewarmStageFor(PlayerShellPanel.LYRICS, false, 2))
        assertTrue(prewarmStageFor(PlayerShellPanel.LYRICS, true, 0))
        assertTrue(prewarmStageFor(PlayerShellPanel.QUEUE, false, 3))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish branch `impl/v0.3-player/task-5` via `gh api`, dispatch + `gh run watch` — Expected: FAIL with `Unresolved reference: restoreShellPanel`.
- [ ] **Step 3: Write minimal implementation**
Restore/store/back-order/prewarm pure logic per reference values (600ms backdrop, 650ms lyrics, 650ms queue); back-handler composable (lyrics > queue > dismiss).
- [ ] **Step 4: Run test to verify it passes**
Run: publish + dispatch + `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
`feat(player): add panel restore back ordering and prewarm timing`

### Task 6: Delete old player + final assemble

**Files:**
- Delete: `FullScreenPlayer.kt`, `FullScreenPlayerArtwork.kt`, `FullScreenPlayerMetadata.kt`, `FullScreenPlayerControls.kt`, `FullScreenPlayerGestures.kt`, `FullScreenPlayerQueuePanel.kt`, `FullScreenPlayerLyricsPanel.kt` (keep `MiniPlayer.kt` unless chrome changed)
- Modify: `NavigationChrome.kt`, `AppDestinationContent.kt` to reference `PlayerShell` + `PlayerShellPanel` exclusively (identical `PlaybackModel` callbacks)
- Test: CI gate only (Tasks 1-5 suites stay green)

**Interfaces:**
- Consumes: all Task 1-5 `PlayerShell*` APIs verbatim; 200ms ticker untouched.
- Produces: green `assembleDevDebug` + release-signed APK; sibling plan owns mount content from here.

- [ ] **Step 1: Write the failing test**
A tree-listing gate: branch tree still lists `FullScreenPlayer.kt` — document the `gh api` tree check as the RED evidence.
- [ ] **Step 2: Run test to verify it fails**
Dispatch ci on branch `impl/v0.3-player/task-6` — tree check shows old files present (RED state recorded).
- [ ] **Step 3: Write minimal implementation**
Deletions via `gh api` git tree + rewiring to `PlayerShell` with identical callbacks.
- [ ] **Step 4: Run test to verify it passes**
Dispatch + `gh run watch` — Expected: PASS (unit + assemble + branding green). Then release-signed APK flow, tag proposed `v0.3.0-beta.1`.
- [ ] **Step 5: Commit**
`chore(player): remove legacy fullscreen player after shell port green`

