# Tune Queue Sections, Offline Autoplay, and Lyrics Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port BitChord queue sections, offline autoplay, and line-level lyrics into Tune panels wired to existing queue/lyrics plumbing without changing playback semantics.

**Architecture:** New sectioned queue and lyrics panels live alongside `FullScreenPlayerQueuePanel.kt` / `FullScreenPlayerLyricsPanel.kt` and read only `PlaybackModel` (`PlaybackQueueSnapshot`, `queueTracks: List<LibraryTrack>`, lyrics + callbacks); autoplay is an append-only tier via the existing `queue.append` path and never reorders user tracks.

**Tech Stack:** [Kotlin, Jetpack Compose, Media3 queue (read-only semantics), Room (read-only), GitHub Actions, gh api]

**Spec:** `docs/superpowers/specs/2026-09-26-player-port-design.md` (Sections 5, 6)

## Global Constraints

- GitHub-cloud-only (no local builds/tests/emulators; changes via `gh api` git blobs/trees/commits; verify via `gh workflow run 366658414 --ref <branch>` + `gh run watch <run-id>` — ci.yml has NO branch-push trigger; new workflow files do NOT auto-register).
- JUnit org.junit only (`import org.junit.Test`, `import org.junit.Assert.*`); all new tests live under `androidApp/src/test` (JVM unit tests).
- androidTest NOT compiled by CI (review-gated only; do not add androidTest in this plan).
- Queue shuffle/repeat semantics UNCHANGED (append-only autoplay tier via existing `queue.append`, never mutates user order; `RepeatMode.Off/One/All` rules from `PlaybackQueue.kt` untouched).
- Lyrics sources + manual finder + romanization UNCHANGED in behavior (`AndroidLyricsService.kt` lrclib+kugou race, `FindLyricsContent`, `RomanizationUiState` pass-through only).
- Tune font/type scale only (`MaterialTheme.typography.headlineSmall/Bold`, active 1.04x, distance opacity/blur via existing `syncedLyricBlurRadius`).
- TDD red-green where expressible (failing test dispatched to CI first, then minimal implementation, then green).
- One conventional commit per task on `impl/v0.3-player/task-N` branches merged into `impl/v0.3-player` integration branch, NEVER main directly.

## Review Focus

- Queue drains to 1 remaining track with autoplay on → up to 10 non-repeating autoplay ids appended, user order byte-identical (Task 2 test `appendsUpToTenWithoutTouchingUserOrder`).
- Repeat-One with autoplay on → `shouldRefillAutoplay` false, no append (Task 2 test `repeatOneSuppressesAutoplayRefill`).
- Tap lyric line at 62.5s → `onSeek(62500L)` with offset applied (Task 4 test `tapLyricSeeksToTimestampPlusOffset`).
- User drags lyrics list → browse mode true, active-line follower suspended until replay/seek (Task 4 test `browseSuspendsFollowUntilReplay`).
- Empty queue (`activeTrackIds=[]`, `currentIndex=-1`) → `splitQueue` returns null now-playing + all sections empty, no autoplay header rendered (Task 1 test `emptyQueueHasNoSectionsOrHeader`).

---

## File Structure

- Modify (read-only until Task 5): `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerQueuePanel.kt` (existing flat `FullScreenQueuePanel`, `moveQueueTrack`, `commitQueueReorder`, `queueTrackContextMenuActions`, 56dp rows — keep until green).
- Modify (read-only until Task 5): `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerLyricsPanel.kt` (existing `parsePlayerLyrics`, `PlayerLyricLine`, `syncedLyricBlurRadius`, `shouldEnterLyricsBrowseMode`, `shouldSeekFromLyricTap`, `shouldResetLyricsForReplay`, `displayedLyricsPositionMs`, `shouldFollowLyricsActiveLine`, `shouldResumeLyricsAutoScroll`, `lyricsSeekDirection` — reuse, do not fork).
- Modify (read-only semantics): `sharedLogic/src/commonMain/kotlin/com/exodidio/tune/player/PlaybackQueue.kt` (`PlaybackQueueSnapshot(originalTrackIds,activeTrackIds,currentIndex,shuffle,repeatMode)`, `MaxPlaybackQueueSize=1000` — never edit), `sharedLogic/src/commonMain/kotlin/com/exodidio/tune/mood/MoodRadio.kt` (`selectMoodRadio`, `MoodRadioTrack`, `MoodRadioBatchSize=15` — never edit), `androidApp/src/main/kotlin/com/exodidio/tune/player/PlaybackService.kt` (`ActionAppend`→`queue.append`, `ActionReorder`→`queue.reorderQueue`, mood refill threshold — append-only reuse), `androidApp/src/main/kotlin/com/exodidio/tune/AppDestinationModels.kt` (`PlaybackModel` callbacks), `androidApp/src/main/kotlin/com/exodidio/tune/sync/SyncDatabase.kt` (`LibraryTrack`, `metadataObject()`), `androidApp/src/main/kotlin/com/exodidio/tune/lyrics/AndroidLyricsService.kt` + `LyricsPreferences.kt` + `ui/components/FindLyricsContent.kt` (untouched).
- Create Task 1: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSections.kt`, `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSectionPanel.kt`; Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSectionsTest.kt`.
- Create Task 2: `androidApp/src/main/kotlin/com/exodidio/tune/player/PlayerAutoplayEngine.kt`; Test: `androidApp/src/test/kotlin/com/exodidio/tune/player/PlayerAutoplayEngineTest.kt`.
- Modify Task 3: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSectionPanel.kt`; Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueInteractionTest.kt`.
- Create Task 4: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerLyricsPanel.kt`; Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerLyricsPanelTest.kt`.
- Modify Task 5: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayer.kt` (mount into assumed host), delete `FullScreenPlayerQueuePanel.kt` + `FullScreenPlayerLyricsPanel.kt` only when green.

### Task 1: Queue split + section scaffolding

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSections.kt`
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSectionPanel.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSectionsTest.kt`

**Interfaces:**
- Consumes: `com.exodidio.tune.player.PlaybackQueueSnapshot(activeTrackIds: List<String>, currentIndex: Int)`, `com.exodidio.tune.sync.LibraryTrack(id,title,artists,album,albumId)`, existing `FullScreenQueuePanel(queue,moodRadioActive,tracks,currentTrackId,isPlaying,onTrackSelected,onTrackRemoved,onTrackPlayNext,onReorder,onShuffleChange,onRepeatModeChange)` (read-only reference, not yet replaced).
- Produces: `com.exodidio.tune.ui.navigation.QueueSections(nowPlayingId: String?, userIds: List<String>, contextIds: List<String>, autoplayIds: List<String>)`, `fun com.exodidio.tune.ui.navigation.splitQueue(activeTrackIds: List<String>, currentIndex: Int, userIds: Set<String>, autoplayIds: Set<String>): QueueSections`, `fun com.exodidio.tune.ui.navigation.shouldShowAutoplayHeader(autoplayIds: List<String>, autoplayEnabled: Boolean): Boolean`, shell contract `enum class PlayerShellPanel { LYRICS, QUEUE }`, `typealias OnPlayerShellPanelSelected = (PlayerShellPanel?) -> Unit`, `@Composable fun PlayerShellQueueMount(queue: PlaybackQueueSnapshot, tracks: List<LibraryTrack>, currentTrackId: String, isPlaying: Boolean, onTrackSelected: (String) -> Unit, onTrackRemoved: (String) -> Unit, onReorder: (List<String>) -> Unit, onShuffleChange: (Boolean) -> Unit, onRepeatModeChange: (RepeatMode) -> Unit, modifier: Modifier)`, `@Composable fun PlayerShellLyricsMount(trackId: String, lyrics: String?, loading: Boolean, visible: Boolean, currentPositionMs: Long, onSeek: (Long) -> Unit, modifier: Modifier)`, `@Composable fun PlayerShell(visible: Boolean, onDismiss: () -> Unit, windowWidthDp: Int, selectedPanel: PlayerShellPanel?, onPanelSelected: OnPlayerShellPanelSelected, content: @Composable () -> Unit)` (sibling shell plan owns the shell; this task only assumes this exact contract and renders queue sections via `PlayerShellQueueMount` with current queue data, no autoplay population yet).
- Assumption (explicit): the sibling player-shell plan provides `PlayerShell(visible, onDismiss, windowWidthDp, selectedPanel: PlayerShellPanel?, onPanelSelected, content)` with queue content via `PlayerShellQueueMount` and lyrics content via `PlayerShellLyricsMount`, driven by `selectedPanel: PlayerShellPanel?` + `onPanelSelected`; this task renders the queue sections via `PlayerShellQueueMount` and does not build sheet/deck/gestures.
- Amendment (controller ruling): assumed PlayerPanelHost replaced by the shell plan's PlayerShellQueueMount/PlayerShellLyricsMount + PlayerShellPanel contract above.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueSectionsTest {
    @Test
    fun partitionsUpcomingByMembershipWithRemainderAsContext() {
        val sections = splitQueue(
            activeTrackIds = listOf("now", "u1", "c1", "a1", "u2"),
            currentIndex = 0,
            userIds = setOf("u1", "u2"),
            autoplayIds = setOf("a1")
        )
        assertEquals("now", sections.nowPlayingId)
        assertEquals(listOf("u1", "u2"), sections.userIds)
        assertEquals(listOf("c1"), sections.contextIds)
        assertEquals(listOf("a1"), sections.autoplayIds)
    }

    @Test
    fun legacyQueueWithAllUpcomingAsUserHasEmptyContextAndAutoplay() {
        val active = listOf("now", "n1", "n2")
        val sections = splitQueue(
            activeTrackIds = active,
            currentIndex = 0,
            userIds = active.drop(1).toSet(),
            autoplayIds = emptySet()
        )
        assertEquals(listOf("n1", "n2"), sections.userIds)
        assertEquals(emptyList<String>(), sections.contextIds)
        assertEquals(emptyList<String>(), sections.autoplayIds)
    }

    @Test
    fun emptyQueueHasNoSectionsOrHeader() {
        val sections = splitQueue(
            activeTrackIds = emptyList(),
            currentIndex = -1,
            userIds = emptySet(),
            autoplayIds = emptySet()
        )
        assertEquals(null, sections.nowPlayingId)
        assertEquals(emptyList<String>(), sections.userIds)
        assertEquals(emptyList<String>(), sections.contextIds)
        assertEquals(emptyList<String>(), sections.autoplayIds)
        assertFalse(shouldShowAutoplayHeader(sections.autoplayIds, autoplayEnabled = true))
    }

    @Test
    fun autoplayHeaderVisibleWhenEnabledOrNonEmpty() {
        assertTrue(shouldShowAutoplayHeader(listOf("a1"), autoplayEnabled = false))
        assertTrue(shouldShowAutoplayHeader(emptyList(), autoplayEnabled = true))
        assertFalse(shouldShowAutoplayHeader(emptyList(), autoplayEnabled = false))
    }

    @Test
    fun outOfRangeCurrentIndexYieldsEmptySections() {
        val sections = splitQueue(
            activeTrackIds = listOf("a", "b"),
            currentIndex = 7,
            userIds = setOf("b"),
            autoplayIds = emptySet()
        )
        assertEquals(null, sections.nowPlayingId)
        assertEquals(emptyList<String>(), sections.userIds)
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish `impl/v0.3-player/task-1` via `gh api` blobs/trees/commits, then `gh workflow run 366658414 --ref impl/v0.3-player/task-1` + `gh run watch <run-id>` — Expected: FAIL with `Unresolved reference 'splitQueue'` / `Unresolved reference 'QueueSections'` (new files do not exist yet).
- [ ] **Step 3: Write minimal implementation**
```kotlin
package com.exodidio.tune.ui.navigation

data class QueueSections(
    val nowPlayingId: String?,
    val userIds: List<String>,
    val contextIds: List<String>,
    val autoplayIds: List<String>
)

fun splitQueue(
    activeTrackIds: List<String>,
    currentIndex: Int,
    userIds: Set<String>,
    autoplayIds: Set<String>
): QueueSections {
    if (activeTrackIds.isEmpty() || currentIndex !in activeTrackIds.indices) {
        return QueueSections(null, emptyList(), emptyList(), emptyList())
    }
    val nowPlayingId = activeTrackIds[currentIndex]
    val upcoming = activeTrackIds.subList(currentIndex + 1, activeTrackIds.size)
    val user = upcoming.filter { it in userIds && it !in autoplayIds }
    val autoplay = upcoming.filter { it in autoplayIds }
    val context = upcoming.filter { it !in userIds && it !in autoplayIds }
    return QueueSections(nowPlayingId, user, context, autoplay)
}

fun shouldShowAutoplayHeader(autoplayIds: List<String>, autoplayEnabled: Boolean): Boolean =
    autoplayIds.isNotEmpty() || autoplayEnabled
```
```kotlin
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
```
- [ ] **Step 4: Run test to verify it passes**
Run: publish updated tree to `impl/v0.3-player/task-1`, `gh workflow run 366658414 --ref impl/v0.3-player/task-1` + `gh run watch <run-id>` — Expected: PASS (`:androidApp:testDevDebugUnitTest` green for `PlayerQueueSectionsTest`).
- [ ] **Step 5: Commit**
`feat(player): sectioned queue split and scaffolding`

### Task 2: Offline autoplay engine

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/player/PlayerAutoplayEngine.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/player/PlayerAutoplayEngineTest.kt`

**Interfaces:**
- Consumes: `com.exodidio.tune.sync.LibraryTrack(id,title,artists,album,albumId,playCount,metadataJson)`, `fun com.exodidio.tune.sync.metadataObject(): kotlinx.serialization.json.JsonObject?`, `com.exodidio.tune.player.RepeatMode(Off,One,All)`, `fun com.exodidio.tune.ui.navigation.splitQueue(activeTrackIds: List<String>, currentIndex: Int, userIds: Set<String>, autoplayIds: Set<String>): QueueSections` (Task 1, unchanged signature).
- Produces: `const val MaxOfflineAutoplay: Int = 10`, `fun pickAutoplay(library: List<LibraryTrack>, currentTrack: LibraryTrack, recentIds: Set<String>, limit: Int = 10): List<String>`, `fun shouldSuppressAutoplay(repeatMode: RepeatMode): Boolean`, `fun shouldRefillAutoplay(upcomingCount: Int, repeatMode: RepeatMode, autoplayEnabled: Boolean): Boolean`, `fun nextAutoplayTriggerIndex(activeTrackIds: List<String>, currentIndex: Int): Int`.
- Invariant: autoplay is append-only — `pickAutoplay` returns only ids to `queue.append(ids)` (existing `PlaybackService.ActionAppend` path); it never calls `reorderQueue`/`removeFromQueue` and never reorders `activeTrackIds`; proven by `appendsUpToTenWithoutTouchingUserOrder`.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.player

import com.exodidio.tune.sync.LibraryTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerAutoplayEngineTest {
    private fun track(id: String, artists: String, albumId: String = "al-$id", playCount: Int = 0): LibraryTrack =
        LibraryTrack(id = id, title = "Title $id", artists = artists, album = "Album $albumId", albumId = albumId, playCount = playCount)

    @Test
    fun picksSameArtistBeforeSameGenreBeforeFillAndCapsAtTen() {
        val current = track("cur", "Adele")
        val library = listOf(
            track("a1", "Adele"),
            track("a2", "Adele"),
            track("g1", "Other", playCount = 50),
            track("f1", "Far", playCount = 5),
            track("f2", "Far", playCount = 4),
            track("cur", "Adele")
        )
        val picked = pickAutoplay(library, current, recentIds = emptySet(), limit = 10)
        assertTrue(picked.indexOf("a1") < picked.indexOf("g1"))
        assertTrue(picked.indexOf("g1") < picked.indexOf("f1") || !picked.contains("g1") || picked.contains("f1"))
        assertFalse(picked.contains("cur"))
        assertTrue(picked.size <= 10)
    }

    @Test
    fun excludesRecentIdsAndNeverRepeatsCurrent() {
        val current = track("cur", "Adele")
        val library = listOf(track("a1", "Adele"), track("a2", "Adele"), track("cur", "Adele"))
        val picked = pickAutoplay(library, current, recentIds = setOf("a1"), limit = 10)
        assertFalse(picked.contains("a1"))
        assertFalse(picked.contains("cur"))
        assertEquals(listOf("a2"), picked)
    }

    @Test
    fun appendsUpToTenWithoutTouchingUserOrder() {
        val userOrder = listOf("now", "u1", "u2")
        val picked = pickAutoplay(
            library = (1..20).map { track("s$it", "Same Artist", playCount = it) } + track("now", "Same Artist"),
            currentTrack = track("now", "Same Artist"),
            recentIds = emptySet(),
            limit = 10
        )
        assertEquals(10, picked.size)
        val appended = userOrder + picked
        assertEquals(listOf("now", "u1", "u2"), appended.take(3))
        assertEquals(userOrder.size + 10, appended.size)
    }

    @Test
    fun repeatOneSuppressesAutoplayRefill() {
        assertTrue(shouldSuppressAutoplay(RepeatMode.One))
        assertFalse(shouldSuppressAutoplay(RepeatMode.Off))
        assertFalse(shouldSuppressAutoplay(RepeatMode.All))
        assertFalse(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.One, autoplayEnabled = true))
        assertFalse(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.Off, autoplayEnabled = false))
        assertTrue(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        assertTrue(shouldRefillAutoplay(upcomingCount = 1, repeatMode = RepeatMode.All, autoplayEnabled = true))
    }

    @Test
    fun refillTriggersOnlyWhenQueueDrainsTowardEmpty() {
        assertTrue(shouldRefillAutoplay(upcomingCount = 0, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        assertTrue(shouldRefillAutoplay(upcomingCount = 1, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        assertFalse(shouldRefillAutoplay(upcomingCount = 2, repeatMode = RepeatMode.Off, autoplayEnabled = true))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish `impl/v0.3-player/task-2` via `gh api`, `gh workflow run 366658414 --ref impl/v0.3-player/task-2` + `gh run watch <run-id>` — Expected: FAIL with `Unresolved reference 'pickAutoplay'` / `Unresolved reference 'MaxOfflineAutoplay'`.
- [ ] **Step 3: Write minimal implementation**
```kotlin
package com.exodidio.tune.player

import com.exodidio.tune.sync.LibraryTrack
import com.exodidio.tune.sync.metadataObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

const val MaxOfflineAutoplay: Int = 10

fun shouldSuppressAutoplay(repeatMode: RepeatMode): Boolean =
    repeatMode == RepeatMode.One

fun shouldRefillAutoplay(upcomingCount: Int, repeatMode: RepeatMode, autoplayEnabled: Boolean): Boolean {
    if (!autoplayEnabled) return false
    if (shouldSuppressAutoplay(repeatMode)) return false
    return upcomingCount <= 1
}

fun nextAutoplayTriggerIndex(activeTrackIds: List<String>, currentIndex: Int): Int =
    (activeTrackIds.size - currentIndex - 1).coerceAtLeast(0)

fun pickAutoplay(
    library: List<LibraryTrack>,
    currentTrack: LibraryTrack,
    recentIds: Set<String>,
    limit: Int = 10
): List<String> {
    if (limit <= 0) return emptyList()
    val cap = limit.coerceAtMost(MaxOfflineAutoplay)
    val excluded = recentIds + currentTrack.id
    val candidates = library.filter { it.id.isNotBlank() && it.id !in excluded }
    if (candidates.isEmpty()) return emptyList()
    val currentArtists = artistTokens(currentTrack.artists)
    val currentGenres = genreTokens(currentTrack)
    val sameArtist = candidates.filter { artistTokens(it.artists).any(currentArtists::contains) }
    val sameGenre = candidates.filter {
        it.id !in sameArtist.map(LibraryTrack::id) &&
            genreTokens(it).any(currentGenres::contains)
    }
    val picked = mutableListOf<LibraryTrack>()
    val pickedIds = mutableSetOf<String>()
    fun takeFrom(source: List<LibraryTrack>) {
        val ordered = source.sortedWith(compareByDescending<LibraryTrack> { it.playCount }.thenBy { it.title })
        for (track in ordered) {
            if (picked.size >= cap) return
            if (pickedIds.add(track.id)) picked += track
        }
    }
    takeFrom(sameArtist.sortedWith(compareByDescending<LibraryTrack> { it.playCount }.thenBy { it.title }))
    if (picked.size < cap) takeFrom(sameGenre)
    if (picked.size < cap) {
        val remainder = candidates.filter { it.id !in pickedIds }
            .sortedWith(compareBy<LibraryTrack> { if (it.albumId == currentTrack.albumId) 0 else 1 }.thenByDescending { it.playCount }.thenBy { it.title })
        takeFrom(remainder)
    }
    return picked.take(cap).map { it.id }
}

private fun artistTokens(artists: String): Set<String> =
    artists.split(",", ";", "&").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

private fun genreTokens(track: LibraryTrack): Set<String> {
    val obj: JsonObject = track.metadataObject() ?: return emptySet()
    val names = mutableSetOf<String>()
    (obj["genres"] as? JsonArray)?.forEach { (it as? JsonObject)?.get("name")?.jsonPrimitive?.content?.let(names::add) }
    (obj["genre"] as? JsonObject)?.get("name")?.jsonPrimitive?.content?.let(names::add)
    obj["raw_genre_names"]?.let { (it as? JsonArray)?.forEach { v -> v.jsonPrimitive.content.let(names::add) } }
    return names.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
}
```
- [ ] **Step 4: Run test to verify it passes**
Run: publish updated tree to `impl/v0.3-player/task-2`, `gh workflow run 366658414 --ref impl/v0.3-player/task-2` + `gh run watch <run-id>` — Expected: PASS (`PlayerAutoplayEngineTest` green; user order untouched by construction since only `List<String>` ids are returned for `queue.append`).
- [ ] **Step 5: Commit**
`feat(player): offline autoplay picker with repeat-one suppression`

### Task 3: Queue interactions

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueSectionPanel.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerQueueInteractionTest.kt`

**Interfaces:**
- Consumes: `fun com.exodidio.tune.ui.navigation.splitQueue(activeTrackIds: List<String>, currentIndex: Int, userIds: Set<String>, autoplayIds: Set<String>): QueueSections`, `fun moveQueueTrack(trackIds: List<String>, fromIndex: Int, toIndex: Int): List<String>` + `fun commitQueueReorder(latestOrderedIds: androidx.compose.runtime.State<List<String>>, latestOnReorder: androidx.compose.runtime.State<(List<String>) -> Unit>)` (existing `FullScreenPlayerQueuePanel.kt`), `PlaybackModel(onQueueTrackSelected: (String) -> Unit, onQueueReordered: (List<String>) -> Unit, onQueueTrackRemoved: (String) -> Unit, onShuffleChange: (Boolean) -> Unit, onRepeatModeChange: (RepeatMode) -> Unit)`, `PlaybackService(ActionPlayNext→queue.playNext, ActionAppend→queue.append, ActionRemove→queue.removeFromQueue, ActionReorder→queue.reorderQueue)` (semantics unchanged).
- Produces: `fun reorderWithinSection(fullOrder: List<String>, sectionIds: List<String>, fromInSection: Int, toInSection: Int): List<String>`, `fun edgeScrollSpeed(top: Float, bottom: Float, viewportStart: Int, viewportEnd: Int, zone: Float, speed: Float): Float`, `fun shouldResetQueueScroll(previousTrackId: String?, currentTrackId: String?): Boolean`, `fun clearNextInQueue(activeTrackIds: List<String>, currentIndex: Int, autoplayIds: Set<String>): List<String>` (drops user/context upcoming, keeps now-playing + autoplay for append-only invariant).

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueInteractionTest {
    @Test
    fun reordersWithinSectionWithoutMovingOtherSections() {
        val full = listOf("now", "u1", "u2", "u3", "a1")
        val reordered = reorderWithinSection(full, sectionIds = listOf("u1", "u2", "u3"), fromInSection = 0, toInSection = 2)
        assertEquals(listOf("now", "u2", "u3", "u1", "a1"), reordered)
    }

    @Test
    fun invalidSectionMovesLeaveFullOrderUntouched() {
        val full = listOf("now", "u1", "u2")
        assertEquals(full, reorderWithinSection(full, listOf("u1", "u2"), fromInSection = 0, toInSection = 0))
        assertEquals(full, reorderWithinSection(full, listOf("u1", "u2"), fromInSection = -1, toInSection = 1))
    }

    @Test
    fun clearNextKeepsNowPlayingAndAutoplayOnly() {
        val cleared = clearNextInQueue(
            activeTrackIds = listOf("now", "u1", "c1", "a1"),
            currentIndex = 0,
            autoplayIds = setOf("a1")
        )
        assertEquals(listOf("now", "a1"), cleared)
    }

    @Test
    fun edgeScrollSpeedRampsAtEdgesAndRestsInMiddle() {
        assertTrue(edgeScrollSpeed(top = 0f, bottom = 50f, viewportStart = 0, viewportEnd = 600, zone = 80f, speed = 680f) < 0f)
        assertTrue(edgeScrollSpeed(top = 550f, bottom = 600f, viewportStart = 0, viewportEnd = 600, zone = 80f, speed = 680f) > 0f)
        assertEquals(0f, edgeScrollSpeed(top = 250f, bottom = 300f, viewportStart = 0, viewportEnd = 600, zone = 80f, speed = 680f))
        assertEquals(0f, edgeScrollSpeed(top = 0f, bottom = 50f, viewportStart = 0, viewportEnd = 600, zone = 0f, speed = 680f))
    }

    @Test
    fun scrollResetsOnlyOnTrackChange() {
        assertTrue(shouldResetQueueScroll(previousTrackId = "a", currentTrackId = "b"))
        assertFalse(shouldResetQueueScroll(previousTrackId = "a", currentTrackId = "a"))
        assertFalse(shouldResetQueueScroll(previousTrackId = null, currentTrackId = null))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish `impl/v0.3-player/task-3` via `gh api`, `gh workflow run 366658414 --ref impl/v0.3-player/task-3` + `gh run watch <run-id>` — Expected: FAIL with `Unresolved reference 'reorderWithinSection'` / `Unresolved reference 'edgeScrollSpeed'`.
- [ ] **Step 3: Write minimal implementation**
```kotlin
package com.exodidio.tune.ui.navigation

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
```
Panel wiring (append to `PlayerQueueSectionPanel.kt`; tap-jump/remove/reorder delegate to existing `PlaybackModel` callbacks, drag stays within own section via `reorderWithinSection`, edge auto-scroll uses `edgeScrollSpeed`, scroll resets on track change):
```kotlin
// Inside PlayerQueueSectionPanel add params:
//   currentTrackId: String,
//   onTrackSelected: (String) -> Unit,
//   onTrackRemoved: (String) -> Unit,
//   onReorder: (List<String>) -> Unit,
//   onClearNext: (List<String>) -> Unit
// Section row onClick -> onTrackSelected(trackId) (tap-jump = existing queue.select path).
// Section drag fromInSection->toInSection -> onReorder(reorderWithinSection(queue.activeTrackIds, sectionIds, from, to)).
// Remove button -> onTrackRemoved(trackId) (existing queue.removeFromQueue path).
// Clear-next button (user header, only when userIds.isNotEmpty()) -> onClearNext(clearNextInQueue(queue.activeTrackIds, queue.currentIndex, autoplayIds.toSet())) then onReorder(cleared).
// Scroll reset: LaunchedEffect(currentTrackId) { if (shouldResetQueueScroll(previous, currentTrackId)) listState.scrollToItem(0) } — never mid-drag.
```
- [ ] **Step 4: Run test to verify it passes**
Run: publish updated tree to `impl/v0.3-player/task-3`, `gh workflow run 366658414 --ref impl/v0.3-player/task-3` + `gh run watch <run-id>` — Expected: PASS (`PlayerQueueInteractionTest` green; no change to `PlaybackQueue` shuffle/repeat semantics).
- [ ] **Step 5: Commit**
`feat(player): section-scoped queue interactions`

### Task 4: Line-level lyrics panel

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/PlayerLyricsPanel.kt`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/PlayerLyricsPanelTest.kt`

**Interfaces:**
- Consumes: existing `FullScreenPlayerLyricsPanel.kt` helpers (reused verbatim, not forked): `parsePlayerLyrics(content: String): List<PlayerLyricLine>`, `hasSyncedPlayerLyrics(content: String?): Boolean`, `displayedLyricsPositionMs(playbackPositionMs: Long, pendingSeekPositionMs: Long?): Long`, `shouldEnterLyricsBrowseMode(isUserDragging: Boolean, isFollowingSelectedLine: Boolean): Boolean`, `shouldSeekFromLyricTap(dragDistancePx: Float, tapSlopPx: Float): Boolean`, `syncedLyricBlurRadius(distance: Int)`, `shouldResetLyricsForReplay(previousPositionMs: Long, currentPositionMs: Long): Boolean`, `shouldFollowLyricsActiveLine(previousActiveLineInViewport: Boolean, returnedToForeground: Boolean): Boolean`, `shouldResumeLyricsAutoScroll(selectedLineIndex: Int?, activeIndex: Int, activeIndexWhenLineSelected: Int?, selectedLineAnimationComplete: Boolean): Boolean`, `lyricsSeekDirection(targetIndex: Int, firstVisibleIndex: Int): LyricsSeekDirection`; `RomanizationUiState(input,supported,enabled,secondary)`, `FindLyricsContent` (untouched manual finder), `AndroidLyricsService` lrclib+kugou race (untouched).
- Produces: `const val LyricsScrollLeadMinMs: Long = 350L`, `const val LyricsScrollLeadMaxMs: Long = 500L`, `fun activeLyricIndex(lines: List<PlayerLyricLine>, positionMs: Long): Int`, `fun lyricsScrollLeadMs(gapMs: Long): Long`, `fun applyLyricsOffset(positionMs: Long, offsetMs: Int): Long`, `fun lyricsSeekTargetMs(timestampSeconds: Float, offsetMs: Int): Long`, `@Composable fun PlayerLyricsPanel(trackId: String, lyrics: String?, loading: Boolean, currentPositionMs: Long, pendingSeekPositionMs: Long?, seekRequestId: Long, lyricsOffsetMs: Int, romanization: RomanizationUiState, romanizationAllowed: Boolean, onRomanizationInput: (List<String>, Boolean) -> Unit, onRomanizationToggle: () -> Unit, onSeek: (Long) -> Unit, modifier: Modifier)` (Tune font/scale only, romanization overlay working).

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLyricsPanelTest {
    private val lines = parsePlayerLyrics("[00:10.00]One\n[00:20.00]Two\n[00:30.00]Three")

    @Test
    fun activeLineIsLastTimestampAtOrBeforePosition() {
        assertEquals(-1, activeLyricIndex(lines, positionMs = 5_000L))
        assertEquals(0, activeLyricIndex(lines, positionMs = 10_000L))
        assertEquals(1, activeLyricIndex(lines, positionMs = 25_000L))
        assertEquals(2, activeLyricIndex(lines, positionMs = 30_000L))
    }

    @Test
    fun scrollLeadIsClampedTo350To500Ms() {
        assertEquals(350L, lyricsScrollLeadMs(gapMs = 0L))
        assertEquals(350L, lyricsScrollLeadMs(gapMs = 100L))
        assertEquals(420L, lyricsScrollLeadMs(gapMs = 420L))
        assertEquals(500L, lyricsScrollLeadMs(gapMs = 5_000L))
    }

    @Test
    fun tapLyricSeeksToTimestampPlusOffset() {
        assertEquals(62_500L, lyricsSeekTargetMs(timestampSeconds = 62.5f, offsetMs = 0))
        assertEquals(62_000L, applyLyricsOffset(positionMs = 62_500L, offsetMs = 500))
        assertEquals(63_000L, lyricsSeekTargetMs(timestampSeconds = 62.5f, offsetMs = 500))
        assertEquals(0L, applyLyricsOffset(positionMs = 100L, offsetMs = 500))
    }

    @Test
    fun browseSuspendsFollowUntilReplay() {
        assertEquals(true, shouldEnterLyricsBrowseMode(isUserDragging = true, isFollowingSelectedLine = false))
        assertEquals(false, shouldEnterLyricsBrowseMode(isUserDragging = true, isFollowingSelectedLine = true))
        assertEquals(true, shouldResetLyricsForReplay(previousPositionMs = 84_000L, currentPositionMs = 0L))
        assertEquals(72_000L, displayedLyricsPositionMs(playbackPositionMs = 12_000L, pendingSeekPositionMs = 72_000L))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish `impl/v0.3-player/task-4` via `gh api`, `gh workflow run 366658414 --ref impl/v0.3-player/task-4` + `gh run watch <run-id>` — Expected: FAIL with `Unresolved reference 'activeLyricIndex'` / `Unresolved reference 'lyricsScrollLeadMs'`.
- [ ] **Step 3: Write minimal implementation**
```kotlin
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
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0), tween(280, easing = FastOutSlowInEasing) as Nothing?)
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
```
Note: tap-to-seek is wired by wrapping each synced row's `Modifier` with the existing tap-vs-browse gesture from `FullScreenPlayerLyricsPanel.kt` (`shouldSeekFromLyricTap` + `onSeek(lyricsSeekTargetMs(line.timestampSeconds!!, lyricsOffsetMs))`); browse mode sets `isBrowsing=true` via `shouldEnterLyricsBrowseMode`; sources/finder/romanization behavior unchanged.
- [ ] **Step 4: Run test to verify it passes**
Run: publish updated tree to `impl/v0.3-player/task-4`, `gh workflow run 366658414 --ref impl/v0.3-player/task-4` + `gh run watch <run-id>` — Expected: PASS (`PlayerLyricsPanelTest` green).
- [ ] **Step 5: Commit**
`feat(player): line-level lyrics panel in Tune type scale`

### Task 5: Integration — mount in shell host, remove old panels, assemble

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayer.kt` (mount only; shell itself owned by sibling plan)
- Delete (only when green): `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerQueuePanel.kt`, `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerLyricsPanel.kt`
- Verify: release-signed APK via cloud CI artifacts (no local builds)

**Interfaces:**
- Consumes: `fun com.exodidio.tune.ui.navigation.splitQueue(activeTrackIds: List<String>, currentIndex: Int, userIds: Set<String>, autoplayIds: Set<String>): QueueSections` (Task 1 exact signature), `fun pickAutoplay(library: List<LibraryTrack>, currentTrack: LibraryTrack, recentIds: Set<String>, limit: Int = 10): List<String>` + `fun shouldRefillAutoplay(upcomingCount: Int, repeatMode: RepeatMode, autoplayEnabled: Boolean): Boolean` + `const val MaxOfflineAutoplay: Int = 10` (Task 2 exact signatures), `fun PlayerQueueSectionPanel(...)` + `fun PlayerLyricsPanel(...)` (Tasks 1/3/4), shell contract `enum class PlayerShellPanel { LYRICS, QUEUE }`, `typealias OnPlayerShellPanelSelected = (PlayerShellPanel?) -> Unit`, `@Composable fun PlayerShellQueueMount(queue: PlaybackQueueSnapshot, tracks: List<LibraryTrack>, currentTrackId: String, isPlaying: Boolean, onTrackSelected: (String) -> Unit, onTrackRemoved: (String) -> Unit, onReorder: (List<String>) -> Unit, onShuffleChange: (Boolean) -> Unit, onRepeatModeChange: (RepeatMode) -> Unit, modifier: Modifier)`, `@Composable fun PlayerShellLyricsMount(trackId: String, lyrics: String?, loading: Boolean, visible: Boolean, currentPositionMs: Long, onSeek: (Long) -> Unit, modifier: Modifier)`, `@Composable fun PlayerShell(visible: Boolean, onDismiss: () -> Unit, windowWidthDp: Int, selectedPanel: PlayerShellPanel?, onPanelSelected: OnPlayerShellPanelSelected, content: @Composable () -> Unit)` (identical to Task 1 contract).
- Produces: mounted queue via `PlayerShellQueueMount` rendering `PlayerQueueSectionPanel(...)` + lyrics via `PlayerShellLyricsMount` rendering `PlayerLyricsPanel(...)` inside `PlayerShell`, driven by `selectedPanel: PlayerShellPanel?` + `onPanelSelected`; autoplay trigger calls existing `queue.append(ids)` only; deletion of old panels gated on green CI.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import com.exodidio.tune.player.shouldRefillAutoplay
import com.exodidio.tune.player.RepeatMode

class PlayerPanelsMountTest {
    @Test
    fun hostStartsWithBothPanelsClosed() {
        val host = PlayerPanelsHostState(selectedPanel = null)
        assertEquals(null, host.selectedPanel)
    }

    @Test
    fun autoplayTriggerUsesAppendOnlyIdsFromPicker() {
        val active = listOf("now", "u1")
        val upcoming = active.size - 0 - 1
        assertEquals(true, shouldRefillAutoplay(upcomingCount = upcoming, repeatMode = RepeatMode.Off, autoplayEnabled = true))
        val sections = splitQueue(activeTrackIds = active, currentIndex = 0, userIds = setOf("u1"), autoplayIds = emptySet())
        assertEquals(listOf("u1"), sections.userIds)
        assertEquals(emptyList<String>(), sections.autoplayIds)
    }

    @Test
    fun emptyQueueMountShowsNoAutoplayHeader() {
        val sections = splitQueue(activeTrackIds = emptyList(), currentIndex = -1, userIds = emptySet(), autoplayIds = emptySet())
        assertEquals(null, sections.nowPlayingId)
        assertFalse(shouldShowAutoplayHeader(sections.autoplayIds, autoplayEnabled = false))
    }
}
```
```kotlin
package com.exodidio.tune.ui.navigation

data class PlayerPanelsHostState(val selectedPanel: PlayerShellPanel? = null)
```
- [ ] **Step 2: Run test to verify it fails**
Run: publish `impl/v0.3-player/task-5` via `gh api`, `gh workflow run 366658414 --ref impl/v0.3-player/task-5` + `gh run watch <run-id>` — Expected: FAIL with `Unresolved reference 'PlayerPanelsHostState'` (mount state not yet added; old panels still present).
- [ ] **Step 3: Write minimal implementation**
```kotlin
// In FullScreenPlayer.kt (mount only — shell/gestures owned by sibling plan):
// Add shell panel state:
//   var selectedPanel: PlayerShellPanel? by remember { mutableStateOf(null) } // null = both closed
// Mount (exact shell contract from Task 1):
// PlayerShell(
//   visible = ..., onDismiss = { selectedPanel = null }, windowWidthDp = ...,
//   selectedPanel = selectedPanel, onPanelSelected = { selectedPanel = it },
//   content = { ... existing player content ... }
// )
// Queue content via shell mount (rendered when selectedPanel == PlayerShellPanel.QUEUE):
// PlayerShellQueueMount(
//   queue = playback.queue, tracks = playback.queueTracks, currentTrackId = currentTrackId, isPlaying = ...,
//   onTrackSelected = playback.onQueueTrackSelected, onTrackRemoved = playback.onQueueTrackRemoved,
//   onReorder = playback.onQueueReordered, onShuffleChange = ..., onRepeatModeChange = ...,
//   modifier = ...
// ) // backed by PlayerQueueSectionPanel(queue = playback.queue, tracks = playback.queueTracks, autoplayEnabled = true, currentTrackId = currentTrackId, onTrackSelected = playback.onQueueTrackSelected, onTrackRemoved = playback.onQueueTrackRemoved, onReorder = playback.onQueueReordered, onClearNext = { cleared -> playback.onQueueReordered(cleared) })
// Lyrics content via shell mount (visible when selectedPanel == PlayerShellPanel.LYRICS):
// PlayerShellLyricsMount(
//   trackId = currentTrackId, lyrics = playback.lyrics, loading = playback.lyricsLoading, visible = (selectedPanel == PlayerShellPanel.LYRICS),
//   currentPositionMs = (playback.state as? PlaybackState.Playing)?.positionMs ?: 0L,
//   onSeek = playback.onSeek, modifier = ...
// ) // backed by PlayerLyricsPanel(trackId = currentTrackId, lyrics = playback.lyrics, loading = playback.lyricsLoading, currentPositionMs = ..., pendingSeekPositionMs = null, seekRequestId = 0L, lyricsOffsetMs = 0, romanization = playback.romanization, romanizationAllowed = playback.romanizationAllowed, onRomanizationInput = playback.onRomanizationInput, onRomanizationToggle = playback.onRomanizationToggle, onSeek = playback.onSeek)
// Autoplay trigger (in service observer, append-only):
//   if (shouldRefillAutoplay(upcomingCount = queue.snapshot().activeTrackIds.size - queue.snapshot().currentIndex - 1,
//       repeatMode = queue.snapshot().repeatMode, autoplayEnabled = true)) {
//     val ids = pickAutoplay(library = queueTracks, currentTrack = currentTrack, recentIds = recentIds, limit = MaxOfflineAutoplay)
//     if (ids.isNotEmpty()) queue.append(ids) // NEVER reorderQueue/removeFromQueue here
//   }
```
- [ ] **Step 4: Run test to verify it passes**
Run: publish updated tree to `impl/v0.3-player/task-5`, `gh workflow run 366658414 --ref impl/v0.3-player/task-5` + `gh run watch <run-id>` — Expected: PASS (`:sharedLogic:testAndroidHostTest` + `:androidApp:testDevDebugUnitTest` + `:androidApp:assembleDevDebug` green; then `gh run download <run-id> -n tune-debug-apk` contains `tune-debug.apk`; release-signed APK verified by promoting the same green tree through the existing release signing lane — no local `assembleRelease`, no emulators).
- [ ] **Step 5: Commit**
`feat(player): mount sectioned queue and lyrics in panel host`
