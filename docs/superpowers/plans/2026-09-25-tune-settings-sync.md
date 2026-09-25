# Tune Settings Sync Relocation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Relocate the local music scan UI from the Library screen to a new Settings → Music sync subpage, leaving Library browse-only.

**Architecture:** Navigation-only relocation: a new `AppStackPage.SettingsMusicSync` entry routes through the existing `OpenPage`/`openPage` stack machinery to a new `MusicSyncContent` screen that hosts the verbatim permission/launcher/scanner block moved out of `LibraryContent`; `LocalLibraryScanner`, `reduceScanPermission`, and the `commitLocalLibrary` path are untouched, and Library keeps its signature so its single host (`AppDestinationContent.kt:659`) is unaffected.

**Tech Stack:** Kotlin, Jetpack Compose (eager Column, ActionList, ActivityResult APIs), MediaStore (`READ_MEDIA_AUDIO`), Room-backed `AndroidLibrarySyncStore`, JUnit4 + kotlinx-coroutines-test unit tests, Compose UI instrumentation tests (on-device only), GitHub Actions CI, gh api git-database publishing.

**Spec:** `docs/superpowers/specs/2026-09-25-performance-settings-sync-design.md` (Section 4)

## Global Constraints

- GitHub-cloud-only: no local builds/tests/emulators; every change is published via `gh api` git blobs/trees/commits and verified via Actions runs.
- Keep scan behavior identical: same permission flow, same `LocalLibraryScanner`, same `commitLocalLibrary` commit path.
- Library becomes browse-only: zero scan/sync rows after Task 3.
- New copy must not reference a desktop (or computer/escritorio/ordinateur/equivalent) in any locale.
- TDD red-green where expressible; UI-only changes use a review step quoting exact changed lines plus green assemble.
- One commit per task, `feat:` prefix.

## Review Focus

- Denying audio permission twice shows the rationale state with a live scan button, never a dead button (Task 2; `LocalScanStateTest.deniedWithRationaleRequestsIt` + rationale `Text` in `MusicSyncContent`).
- Tapping scan while granted shows the `CircularProgressIndicator` trailing content inside the Settings screen (Task 2; review quotes the moved `trailingContent` block, `MusicSyncContentTest` asserts the row).
- Library root list shows zero scan/sync UI (Task 3; updated `LibraryContentTest` asserts the scan row does not exist).
- Empty library/playlist copy points at the Settings → Music sync scan and contains no desktop wording in any of the 13 locales (Task 4; `DesktopCopyGateTest`).
- `OpenPage(SettingsMusicSync)` selects Settings, pushes the page, and back returns to the Settings root (Task 1; `SettingsMusicSyncNavigationTest`).

---

## File Structure

- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/AppDestination.kt` — add `SettingsMusicSync` enum entry + Settings destination mapping.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/AppNavigationMetadata.kt` — title metadata for the new page.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/SettingsContent.kt` — new `onMusicSyncSelected` entry between Integration and About.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/AppDestinationContent.kt` — new page branch + root entry wiring.
- Modify: `androidApp/src/main/res/values/strings.xml`, `values-en/…` (+ 11 locales in Task 4) — new `settings_music_sync` / `music_sync_title` / scan-status keys; rewritten empty-state copy.
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/MusicSyncContent.kt` — new screen hosting the moved permission/scan logic.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/LibraryContent.kt` — strip scan UI, becomes browse-only.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/SettingsMusicSyncNavigationTest.kt` — Task 1 gate (unit, runs in CI).
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/DesktopCopyGateTest.kt` — Task 4 gate (unit, runs in CI).
- Modify: `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/screens/LibraryContentTest.kt` — browse-only action list (on-device; not compiled by CI).
- Modify: `androidApp/src/androidTest/kotlin/com/exodidio/tune/AppNavigationTest.kt` — music-sync navigation test (on-device; not compiled by CI).
- Unchanged but relied on: `library/MediaStoreScanner.kt` (`LocalLibraryScanner.state`, `scan()`, `audioPermission()`), `library/LocalLibrary.kt` (`LOCAL_PLAN_ID`, `ScanPermission`, `LocalImportSummary`, `LocalScanUiState`, `reduceScanPermission`), `sync/SyncRuntime.kt`, `sync/SyncDatabase.kt` commit path, `AppIntent.kt` (`OpenPage`), `MainViewModel.kt:90-164` (generic `openPage`, no change needed).

Type consistency (identical everywhere): callback `onMusicSyncSelected`, page `AppStackPage.SettingsMusicSync`, composable `MusicSyncContent(modifier: Modifier = Modifier)`.

### Task 1: Add SettingsMusicSync destination, title metadata, dispatch wiring, Settings root entry

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/AppDestination.kt`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/AppNavigationMetadata.kt`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/SettingsContent.kt`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/AppDestinationContent.kt`
- Modify: `androidApp/src/main/res/values/strings.xml`
- Modify: `androidApp/src/main/res/values-en/strings.xml`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/SettingsMusicSyncNavigationTest.kt` (create; CI gate)
- Test: `androidApp/src/androidTest/kotlin/com/exodidio/tune/AppNavigationTest.kt` (add one test; on-device only, not compiled by CI)

**Interfaces:**
- Consumes: `AppIntent.OpenPage`, `AppStackPage.destination`, `AppStackPage.titleRes()`, `SettingsContent` callbacks.
- Produces: `AppStackPage.SettingsMusicSync` routable to Settings, titled `music_sync_title`, reachable via `onMusicSyncSelected` from the Settings root.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune

import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.settings.ThemeModeStore
import com.exodidio.tune.ui.navigation.titleRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsMusicSyncNavigationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `music sync page belongs to the settings destination`() {
        assertEquals(AppDestination.Settings, AppStackPage.SettingsMusicSync.destination)
    }

    @Test
    fun `music sync page resolves to the music sync title`() {
        assertEquals(R.string.music_sync_title, AppStackPage.SettingsMusicSync.titleRes(AppDestination.Settings))
    }

    @Test
    fun `opening the music sync page selects settings and pushes the page`() = runTest {
        val viewModel = MainViewModel(MusicSyncFakeThemeModeStore())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.dispatch(AppIntent.OpenPage(AppStackPage.SettingsMusicSync))
        advanceUntilIdle()

        assertEquals(AppDestination.Settings, viewModel.uiState.value.selectedDestination)
        assertEquals(AppStackPage.SettingsMusicSync, viewModel.uiState.value.currentPage)

        viewModel.dispatch(AppIntent.NavigateBack)
        advanceUntilIdle()

        assertEquals(AppStackPage.Root, viewModel.uiState.value.currentPage)
    }
}

private class MusicSyncFakeThemeModeStore(initialThemeMode: ThemeMode = ThemeMode.System) : ThemeModeStore {
    private val mutableThemeMode = MutableStateFlow(initialThemeMode)
    private val mutableReduceTransparency = MutableStateFlow(false)
    override val themeMode: Flow<ThemeMode> = mutableThemeMode
    override val reduceTransparency: Flow<Boolean> = mutableReduceTransparency
    override suspend fun setThemeMode(themeMode: ThemeMode) {
        mutableThemeMode.value = themeMode
    }
    override suspend fun setReduceTransparency(enabled: Boolean) {
        mutableReduceTransparency.value = enabled
    }
}
```
(The `ThemeModeStore` members mirror `androidApp/src/test/kotlin/com/exodidio/tune/MainViewModelTest.kt:225-242`; `titleRes` is the existing internal extension in `AppNavigationMetadata.kt:10-33`, visible to same-module tests.)

- [ ] **Step 2: Run test to verify it fails**
Run: publish the test file to branch `plan/settings-sync-nav` via `gh api` git-database endpoints (blobs → tree → commit → update `refs/heads/plan/settings-sync-nav`), then `gh run watch <id> --exit-status` on the CI `build` job — Expected: FAIL at `:androidApp:compileDevDebugUnitTestKotlin` with `unresolved reference: SettingsMusicSync` (enum entry, title key, and wiring do not exist yet).

- [ ] **Step 3: Write minimal implementation**
`AppDestination.kt` — insert after line 30 (`SettingsIntegration,`):
```kotlin
    SettingsIntegration,
    SettingsMusicSync,
    SettingsLastFm,
```
and extend the Settings destination branch (after line 64 `AppStackPage.SettingsIntegration,`):
```kotlin
        AppStackPage.SettingsIntegration,
        AppStackPage.SettingsMusicSync,
        AppStackPage.SettingsLastFm,
```
`AppNavigationMetadata.kt` — insert after line 28 (`AppStackPage.SettingsIntegration -> R.string.integration_title`):
```kotlin
    AppStackPage.SettingsIntegration -> R.string.integration_title
    AppStackPage.SettingsMusicSync -> R.string.music_sync_title
    AppStackPage.SettingsLastFm -> R.string.lastfm_title
```
`SettingsContent.kt` — replace the signature and list (lines 13-42):
```kotlin
internal fun SettingsContent(
    onAppearanceSelected: () -> Unit,
    onPlaybackSelected: () -> Unit,
    onIntegrationSelected: () -> Unit,
    onMusicSyncSelected: () -> Unit,
    onAboutSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ActionList(
            items = listOf(
                ActionListItem(
                    R.string.settings_appearance,
                    leadingSymbol = MaterialSymbols.Palette,
                    onClick = onAppearanceSelected,
                ),
                ActionListItem(
                    R.string.settings_playback,
                    leadingSymbol = MaterialSymbols.Subwoofer,
                    onClick = onPlaybackSelected,
                ),
                ActionListItem(
                    R.string.settings_integration,
                    leadingSymbol = MaterialSymbols.Power,
                    onClick = onIntegrationSelected,
                ),
                ActionListItem(
                    R.string.settings_music_sync,
                    leadingSymbol = MaterialSymbols.Sync,
                    onClick = onMusicSyncSelected,
                ),
                ActionListItem(
                    R.string.settings_about,
                    leadingSymbol = MaterialSymbols.Info,
                    onClick = onAboutSelected,
                ),
            ),
            containerStyle = ActionListContainerStyle.Card,
        )
    }
}
```
(`MaterialSymbols.Sync = "sync"` already exists per `MaterialSymbols.kt:99` and is now a live reference — the sibling strip plan explicitly keeps it; no import change needed — `MaterialSymbols` is already imported.)
`AppDestinationContent.kt` — insert after the `SettingsIntegration` branch (lines 426-430):
```kotlin
                            AppStackPage.SettingsMusicSync -> MusicSyncContent(
                                modifier = settingsPageModifier,
                            )
```
and extend the root `else -> SettingsContent` call (lines 444-458) with:
```kotlin
                                onIntegrationSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsIntegration))
                                },
                                onMusicSyncSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsMusicSync))
                                },
                                onAboutSelected = {
```
(`MusicSyncContent` is created in Task 2 in the same package `com.exodidio.tune.ui.screens`, so no import is needed — matching how `IntegrationContent` at line 426 resolves without an import.)
`values/strings.xml` and `values-en/strings.xml` — insert after the `integration_title` line (line 230: `<string name="integration_title">Integration</string>`):
```xml
    <string name="settings_music_sync">Music sync</string>
    <string name="music_sync_title">Music sync</string>
    <string name="music_sync_permission_rationale">Tune needs audio access to find music on this device. Allow access, then scan again.</string>
    <string name="music_sync_permission_denied">Audio access is off. Turn it on in system Settings to scan for music.</string>
    <string name="music_sync_last_result">Found %1$d tracks · skipped %2$d</string>
    <string name="music_sync_error">Scan failed: %1$s</string>
```
(New keys go in `values/` + `values-en/` only; the other 11 locales fall back to `values/` at runtime. No other locale file may invent a different key name — the key set above is canonical.)
Also append the on-device instrumentation test to `AppNavigationTest.kt` (mirrors `libraryArtistsActionOpensTheArtistList` at lines 116-124; `string()` helper at line 840-841 and `AppHarness` at 854-868 already exist):
```kotlin
    @Test
    fun settingsMusicSyncActionOpensTheMusicSyncPage() {
        val harness = AppHarness(AppUiState(selectedDestination = AppDestination.Settings))
        composeTestRule.setContent { harness.Render() }

        composeTestRule.onNodeWithContentDescription(string(R.string.settings_music_sync)).performClick()

        composeTestRule.onNodeWithContentDescription(string(R.string.library_scan_local)).assertIsDisplayed()
        assertEquals(AppIntent.OpenPage(AppStackPage.SettingsMusicSync), harness.intents.last())
    }
```
(This asserts the scan row by its content description, the same way existing tests address `ActionList` rows, e.g. line 98 `onNodeWithContentDescription(string(R.string.settings_appearance))`.)

- [ ] **Step 4: Run test to verify it passes**
Run: publish via `gh api` to `plan/settings-sync-nav`, then `gh run watch <id> --exit-status` — Expected: PASS (`:androidApp:testDevDebugUnitTest` green including `SettingsMusicSyncNavigationTest`; `:androidApp:assembleDevDebug` green; the new androidTest is not compiled by CI by design).

- [ ] **Step 5: Commit**
Commit message: `feat: add Settings music sync destination and root entry`

### Task 2: Create MusicSyncContent screen hosting the moved permission/scan logic

**Files:**
- Create: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/MusicSyncContent.kt`
- Test: `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/screens/MusicSyncContentTest.kt` (create; on-device only, not compiled by CI)
- Relied on unchanged: `LocalLibraryScanner.state/refreshPermission/scan()`, `reduceScanPermission`, `audioPermission()`, `AndroidSyncRuntime.syncStore()`, `R.string.library_scan_local` (button label reuse)

**Interfaces:**
- Consumes: `LocalLibraryScanner(context.applicationContext, AndroidSyncRuntime.syncStore())`, `ScanPermission`, scan-status strings from Task 1.
- Produces: `internal fun MusicSyncContent(modifier: Modifier = Modifier)` rendered by the `SettingsMusicSync` branch.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.exodidio.tune.R
import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.ui.theme.TuneTheme
import org.junit.Rule
import org.junit.Test

class MusicSyncContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun musicSyncContentDisplaysTheScanAction() {
        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                MusicSyncContent()
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.onNodeWithText(context.getString(R.string.library_scan_local)).assertIsDisplayed()
    }

    @Test
    fun musicSyncContentShowsTheMusicSyncTitleString() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assert(context.getString(R.string.music_sync_title).isNotBlank())
        assert(context.getString(R.string.settings_music_sync).isNotBlank())
    }
}
```
(Mirrors `LibraryContentTest.kt:24-65` structure: `TuneTheme` + `targetContext.getString`. The reducer/permission behavior stays covered by the unchanged unit test `test/.../library/LocalScanStateTest.kt`.)

- [ ] **Step 2: Review step (UI-only change — no local execution)**
Run: publish to branch `plan/music-sync-screen` via `gh api`, then `gh run watch <id> --exit-status` — Expected: FAIL at `:androidApp:assembleDevDebug` with `unresolved reference: MusicSyncContent` from `AppDestinationContent.kt` (branch added in Task 1, screen not yet created). Quote the referencing lines (`AppStackPage.SettingsMusicSync -> MusicSyncContent(modifier = settingsPageModifier,)`) in the run log review.

- [ ] **Step 3: Write minimal implementation**
```kotlin
package com.exodidio.tune.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exodidio.tune.R
import com.exodidio.tune.library.LocalLibraryScanner
import com.exodidio.tune.library.ScanPermission
import com.exodidio.tune.library.audioPermission
import com.exodidio.tune.sync.AndroidSyncRuntime
import com.exodidio.tune.ui.components.ActionList
import com.exodidio.tune.ui.components.ActionListContainerStyle
import com.exodidio.tune.ui.components.ActionListDividerStyle
import com.exodidio.tune.ui.components.ActionListItem
import com.exodidio.tune.ui.components.MaterialSymbols
import com.exodidio.tune.ui.theme.LocalTuneColors
import kotlinx.coroutines.launch

@Composable
internal fun MusicSyncContent(
    modifier: Modifier = Modifier,
) {
    val colors = LocalTuneColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember(context.applicationContext) {
        LocalLibraryScanner(context.applicationContext, AndroidSyncRuntime.syncStore())
    }
    val scanState by scanner.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context as? Activity
        val rationale = activity?.shouldShowRequestPermissionRationale(audioPermission()) == true
        scope.launch {
            scanner.refreshPermission(granted, rationale)
            if (granted) scanner.scan()
        }
    }
    LaunchedEffect(scanner) {
        val granted = context.checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED
        val activity = context as? Activity
        val rationale = !granted && activity?.shouldShowRequestPermissionRationale(audioPermission()) == true
        scanner.refreshPermission(granted, rationale)
    }
    fun startScan() {
        if (context.checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED) {
            scope.launch { scanner.refreshPermission(true, false); scanner.scan() }
        } else {
            permissionLauncher.launch(audioPermission())
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActionList(
            items = listOf(
                ActionListItem(
                    labelRes = R.string.library_scan_local,
                    leadingSymbol = MaterialSymbols.Refresh,
                    leadingIconTint = colors.primary,
                    trailingContent = if (scanState.scanning) {
                        { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                    } else {
                        null
                    },
                    onClick = ::startScan,
                ),
            ),
            containerStyle = ActionListContainerStyle.Card,
            dividerStyle = ActionListDividerStyle.FullWidth,
        )
        when (scanState.permission) {
            ScanPermission.NeedsRationale -> Text(
                text = stringResource(R.string.music_sync_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
            ScanPermission.Denied -> Text(
                text = stringResource(R.string.music_sync_permission_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
            else -> Unit
        }
        scanState.lastResult?.let { result ->
            Text(
                text = stringResource(R.string.music_sync_last_result, result.inserted, result.skipped),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
        scanState.error?.let { error ->
            Text(
                text = stringResource(R.string.music_sync_error, error),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
    }
}
```
(Line-by-line provenance: the scanner/launcher/`LaunchedEffect`/`startScan`/button block is the verbatim moved code from `LibraryContent.kt:72-119`; the `Column` + `Card` list shell follows the `IntegrationContent.kt:37-53` subpage pattern; `colors.primary`/`colors.textMuted` match existing usages in `LibraryContent.kt:112` and `IntegrationContent.kt:100`; the permission/result/error `Text` rows only read `scanState` — the scanner, reducer, and commit path are untouched.)

- [ ] **Step 4: Review step to verify it passes (UI-only change)**
Run: publish via `gh api` to `plan/music-sync-screen`, then `gh run watch <id> --exit-status` — Expected: PASS (`:androidApp:testDevDebugUnitTest` green, `:androidApp:assembleDevDebug` green). Review quotes the exact added lines: the `ActionListItem(labelRes = R.string.library_scan_local, … trailingContent = if (scanState.scanning) …)` block and the `SettingsMusicSync -> MusicSyncContent` branch resolving. On-device: `MusicSyncContentTest` and denying audio permission twice must show the rationale text with a live scan button.

- [ ] **Step 5: Commit**
Commit message: `feat: add MusicSyncContent hosting local scan`

### Task 3: Strip scan UI from LibraryContent and update its test

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/LibraryContent.kt`
- Test: `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/screens/LibraryContentTest.kt` (modify; on-device only, not compiled by CI)

**Interfaces:**
- Consumes: unchanged `LibraryContent` signature (same callbacks; `AppDestinationContent.kt:659-693` host untouched).
- Produces: browse-only Library root list (search/artists/albums/tracks/genres/composers/playlists + recent grid).

- [ ] **Step 1: Write the failing test**
Replace `libraryContentDisplaysActionListItems` (`LibraryContentTest.kt:24-65`) with the browse-only version plus a scan-absence assertion (existing imports cover `onNodeWithText`/`performClick`; add `import androidx.compose.ui.test.assertDoesNotExist`):
```kotlin
    @Test
    fun libraryContentDisplaysBrowseOnlyActionListItems() {
        var searchClicked = false
        var artistsClicked = false
        var albumsClicked = false
        var tracksClicked = false
        var genresClicked = false
        var composersClicked = false
        var playlistsClicked = false

        composeTestRule.setContent {
            TuneTheme(themeMode = ThemeMode.Dark) {
                LibraryContent(
                    onSearchSelected = { searchClicked = true },
                    onArtistsSelected = { artistsClicked = true },
                    onAlbumsSelected = { albumsClicked = true },
                    onTracksSelected = { tracksClicked = true },
                    onGenresSelected = { genresClicked = true },
                    onComposersSelected = { composersClicked = true },
                    onPlaylistsSelected = { playlistsClicked = true },
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.onNodeWithText(context.getString(R.string.library_scan_local)).assertDoesNotExist()

        composeTestRule.onNodeWithText(context.getString(R.string.library_search)).assertIsDisplayed().performClick()
        assertTrue(searchClicked)

        composeTestRule.onNodeWithText(context.getString(R.string.library_artists)).assertIsDisplayed().performClick()
        assertTrue(artistsClicked)

        composeTestRule.onNodeWithText(context.getString(R.string.library_albums)).assertIsDisplayed().performClick()
        assertTrue(albumsClicked)

        composeTestRule.onNodeWithText(context.getString(R.string.library_tracks)).assertIsDisplayed().performClick()
        assertTrue(tracksClicked)

        composeTestRule.onNodeWithText(context.getString(R.string.library_genres)).assertIsDisplayed().performClick()
        assertTrue(genresClicked)

        composeTestRule.onNodeWithText(context.getString(R.string.library_composers)).assertIsDisplayed().performClick()
        assertTrue(composersClicked)

        composeTestRule.onNodeWithText(context.getString(R.string.library_playlists)).assertIsDisplayed().performClick()
        assertTrue(playlistsClicked)
    }
```
(The `onPlaylistsSelected` parameter already exists on `LibraryContent` at line 69; the recent-grid and context-menu tests at lines 67-125 are unchanged.)

- [ ] **Step 2: Review step (UI-only change — no local execution)**
Run: publish the test change to branch `plan/library-browse-only` via `gh api`, then `gh run watch <id> --exit-status` — Expected: PASS on CI (androidTest is not compiled by CI, so the suite stays green) while the test is red on-device against the unmodified `LibraryContent` (the `library_scan_local` row still exists, so `assertDoesNotExist` fails). Quote the failing on-device assertion when reporting.

- [ ] **Step 3: Write minimal implementation**
In `LibraryContent.kt`, delete lines 72-99 (the `colors`/`context`/`scope`/`scanner`/`scanState`/`permissionLauncher`/`LaunchedEffect`/`startScan` block; keep `var contextTrack` at line 100) and delete the scan `ActionListItem` (lines 109-119), leaving the search item first:
```kotlin
        item {
            ActionList(
                items = listOf(
                    ActionListItem(
                        labelRes = R.string.library_search,
                        leadingSymbol = MaterialSymbols.Search,
                        leadingIconTint = colors.primary,
                        onClick = onSearchSelected,
                    ),
```
Delete these now-unused imports (each verified as used only by the removed block): `android.app.Activity` (line 3), `android.content.pm.PackageManager` (line 4), `androidx.activity.compose.rememberLauncherForActivityResult` (line 5), `androidx.activity.result.contract.ActivityResultContracts` (line 6), `androidx.compose.foundation.layout.size` (line 11), `androidx.compose.material3.CircularProgressIndicator` (line 15), `androidx.compose.runtime.LaunchedEffect` (line 19), `androidx.compose.runtime.collectAsState` (line 20), `androidx.compose.runtime.getValue` (line 21), `androidx.compose.runtime.remember` (line 23 — `remember` for `contextTrack` at line 100 is `remember { mutableStateOf… }`; keep `remember`, drop only if unused: `remember` is still used by `remember { mutableStateOf<LibraryTrack?>(null) }`, so KEEP line 23), `androidx.compose.runtime.rememberCoroutineScope` (line 24), `androidx.compose.ui.platform.LocalContext` (line 27), `com.exodidio.tune.library.LocalLibraryScanner` (line 32), `com.exodidio.tune.library.ScanPermission` (line 33), `com.exodidio.tune.library.audioPermission` (line 34), `com.exodidio.tune.sync.AndroidSyncRuntime` (line 36), `kotlinx.coroutines.launch` (line 48). Keep `remember` (line 23), `mutableStateOf`/`setValue` (lines 22/25, used by `contextTrack`), `LocalTuneColors`/`colors` (line 47, still used by `leadingIconTint = colors.primary`), `R` (line 31), and all other imports.
Net result: `LibraryContent` keeps its exact parameter list (lines 51-71) and renders only the seven browse rows plus the recent-tracks grid (lines 120-214 unchanged).

- [ ] **Step 4: Review step to verify it passes (UI-only change)**
Run: publish via `gh api` to `plan/library-browse-only`, then `gh run watch <id> --exit-status` — Expected: PASS (`:androidApp:testDevDebugUnitTest` green, `:androidApp:assembleDevDebug` green, branding gate green). Review quotes the exact deleted lines (scanner block, launcher, `LaunchedEffect`, `startScan`, scan `ActionListItem`, dropped imports) and confirms the diff touches only `LibraryContent.kt` + `LibraryContentTest.kt`. On-device: the updated `LibraryContentTest` is green, including `assertDoesNotExist` for the scan row.

- [ ] **Step 5: Commit**
Commit message: `feat: strip scan UI from LibraryContent`

### Task 4: Rewrite empty-state copy in all 13 locale files to point at Settings scan

**Files:**
- Modify: `androidApp/src/main/res/values/strings.xml`
- Modify: `androidApp/src/main/res/values-en/strings.xml`
- Modify: `androidApp/src/main/res/values-de/strings.xml`
- Modify: `androidApp/src/main/res/values-es/strings.xml`
- Modify: `androidApp/src/main/res/values-fr/strings.xml`
- Modify: `androidApp/src/main/res/values-it/strings.xml`
- Modify: `androidApp/src/main/res/values-pt/strings.xml`
- Modify: `androidApp/src/main/res/values-ru/strings.xml`
- Modify: `androidApp/src/main/res/values-ja/strings.xml`
- Modify: `androidApp/src/main/res/values-ko/strings.xml`
- Modify: `androidApp/src/main/res/values-th/strings.xml`
- Modify: `androidApp/src/main/res/values-vi/strings.xml`
- Modify: `androidApp/src/main/res/values-zh/strings.xml`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/DesktopCopyGateTest.kt` (create; CI gate)

**Interfaces:**
- Consumes: existing keys `playlists_empty_description` (`LibraryPlaylistsContent.kt:56`), `playlist_sync_failed` (`PlaylistRow.kt:61`), `library_empty_title`/`library_empty_description` (`HomeContent.kt:70-71`); key names unchanged so no Kotlin changes.
- Produces: desktop-free copy pointing at the Settings → Music sync scan; `library_scan_local` value unchanged (reused as the button label in `MusicSyncContent`).

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopCopyGateTest {
    private val localeDirs = listOf(
        "values", "values-en", "values-de", "values-es", "values-fr", "values-it",
        "values-pt", "values-ru", "values-ja", "values-ko", "values-th", "values-vi", "values-zh",
    )
    private val rewrittenKeys = listOf(
        "playlists_empty_description",
        "playlist_sync_failed",
        "library_empty_title",
        "library_empty_description",
    )
    private val banned = listOf(
        "desktop", "escritorio", "ordinateur", "bureau", "computador", "computer",
        "компьютер", "デスクトップ", "데스크톱", "เดสก์ท็อป", "máy tính", "电脑",
    )

    @Test
    fun rewrittenEmptyStateCopyNeverReferencesADesktop() {
        val resDir = File(System.getProperty("user.dir"), "src/main/res")
        assertTrue("res dir missing: ${resDir.absolutePath}", resDir.isDirectory)
        val violations = mutableListOf<String>()
        val factory = DocumentBuilderFactory.newInstance()
        for (locale in localeDirs) {
            val file = File(resDir, "$locale/strings.xml")
            assertTrue("missing $locale/strings.xml", file.isFile)
            val doc = factory.newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            val values = mutableMapOf<String, String>()
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val name = node.attributes.getNamedItem("name").nodeValue
                if (name in rewrittenKeys) {
                    values[name] = node.textContent
                }
            }
            for (key in rewrittenKeys) {
                val value = values[key]
                if (value == null) {
                    violations += "$locale:$key MISSING"
                    continue
                }
                val lowered = value.lowercase(Locale.ROOT)
                val hit = banned.firstOrNull { lowered.contains(it.lowercase(Locale.ROOT)) }
                if (hit != null) {
                    violations += "$locale:$key contains \"$hit\": $value"
                }
            }
        }
        assertTrue(
            "Desktop references in empty-state copy:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}
```
(Pure JVM test: `javax.xml` DOM + JUnit4, no Robolectric; Gradle runs module unit tests with `user.dir` = `androidApp/`, so `src/main/res` resolves. The ban list covers desktop/computer wording in all 12 locales; the word "sync" is intentionally NOT banned because the new copy legitimately names the "Music sync" feature.)

- [ ] **Step 2: Run test to verify it fails**
Run: publish the test file to branch `plan/settings-copy` via `gh api`, then `gh run watch <id> --exit-status` — Expected: FAIL with `Desktop references in empty-state copy:` listing `values:playlists_empty_description contains "desktop": Sync playlists from your connected desktop…`, `values:playlist_sync_failed …`, `values:library_empty_title …`, `values:library_empty_description …` (plus per-locale equivalents).

- [ ] **Step 3: Write minimal implementation**
Edit exactly these 4 keys per file (key names unchanged; `library_scan_local` value untouched). `values/strings.xml` (lines 159, 175, 189-190) and `values-en/strings.xml` (same lines):
```xml
    <string name="playlists_empty_description">Create a playlist to see it here.</string>
```
```xml
    <string name="playlist_sync_failed">Could not update this playlist</string>
```
```xml
    <string name="library_empty_title">No music yet</string>
    <string name="library_empty_description">Scan the music on this device from Settings → Music sync.</string>
```
Before/after for review: `playlists_empty_description`: "Sync playlists from your connected desktop to see them here." → "Create a playlist to see it here."; `playlist_sync_failed`: "Could not sync this playlist to desktop" → "Could not update this playlist"; `library_empty_title`: "No synced music yet" → "No music yet"; `library_empty_description`: "Start Sync on your connected desktop to bring your library here." → "Scan the music on this device from Settings → Music sync."
Per-locale edits (same 4 keys; verified current values via grep before editing):
- `values-de`: `playlists_empty_description` → `Erstelle eine Playlist, um sie hier zu sehen.`; `playlist_sync_failed` → `Diese Playlist konnte nicht aktualisiert werden`; `library_empty_title` → `Noch keine Musik`; `library_empty_description` → `Scanne die Musik auf diesem Gerät unter Einstellungen → Musiksync.`
- `values-es`: `playlists_empty_description` → `Crea una lista para verla aquí.`; `playlist_sync_failed` → `No se pudo actualizar esta lista`; `library_empty_title` → `Aún no hay música`; `library_empty_description` → `Analiza la música de este dispositivo en Ajustes → Sincronización de música.`
- `values-fr` (escape apostrophes as `\'` per existing file convention): `playlists_empty_description` → `Créez une playlist pour la voir ici.`; `playlist_sync_failed` → `Impossible de mettre à jour cette playlist`; `library_empty_title` → `Aucune musique pour l\'instant`; `library_empty_description` → `Analysez la musique de cet appareil dans Réglages → Synchro musique.`
- `values-it`: `playlists_empty_description` → `Crea una playlist per vederla qui.`; `playlist_sync_failed` → `Impossibile aggiornare questa playlist`; `library_empty_title` → `Ancora nessuna musica`; `library_empty_description` → `Scansiona la musica su questo dispositivo in Impostazioni → Sincronizzazione musica.`
- `values-pt`: `playlists_empty_description` → `Crie uma playlist para a ver aqui.`; `playlist_sync_failed` → `Não foi possível atualizar esta playlist`; `library_empty_title` → `Ainda sem música`; `library_empty_description` → `Analise a música neste dispositivo em Definições → Sincronização de música.`
- `values-ru`: `playlists_empty_description` → `Создайте плейлист, чтобы увидеть его здесь.`; `playlist_sync_failed` → `Не удалось обновить этот плейлист`; `library_empty_title` → `Музыки пока нет`; `library_empty_description` → `Просканируйте музыку на этом устройстве в разделе «Настройки» → «Синхронизация музыки».`
- `values-ja`: `playlists_empty_description` → `プレイリストを作成するとここに表示されます。`; `playlist_sync_failed` → `このプレイリストを更新できませんでした`; `library_empty_title` → `音楽がまだありません`; `library_empty_description` → `設定 → 音楽の同期からデバイス内の音楽をスキャンしてください。`
- `values-ko`: `playlists_empty_description` → `재생 목록을 만들면 여기에 표시됩니다.`; `playlist_sync_failed` → `이 재생 목록을 업데이트할 수 없습니다`; `library_empty_title` → `아직 음악이 없습니다`; `library_empty_description` → `설정 → 음악 동기화에서 이 기기의 음악을 스캔하세요.`
- `values-th`: `playlists_empty_description` → `สร้างเพลย์ลิสต์เพื่อดูที่นี่`; `playlist_sync_failed` → `ไม่สามารถอัปเดตเพลย์ลิสต์นี้ได้`; `library_empty_title` → `ยังไม่มีเพลง`; `library_empty_description` → `สแกนเพลงในอุปกรณ์นี้จาก การตั้งค่า → ซิงก์เพลง`
- `values-vi`: `playlists_empty_description` → `Tạo danh sách phát để xem tại đây.`; `playlist_sync_failed` → `Không thể cập nhật danh sách phát này`; `library_empty_title` → `Chưa có bản nhạc nào`; `library_empty_description` → `Quét nhạc trên thiết bị này trong Cài đặt → Đồng bộ nhạc.`
- `values-zh`: `playlists_empty_description` → `创建播放列表后将在此处显示。`; `playlist_sync_failed` → `无法更新此播放列表`; `library_empty_title` → `暂无音乐`; `library_empty_description` → `前往“设置”→“音乐同步”，扫描此设备上的音乐。`
(No Kotlin file changes: the empty-state call sites `HomeContent.kt:70-71`, `LibraryPlaylistsContent.kt:56`, `PlaylistRow.kt:61` keep their key references.)

- [ ] **Step 4: Run test to verify it passes**
Run: publish via `gh api` to `plan/settings-copy`, then `gh run watch <id> --exit-status` — Expected: PASS (`:androidApp:testDevDebugUnitTest` green including `DesktopCopyGateTest`; `:androidApp:assembleDevDebug` green; branding gate green).

- [ ] **Step 5: Commit**
Commit message: `feat: rewrite empty-state copy to point at Settings scan`

