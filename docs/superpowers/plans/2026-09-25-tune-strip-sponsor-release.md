# Tune Desktop Strip, Sponsor Removal, and Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strip all dead desktop-sync remnants, sponsor links, and unused camera/MLKit/MQTT/eddsa dependencies from Tune, then publish prerelease `v0.2.0-debug.1`.

**Architecture:** Pure-deletion change set (plus one lyrics-default flip, one dialog-copy rewrite, and one release) layered as eight sequential commits on short-lived branches pushed via the `gh api` git-database; each task is gated by a green `ci` workflow run (`sharedLogic:testAndroidHostTest`, `androidApp:testDevDebugUnitTest`, `assembleDevDebug`, branding gate) plus a temporary `tmp-grep-gate.yml` workflow asserting zero remaining references.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle version catalog (`gradle/libs.versions.toml`), Room (untouched), DataStore Preferences, GitHub Actions (`ci.yml`), GitHub Releases, `gh api` git-database endpoints (all changes published from the read-only clone; no local Gradle/node/adb execution)

**Spec:** `docs/superpowers/specs/2026-09-25-performance-settings-sync-design.md` (Sections 5, 6, 8)

## Global Constraints

- GitHub-cloud-only: no local builds/tests/emulators; every change is published via `gh api` git blobs/trees/commits and verified via Actions runs (`gh workflow run` + `gh run watch`).
- Deletion-only except the lyrics default flip (`Desktop` → `AutoFetch`), the `lyrics_info_description` + `AirmedyDialogTest` copy rewrites, and the release itself.
- No behavior change to playback/library/import; the sync store, scanner, and playback wiring are untouched (owned by the sibling settings-sync plan).
- Every deletion batch is gated by green `assembleDevDebug` (via the existing `ci` workflow, dispatched on the task branch) plus a zero-reference grep gate.
- TDD red-green where expressible (lyrics-default unit test, dead-reference grep gates); one commit per task with a `chore:` prefix.

## Review Focus

- Fresh install defaults lyrics to `AutoFetch` with no Desktop option rendered (owner: Task 4, `LyricsSourceTest.defaultSourceIsAutoFetch` + `IntegrationContentTest.lyricsSourceSelectionReportsAutoFetch`).
- About screen shows version/GitHub/License only, no sponsor card (owner: Task 7, `AppNavigationTest.aboutLinkDispatchesOneTimeExternalUrlIntent` still green, sponsor test deleted).
- Zero remaining references to removed deps/strings/symbols — `tmp-grep-gate.yml` asserts empty `grep -rn` output for every removed key/symbol (owner: Tasks 1, 2, 3, 5, 7).
- `androidTest` compiles after test fixes (owner: Task 6, temporary `tmp-androidtest-compile.yml` running `compileDevDebugAndroidTestKotlin`, since `ci.yml` never compiles `androidTest`).
- Release asset digest matches the green-CI APK and the tag points at the green main SHA (owner: Task 8, `sha256sum` vs `gh api .../releases/assets/<id>` digest comparison).

---

## File Structure

```
androidApp/build.gradle.kts                            # Task 1: drop 7 dep lines
gradle/libs.versions.toml                              # Task 1: drop 4 versions + 7 aliases
androidApp/proguard-rules.pro                          # Task 1: drop MLKit + Firebase keeps
androidApp/src/main/res/values{,-en,-de,-es,-fr,-it,-pt,-ru,-ja,-ko,-th,-vi,-zh}/strings.xml
                                                       # Tasks 2/4/5/7: string deletions + 1 rewrite
androidApp/src/main/kotlin/com/exodidio/tune/App.kt            # Task 3: delete formatSyncStorageMegabytes
androidApp/src/main/kotlin/com/exodidio/tune/ui/components/MaterialSymbols.kt  # Task 3
androidApp/src/main/kotlin/com/exodidio/tune/lyrics/LyricsPreferences.kt        # Task 4
androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/IntegrationContent.kt   # Task 4
androidApp/src/main/kotlin/com/exodidio/tune/MainActivity.kt                    # Task 4 (lyrics wiring)
androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/InsightViewModel.kt    # Task 5
androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/InsightComponents.kt   # Task 5 (delete SourceControl)
androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/InsightContent.kt      # Task 5 (drop filter param)
androidApp/src/main/kotlin/com/exodidio/tune/AppDestinationModels.kt           # Task 5 (drop onSourceSelected)
androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/AppDestinationContent.kt  # Task 5 (drop wiring)
androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/AboutContent.kt        # Task 7
androidApp/src/test/kotlin/com/exodidio/tune/lyrics/LyricsSourceTest.kt         # Task 4 (unit test)
androidApp/src/test/kotlin/com/exodidio/tune/ui/screens/InsightViewModelTest.kt # Task 5 (unit test)
androidApp/src/androidTest/kotlin/com/exodidio/tune/AppNavigationTest.kt        # Tasks 6/7 (fix + sponsor test)
androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/screens/IntegrationContentTest.kt  # Task 6
androidApp/src/androidTest/kotlin/com/exodidio/tune/InsightContentTest.kt       # Task 5 (filter removal)
androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/components/AirmedyDialogTest.kt    # Task 6 (copy)
```

---

### Task 1: Remove dead Camera/MLKit/MQTT/eddsa deps + catalog entries + ProGuard keeps

**Files:**
- Modify: `androidApp/build.gradle.kts`, `gradle/libs.versions.toml`, `androidApp/proguard-rules.pro`
- Test: none (gate: green `assembleDevDebug` + zero-reference grep gate)

**Interfaces:**
- Consumes: version catalog aliases `androidx-camera-*`, `mlkit-barcode`, `hivemq-mqtt`, `eddsa`
- Produces: slimmer dependency graph; no Kotlin file references any removed alias (verified: no `androidx.camera`/`mlkit`/`hivemq`/`eddsa`/`i2p` imports exist in `androidApp/src` or `sharedLogic/src`)

- [ ] **Step 1: Write the failing gate**
  Publish a temporary workflow `tmp-grep-gate.yml` on the task branch (via `gh api` git-database, alongside the branch) containing:
  ```yaml
  name: tmp-grep-gate
  on: workflow_dispatch
  jobs:
    grep:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
          with: { ref: ${{ github.ref }} }
        - name: assert zero references to stripped deps
          run: |
            ! grep -rn "androidx.camera\|camera-mlkit\|mlkit\|hivemq\|net.i2p.crypto:eddsa\|libs.eddsa\|libs.mlkit\|libs.hivemq\|libs.androidx.camera" androidApp sharedLogic gradle --include="*.kts" --include="*.toml" --include="*.kt" --include="*.pro" .
  ```
  Expected before the fix: FAIL (7 matches in `build.gradle.kts`, 11 in `libs.versions.toml`, 2 keeps in `proguard-rules.pro`).
- [ ] **Step 2: Run gate to verify it fails**
  Run: push branch `strip/task-1-deps` via `gh api`, then `gh workflow run tmp-grep-gate.yml --ref strip/task-1-deps && gh run watch` — Expected: FAIL with grep matches listed.
- [ ] **Step 3: Make the change**
  In `androidApp/build.gradle.kts:104-110`, delete exactly:
  ```kotlin
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.camera.mlkit)
  implementation(libs.mlkit.barcode)
  implementation(libs.hivemq.mqtt)
  implementation(libs.eddsa)
  ```
  In `gradle/libs.versions.toml:22-25`, delete exactly:
  ```toml
  cameraX = "1.5.1"
  mlkitBarcode = "17.3.0"
  hivemq = "1.3.14"
  eddsa = "0.3.0"
  ```
  In `gradle/libs.versions.toml:58-64`, delete exactly:
  ```toml
  androidx-camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "cameraX" }
  androidx-camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "cameraX" }
  androidx-camera-view = { module = "androidx.camera:camera-view", version.ref = "cameraX" }
  androidx-camera-mlkit = { module = "androidx.camera:camera-mlkit-vision", version.ref = "cameraX" }
  mlkit-barcode = { module = "com.google.mlkit:barcode-scanning", version.ref = "mlkitBarcode" }
  hivemq-mqtt = { module = "com.hivemq:hivemq-mqtt-client", version.ref = "hivemq" }
  eddsa = { module = "net.i2p.crypto:eddsa", version.ref = "eddsa" }
  ```
  In `androidApp/proguard-rules.pro:28-35`, delete exactly:
  ```pro
  # ML Kit loads the bundled barcode pipeline by class name after R8 has run.
  -keep class com.google.mlkit.vision.barcode.bundled.internal.** { *; }

  # Firebase discovers component registrars from manifest metadata and constructs
  # them reflectively. Its consumer rule keeps their names but not constructors.
  -keep class * implements com.google.firebase.components.ComponentRegistrar {
      public <init>();
  }
  ```
  Leave untouched: the JNI `-keepclasseswithmembernames` block (`:24-26`), the entire Netty/JCTools/Log4j/BouncyCastle `dontwarn` block (`:37-142`, still valid for other transitives), and the `packaging.resources.excludes` for `/META-INF/io.netty.versions.properties` in `build.gradle.kts` (harmless without HiveMQ).
- [ ] **Step 4: Re-run gate**
  Run: push updated blobs via `gh api`, `gh workflow run ci.yml --ref strip/task-1-deps && gh run watch` — Expected: PASS (`:sharedLogic:testAndroidHostTest`, `:androidApp:testDevDebugUnitTest`, `:androidApp:assembleDevDebug`, branding gate all green); then `gh workflow run tmp-grep-gate.yml --ref strip/task-1-deps && gh run watch` — Expected: PASS (no output, exit 0). Delete `tmp-grep-gate.yml` from the branch afterwards.
- [ ] **Step 5: Commit**
  Commit message: `chore: drop dead desktop sync dependencies` (camera2/lifecycle/view/mlkit, mlkit barcode, hivemq mqtt, eddsa + catalog entries + MLKit/Firebase ProGuard keeps).

---

### Task 2: Delete the 43 dead sync strings in all 13 locale files

**Files:**
- Modify: `androidApp/src/main/res/values/strings.xml` + `values-en/-de/-es/-fr/-it/-pt/-ru/-ja/-ko/-th/-vi/-zh/strings.xml`
- Test: none (gate: green `assembleDevDebug` + zero-reference grep gate)

**Interfaces:**
- Consumes: nothing (verified: zero `R.string.sync_*` / `R.string.settings_sync` / `R.string.sync_library_analysis` references in `androidApp/src/main`, `src/test`, `sharedLogic/src`)
- Produces: 43 fewer string resources per locale; no Kotlin/XML reference to any deleted key

- [ ] **Step 1: Write the failing gate**
  Extend `tmp-grep-gate.yml` on branch `strip/task-2-strings` with:
  ```bash
  ! grep -rn "sync_library_analysis\|settings_sync\|sync_title\|sync_scan_title\|sync_add_device\|sync_empty_title\|sync_empty_description\|sync_device_type_desktop\|sync_status_connected\|sync_status_online\|sync_status_offline\|sync_last_synced\|sync_never_synced\|sync_paired_device_description\|sync_ready_to_connect_description\|sync_revoke\|sync_help\|sync_waiting_title\|sync_waiting_description\|sync_camera_required\|sync_scan_description\|sync_scan_hint\|sync_select_qr_image\|sync_revoke_confirm_title\|sync_revoke_confirm_description\|sync_error_already_paired\|sync_error_invalid_qr\|sync_error_transport\|sync_error_timeout\|sync_error_rejected\|sync_error_expired\|sync_error_invalid_response\|sync_notification_channel\|sync_notification_title\|sync_notification_connecting\|sync_notification_progress\|sync_notification_complete\|sync_notification_failed\|sync_insufficient_storage_title\|sync_insufficient_storage_description\|sync_error_background_timeout\|sync_progress_connecting\|sync_progress_syncing" androidApp/src sharedLogic/src --include="*.kt" --include="*.xml"
  ```
  (Note: the audit memo said "42"; the enumerated key list above has 43 — `sync_*` lines 75–109 are 35 keys plus `:30`, `:71–74` (4), `:138–140` (3). The key list governs, not the count.) Expected before the fix: FAIL (43 key definitions × 13 files, plus `AppNavigationTest` androidTest references — androidTest is fixed in Task 6, so the gate here scopes to `androidApp/src/main`, `src/test`, `sharedLogic/src` and `res/` only).
- [ ] **Step 2: Run gate to verify it fails**
  Run: push branch via `gh api`, `gh workflow run tmp-grep-gate.yml --ref strip/task-2-strings && gh run watch` — Expected: FAIL with the 43 `strings.xml` definitions listed.
- [ ] **Step 3: Make the change**
  In default `values/strings.xml`, delete these exact lines (verified by reading the file):
  - `:30` `<string name="sync_library_analysis">Library analysis</string>`
  - `:71–74` `settings_sync`, `sync_title`, `sync_scan_title`, `sync_add_device`
  - `:75–109` the full `sync_*` block: `sync_empty_title`, `sync_empty_description`, `sync_device_type_desktop`, `sync_status_connected`, `sync_status_online`, `sync_status_offline`, `sync_last_synced`, `sync_never_synced`, `sync_paired_device_description`, `sync_ready_to_connect_description`, `sync_revoke`, `sync_help`, `sync_waiting_title`, `sync_waiting_description`, `sync_camera_required`, `sync_scan_description`, `sync_scan_hint`, `sync_select_qr_image`, `sync_revoke_confirm_title`, `sync_revoke_confirm_description`, `sync_error_already_paired`, `sync_error_invalid_qr`, `sync_error_transport`, `sync_error_timeout`, `sync_error_rejected`, `sync_error_expired`, `sync_error_invalid_response`, `sync_notification_channel`, `sync_notification_title`, `sync_notification_connecting`, `sync_notification_progress`, `sync_notification_complete`, `sync_notification_failed`, `sync_insufficient_storage_title`, `sync_insufficient_storage_description`
  - `:138–140` `sync_error_background_timeout`, `sync_progress_connecting`, `sync_progress_syncing`
  Repeat by **key name** (not line number — offsets vary per locale, e.g. `values-de` carries the sync block at `:30`/`:72–109`/`:138–140` with no `settings_sync` entry) in all 12 other locale files: `values-en`, `values-de`, `values-es`, `values-fr`, `values-it`, `values-pt`, `values-ru`, `values-ja`, `values-ko`, `values-th`, `values-vi`, `values-zh`. Locate each key per file with `grep -n` before deleting.
  Explicitly NOT deleted here (owned elsewhere): `lyrics_source_desktop` + `lyrics_info_description` (Task 4), `insight_desktop`/`insight_other_devices`/`insight_all_devices`/`insight_this_phone` (Task 5), `about_sponsor*` (Task 7), and every library empty-state string mentioning sync (`library_empty_*`, `tracks/artists/albums/genres/composers_empty_description`, `playlists_empty_description`, `playlist_sync_failed`, `playlist_delete_confirm_description`, `*_details_empty_description`) — those belong to the sibling settings-sync plan and must not be touched.
- [ ] **Step 4: Re-run gate**
  Run: push via `gh api`, `gh workflow run ci.yml --ref strip/task-2-strings && gh run watch` — Expected: PASS (assemble green proves no `R.string` reference broke); `gh workflow run tmp-grep-gate.yml --ref strip/task-2-strings && gh run watch` — Expected: PASS. Delete the temp workflow file afterwards.
- [ ] **Step 5: Commit**
  Commit message: `chore: delete dead desktop sync strings in all locales`.

---

### Task 3: Delete dead Kotlin symbols (`formatSyncStorageMegabytes`, `DesktopWindows`)

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/App.kt`, `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/MaterialSymbols.kt`
- Test: none (gate: green `assembleDevDebug` + zero-reference grep gate)

**Interfaces:**
- Consumes: nothing (verified: `formatSyncStorageMegabytes` has zero callers — its only occurrence is the definition at `App.kt:662`, and the insufficient-storage dialog that would have used it is already gone from `App.kt`; `MaterialSymbols.DesktopWindows` has zero usages in `androidApp/src`)
- Produces: one fewer `MaterialSymbols` constant (the `SubsetMaterialSymbolsFontTask` in `build.gradle.kts` reads `MaterialSymbols.kt`, so the subset font simply drops one glyph)
- NOTE (cross-plan fix): `MaterialSymbols.Sync` is NOT deleted — it is a live reference used by the sibling settings-sync plan's new Settings → Music sync root entry (`SettingsContent.kt`: `leadingSymbol = MaterialSymbols.Sync`).

**Interfaces:**
- Consumes: nothing (verified: `formatSyncStorageMegabytes` has zero callers — its only occurrence is the definition at `App.kt:662`, and the insufficient-storage dialog that would have used it is already gone from `App.kt`; `MaterialSymbols.DesktopWindows` has zero usages in `androidApp/src`)
- Produces: one fewer `MaterialSymbols` constant (the `SubsetMaterialSymbolsFontTask` in `build.gradle.kts` reads `MaterialSymbols.kt`, so the subset font simply drops one glyph)

- [ ] **Step 1: Write the failing gate**
  `tmp-grep-gate.yml` on branch `strip/task-3-symbols`:
  ```bash
  ! grep -rn "formatSyncStorageMegabytes\|DesktopWindows" androidApp/src sharedLogic/src --include="*.kt"
  ```
  Expected before the fix: FAIL (2 hits: `App.kt:662`, `MaterialSymbols.kt:98`). The gate deliberately does NOT match `MaterialSymbols.Sync` — that symbol stays live (see NOTE above).
- [ ] **Step 2: Run gate to verify it fails**
  Run: push branch via `gh api`, `gh workflow run tmp-grep-gate.yml --ref strip/task-3-symbols && gh run watch` — Expected: FAIL listing the 2 hits.
- [ ] **Step 3: Make the change**
  In `App.kt`, delete exactly (line 662, file end):
  ```kotlin
  internal fun formatSyncStorageMegabytes(bytes: Long): String = "%,.1f MB".format(bytes / 1024.0 / 1024.0)
  ```
  (plus the single blank line separating it from the `@Preview` block above / `AppPreview` below — keep exactly one blank line between `}` at `:660` and `@Preview`.)
  In `MaterialSymbols.kt:98`, delete exactly:
  ```kotlin
  const val DesktopWindows = "desktop_windows"
  ```
  keeping `const val Power = "power"` above and `const val Sync = "sync"` (`:99`) plus `const val Add = "add"` below untouched — `Sync` stays because the sibling settings-sync plan uses it for the new Music sync Settings entry.
- [ ] **Step 4: Re-run gate**
  Run: push via `gh api`, `gh workflow run ci.yml --ref strip/task-3-symbols && gh run watch` — Expected: PASS; `gh workflow run tmp-grep-gate.yml --ref strip/task-3-symbols && gh run watch` — Expected: PASS. Delete the temp workflow file afterwards.
- [ ] **Step 5: Commit**
  Commit message: `chore: delete dead sync storage formatter and desktop symbol`.

---

### Task 4: Lyrics default → AutoFetch, remove Desktop option + MainActivity wiring

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/lyrics/LyricsPreferences.kt`, `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/IntegrationContent.kt`, `androidApp/src/main/kotlin/com/exodidio/tune/MainActivity.kt`, `androidApp/src/main/kotlin/com/exodidio/tune/App.kt` (lyrics_info_description display site only — string value change is in Task 4's string edit), all 13 `strings.xml` (rewrite `lyrics_info_description`, delete now-unreferenced `lyrics_source_desktop`)
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/lyrics/LyricsSourceTest.kt` (rewritten; runs in CI `testDevDebugUnitTest`); `androidApp/src/androidTest/.../ui/screens/IntegrationContentTest.kt` (fixed in Task 6)

**Interfaces:**
- Consumes: `LyricsSettings.preferredSource`, `preferredLyrics(...)`, `R.string.lyrics_source_auto_fetch`
- Produces: single-variant `LyricsSource`; `preferredLyrics(provider)` returns provider lyrics; `MainActivity` no longer subscribes to `desktopLyrics`

- [ ] **Step 1: Write the failing test / failing gate**
  Rewrite `androidApp/src/test/kotlin/com/exodidio/tune/lyrics/LyricsSourceTest.kt` as:
  ```kotlin
  package com.exodidio.tune.lyrics

  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertNull
  import org.junit.Test

  class LyricsSourceTest {
      @Test
      fun defaultSourceIsAutoFetch() {
          assertEquals(LyricsSource.AutoFetch, LyricsSettings().preferredSource)
          assertEquals(LyricsSource.AutoFetch, LyricsSource.fromStorage(null))
          assertEquals(LyricsSource.AutoFetch, LyricsSource.fromStorage("desktop"))
          assertEquals(LyricsSource.AutoFetch, LyricsSource.fromStorage("auto_fetch"))
      }

      @Test
      fun preferredLyricsReturnsProviderLyrics() {
          assertEquals("provider", preferredLyrics("provider"))
          assertNull(preferredLyrics(null))
      }
  }
  ```
  Expected before the fix: FAIL — `LyricsSource.Desktop` still exists so `fromStorage("desktop")` returns `Desktop`, the default is `Desktop`, and `preferredLyrics` still takes three args (compile error in the test).
- [ ] **Step 2: Run gate to verify it fails**
  Run: push branch `strip/task-4-lyrics` via `gh api`, `gh workflow run ci.yml --ref strip/task-4-lyrics && gh run watch` — Expected: FAIL at `:androidApp:testDevDebugUnitTest` with `LyricsSourceTest` compilation/expectation failures.
- [ ] **Step 3: Make the change**
  In `LyricsPreferences.kt:16-36`, replace with:
  ```kotlin
  internal enum class LyricsSource(val storageValue: String) {
      AutoFetch("auto_fetch"),
      ;

      companion object {
          fun fromStorage(value: String?) = entries.firstOrNull { it.storageValue == value } ?: AutoFetch
      }
  }

  internal data class LyricsSettings(
      val preferredSource: LyricsSource = LyricsSource.AutoFetch,
      val lrclib: Boolean = true,
      val kugou: Boolean = true,
      val romanizationEnabled: Boolean = false,
  )

  internal fun preferredLyrics(provider: String?): String? = provider
  ```
  (Legacy stored value `"desktop"` now falls through to `AutoFetch` — no migration code needed. The `preferredSource` field and `setPreferredSource` are kept so stored prefs keep parsing; the enum just has one variant. Spelling must be exactly `AutoFetch` in all files.)
  In `IntegrationContent.kt:128-131`, replace with:
  ```kotlin
  options = listOf(
      SelectionOption(LyricsSource.AutoFetch, R.string.lyrics_source_auto_fetch),
  ),
  ```
  In `MainActivity.kt:217-234`, delete the `desktopLyricsFlow` block and simplify (keep `providerLyricsFlow` as-is at `:220-222`):
  ```kotlin
  val providerLyricsFlow = remember(lyricsTrackId) {
      lyricsTrackId?.let(AndroidSyncRuntime.syncStore()::providerLyrics) ?: flowOf(null)
  }
  val providerLyrics by providerLyricsFlow.collectAsStateWithLifecycle(initialValue = null)
  var manualLyricsOverride by remember { mutableStateOf<ManualLyricsOverride?>(null) }
  LaunchedEffect(lyricsTrackId) {
      if (manualLyricsOverride?.trackId != lyricsTrackId) manualLyricsOverride = null
  }
  val lyrics = manualLyricsOverride?.takeIf { it.trackId == lyricsTrackId }?.content
      ?: com.exodidio.tune.lyrics.preferredLyrics(providerLyrics)
  var lyricsLoadingTrackId by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(lyricsTrackId, providerLyrics, lyricsSettings, manualLyricsOverride) {
      if (lyricsTrackId == null || manualLyricsOverride?.trackId == lyricsTrackId || !providerLyrics.isNullOrBlank()) return@LaunchedEffect
  ```
  (i.e. delete `desktopLyricsFlow`/`desktopLyrics` and the `(lyricsSettings.preferredSource == ... LyricsSource.Desktop && ...)` clause; the fetch-on-missing-provider behavior is unchanged.)
  Strings: delete `lyrics_source_desktop` (`values/strings.xml:243`, plus the matching key in each of the 12 other locales — locate per file via `grep -n lyrics_source_desktop`) since nothing references it after the `IntegrationContent` edit. Rewrite `lyrics_info_description` (`values/strings.xml:247`, shown at `App.kt:480` via `stringResource(R.string.lyrics_info_description)`) from `You can choose to prioritize lyrics from desktop or fetch them automatically. Lyrics fetched automatically on mobile are not synced back to desktop.` to `Lyrics are fetched automatically from the enabled providers.` Apply the identical new English sentence to `values-en` and to the other 11 locales (replacing their translations — locate each via `grep -n lyrics_info_description`; a follow-up localization pass can re-translate, but no locale may keep desktop-pointing copy).
- [ ] **Step 4: Re-run gate**
  Run: push via `gh api`, `gh workflow run ci.yml --ref strip/task-4-lyrics && gh run watch` — Expected: PASS (`LyricsSourceTest` green, `assembleDevDebug` green); plus grep gate `! grep -rn "LyricsSource\.Desktop\|lyrics_source_desktop\|desktopLyrics" androidApp/src/main androidApp/src/test sharedLogic/src --include="*.kt" --include="*.xml"` — Expected: PASS (the only surviving `desktopLyrics` reference must be the `syncStore()::desktopLyrics` DAO method itself, which the sibling plan owns — if it appears, narrow the gate to `MainActivity.kt`, `IntegrationContent.kt`, `LyricsPreferences.kt`, and `res/`).
- [ ] **Step 5: Commit**
  Commit message: `chore: default lyrics to autofetch and drop desktop source`.

---

### Task 5: Remove dead Insight Desktop/Other filter + pairing linkage + insight strings

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/InsightViewModel.kt`, `.../InsightComponents.kt` (delete `SourceControl`), `.../InsightContent.kt` (drop `onSourceSelected` param + call site), `androidApp/src/main/kotlin/com/exodidio/tune/AppDestinationModels.kt`, `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/AppDestinationContent.kt`, all 13 `strings.xml` (delete `insight_desktop`, `insight_other_devices`, `insight_all_devices`, `insight_this_phone` — the last two die with `SourceControl`)
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/screens/InsightViewModelTest.kt` (rewritten; runs in CI), `androidApp/src/androidTest/kotlin/com/exodidio/tune/InsightContentTest.kt` (filter clicks removed)

**Interfaces:**
- Consumes: `InsightUiState`, `InsightRawData`, `buildInsightUiState(...)` (all callers updated in lockstep)
- Produces: local-only Insight with period selectors only; no `InsightSourceFilter`, no pairing imports, no source-device branching

- [ ] **Step 1: Write the failing test / failing gate**
  Rewrite `InsightViewModelTest.kt`: build `InsightRawData(library = bundle, dailyTracks = ..., dailyAttempts = ...)` with NO `identity`/`desktop` args, call `buildInsightUiState(raw, InsightPeriod.SevenDays, InsightPeriod.SevenDays, LocalDate.parse("2026-01-10"))` (no source arg), and assert the former `All`-filter numbers: `listenedSeconds == 960`, `plays == 6`, `streakDays == 3`, `averageSessionSeconds == 233`, `changePercent == 220.0 ± 0.01`, top-track order `[one, two, three]`; drop the `ThisPhone` test's source assertions and the `desktopName`/`hasDesktopSource`/`hasOtherSources` assertions (keep its library-projection assertions: tracks 3, albums 2, playlists 1, bytes 6000). Delete imports of `MobileIdentity`, `MobilePlatform`, `PairedDesktop`. Expected before the fix: FAIL (compile error — `InsightRawData` still requires `identity`/`desktop`, `buildInsightUiState` still takes a source param).
- [ ] **Step 2: Run gate to verify it fails**
  Run: push branch `strip/task-5-insight` via `gh api`, `gh workflow run ci.yml --ref strip/task-5-insight && gh run watch` — Expected: FAIL at `:androidApp:testDevDebugUnitTest` (`InsightViewModelTest` does not compile).
- [ ] **Step 3: Make the change**
  In `InsightViewModel.kt`: delete imports `:21-22` (`com.exodidio.tune.pairing.MobileIdentity`, `com.exodidio.tune.pairing.PairedDesktop`); delete the enum at `:37` (`internal enum class InsightSourceFilter { All, ThisPhone, Desktop, Other }`); from `InsightUiState` (`:71-80`) delete `:74` `val sourceFilter...`, `:75` `val desktopName...`, `:76` `val hasDesktopSource...`, `:77` `val hasOtherSources...`; from `InsightRawData` (`:89-95`) delete `:93` `val identity: MobileIdentity,` and `:94` `val desktop: PairedDesktop?,`; change the constructor (`:97-103`) to `internal class InsightViewModel(store: AndroidLibrarySyncStore, private val playbackController: PlaybackController, private val today: () -> LocalDate = LocalDate::now)` (drop `:99-100` identity/desktop flows); update `Factory.create` (`:109-114`) to `InsightViewModel(store, playbackController)` (drop `:111-112` `flowOf(MobileIdentity(...))` / `flowOf<PairedDesktop?>(null)`); delete `:119` `sourceFilter` state flow and `:134` `setSourceFilter`; simplify `:124-126` raw combine to `combine(library, store.dailyTrackListeningStats, store.dailyPlaybackAttemptStats) { library, tracks, attempts -> InsightRawData(library, tracks, attempts) }`; simplify `:128-130` to `combine(raw, libraryPeriod, listeningPeriod) { data, libraryRange, listeningRange -> buildInsightUiState(data, libraryRange, listeningRange, today()) }`; rewrite `buildInsightUiState` (`:143-167`) to take no source and return `InsightUiState(libraryPeriod, listeningPeriod, library = ..., listening = ...)` (delete `:150-156` desktopId/sourceIds/effectiveSource block and `:160-163` desktopName/hasDesktopSource/hasOtherSources assignments); in `listeningInsights` (`:200-207`) delete the `matchesSource` lambda and both `.filter { matchesSource(...) }` predicates (keep the date predicates), and in `:254` drop the `matchesSource` predicate from the streak computation.
  In `InsightComponents.kt`, delete the whole `SourceControl` (`:77-91`).
  In `InsightContent.kt`, delete the `:46` parameter `onSourceSelected: (InsightSourceFilter) -> Unit,` and the `:99` call `SourceControl(state, onSourceSelected, Modifier.testTag("insight-source-filter"))`, plus the file's `import ...InsightSourceFilter` line (locate via grep — it is the only such import in the file).
  In `AppDestinationModels.kt`, delete `:30` `import com.exodidio.tune.ui.screens.InsightSourceFilter` and `:51` `val onSourceSelected: (InsightSourceFilter) -> Unit = {},`.
  In `AppDestinationContent.kt`, delete `:55` `import com.exodidio.tune.ui.screens.InsightSourceFilter`, `:192` `val onInsightSourceSelected = destinations.insight.onSourceSelected`, and `:353` `onSourceSelected = onInsightSourceSelected,`.
  Strings: delete `insight_desktop` (`:21`), `insight_other_devices` (`:22`), `insight_all_devices` (`:19`), `insight_this_phone` (`:20`) from default `values/strings.xml` and from all 12 other locales by key name (locate per file via grep).
  Tests: apply the `InsightViewModelTest.kt` rewrite from Step 1. In androidTest `InsightContentTest.kt`: delete `:26` `import ...InsightSourceFilter`; change `:44-46` `InsightUiState(hasOtherSources = true, ...)` to `InsightUiState(...)` without the flag; delete `:78-80` (the `onNodeWithText("All devices")` / `onNodeWithText("Other synced devices")` clicks and the `assertEquals(InsightSourceFilter.Other, ...)` assertion) — keep the `:75-77` period-selector clicks and their assertion.
- [ ] **Step 4: Re-run gate**
  Run: push via `gh api`, `gh workflow run ci.yml --ref strip/task-5-insight && gh run watch` — Expected: PASS (unit tests + assemble green); plus grep gate `! grep -rn "InsightSourceFilter\|hasDesktopSource\|hasOtherSources\|desktopName\|onSourceSelected\|SourceControl\|insight_desktop\|insight_other_devices\|insight_all_devices\|insight_this_phone\|MobileIdentity\|PairedDesktop" androidApp/src/main androidApp/src/test --include="*.kt" --include="*.xml"` — Expected: PASS. (Note: `sharedLogic` pairing files are owned by the sibling plan and are out of scope — the gate deliberately excludes `sharedLogic/`.)
- [ ] **Step 5: Commit**
  Commit message: `chore: remove dead insight desktop filter and pairing linkage`.

---

### Task 6: Fix `AppNavigationTest` compile errors, rename sync-named test, update `AirmedyDialogTest` copy

**Files:**
- Modify: `androidApp/src/androidTest/kotlin/com/exodidio/tune/AppNavigationTest.kt`, `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/screens/IntegrationContentTest.kt`, `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/components/AirmedyDialogTest.kt`
- Test: the modified androidTest files themselves (gate: temporary `tmp-androidtest-compile.yml` — see Step 1)

**Interfaces:**
- Consumes: `LyricsSource.AutoFetch` (Task 4), `TuneDialog` generic API (unchanged)
- Produces: compiling `androidTest` source set with no sync-named tests and no desktop-pointing dialog copy

- [ ] **Step 1: Write the failing test / failing gate**
  Publish temporary workflow `tmp-androidtest-compile.yml` on branch `strip/task-6-tests`:
  ```yaml
  name: tmp-androidtest-compile
  on: workflow_dispatch
  jobs:
    compile-androidtest:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
          with: { ref: ${{ github.ref }} }
        - uses: actions/setup-java@v4
          with: { distribution: temurin, java-version: "21" }
        - uses: android-actions/setup-android@v3
          with: { packages: platform-tools platforms;android-36 }
        - uses: gradle/actions/setup-gradle@v4
        - name: Compile androidTest
          run: ./gradlew :androidApp:compileDevDebugAndroidTestKotlin --no-daemon
  ```
  Chosen gate (stated explicitly): this temporary workflow, because `ci.yml` never compiles `androidTest` (no emulator, no `connectedCheck`, no `compile*AndroidTest*` task) — plus mandatory human review of the diff. Expected before the fix: FAIL with `unresolved reference: SyncUiState` (`AppNavigationTest.kt:78`), `unresolved reference: sync_insufficient_storage_title` (`:85`), and `unresolved reference: Desktop` (`IntegrationContentTest.kt:74`).
- [ ] **Step 2: Run gate to verify it fails**
  Run: push branch via `gh api`, `gh workflow run tmp-androidtest-compile.yml --ref strip/task-6-tests && gh run watch` — Expected: FAIL with the unresolved-reference errors above.
- [ ] **Step 3: Make the change**
  In `AppNavigationTest.kt`: delete the entire `insufficientStorageAlertIsGlobalAndDismissible` test (`:71-90`) — it tests the removed sync-failure dialog; `SyncUiState` is undefined anywhere in the repo and `formatSyncStorageMegabytes`/`sync_insufficient_storage_title` are deleted in Tasks 3/2. Delete `:39` `import com.exodidio.tune.sync.AndroidSyncState` (verified: its only use in the file was `:78`). Rename `:518` `fun homeDisplaysSyncPlaceholderWhenThereAreNoTracks()` to `fun homeDisplaysEmptyLibraryMessageWhenThereAreNoTracks()` with the body byte-identical (`:519-523` still assert `R.string.library_empty_title`/`library_empty_description` — that copy belongs to the sibling settings-sync plan and must NOT be reworded here). Leave `:102-113` (`selectingLibraryDispatchesIntentAndUpdatesTheVisibleDestination`) and `:750-753` (`library_empty_title` assertion) untouched for the same reason.
  In `IntegrationContentTest.kt`: change `:74` `var source = LyricsSource.Desktop` to `var source = LyricsSource.AutoFetch`; delete `:87` `composeTestRule.onNodeWithText("Desktop sync").performClick()` so the test asserts the AutoFetch selection path with `assertEquals(LyricsSource.AutoFetch, source)`.
  In `AirmedyDialogTest.kt` (`TuneDialogTest` — a generic dialog test whose copy happens to be desktop-flavored; decision: update copy to non-desktop wording, keep both tests): change `:25-26` title/description from `"Disconnect desktop?"` / `"The desktop remains authorized until revoked there."` to `"Delete this item?"` / `"This permanently removes the selected item."`, and `:29` `confirmLabel = "Revoke"` to `confirmLabel = "Delete"` with `:36` `onNodeWithText("Revoke")` → `onNodeWithText("Delete")`; change `:46-47` from `"Not enough storage"` / `"Needs 2 GB, 1 GB available."` to `"Something went wrong"` / `"Please try again later."` (dismiss path via `"Close"` unchanged).
- [ ] **Step 4: Re-run gate**
  Run: push via `gh api`, `gh workflow run tmp-androidtest-compile.yml --ref strip/task-6-tests && gh run watch` — Expected: PASS; plus `gh workflow run ci.yml --ref strip/task-6-tests && gh run watch` — Expected: PASS (main source set unaffected). Delete the temp workflow file afterwards.
- [ ] **Step 5: Commit**
  Commit message: `chore: fix androidtest compile errors and drop desktop dialog copy`.

---

### Task 7: Sponsor removal (`AboutContent`, 13 locale files, sponsor test)

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/AboutContent.kt`, all 13 `strings.xml`
- Test: `androidApp/src/androidTest/kotlin/com/exodidio/tune/AppNavigationTest.kt` (delete sponsor test; keep github test)

**Interfaces:**
- Consumes: `R.string.about_version`, `R.string.about_github`, `R.string.about_license` (kept)
- Produces: About screen with hero + version/GitHub/License only; zero `about_sponsor*` references

- [ ] **Step 1: Write the failing gate**
  `tmp-grep-gate.yml` on branch `strip/task-7-sponsor`:
  ```bash
  ! grep -rn "about_sponsor\|Sponsor Tune\|GitHub Sponsors\|Ko-fi\|Buy Me a Coffee\|Patreon\|sponsors/exodidio\|ko-fi\.com\|buymeacoffee\|patreon\.com" androidApp/src sharedLogic/src --include="*.kt" --include="*.xml"
  ```
  Expected before the fix: FAIL (hits in `AboutContent.kt:27-30,77,83,87,91,95`, 5 keys × 13 `strings.xml`, `AppNavigationTest.kt:497-515`).
- [ ] **Step 2: Run gate to verify it fails**
  Run: push branch via `gh api`, `gh workflow run tmp-grep-gate.yml --ref strip/task-7-sponsor && gh run watch` — Expected: FAIL listing the sponsor hits.
- [ ] **Step 3: Make the change**
  In `AboutContent.kt`: delete `:22` `import com.exodidio.tune.ui.components.LabeledCard` (verified: its only use in this file is the sponsor card at `:76`); delete `:27-30`:
  ```kotlin
  private const val TuneGithubSponsorsUrl = "https://github.com/sponsors/exodidio"
  private const val TuneKofiUrl = "https://ko-fi.com/exodidio"
  private const val TuneBuyMeACoffeeUrl = "https://buymeacoffee.com/exodidio2"
  private const val TunePatreonUrl = "https://www.patreon.com/c/exodidio"
  ```
  keeping `:25-26` `TuneGithubUrl`/`TuneLicenseUrl`; delete the sponsor card block `:76-102` (the `LabeledCard(label = stringResource(R.string.about_sponsor), ...)` with the four `ActionListItem`s for `about_sponsor_github`/`about_sponsor_kofi`/`about_sponsor_bmac`/`about_sponsor_patreon` wired to the four deleted URLs), keeping `:42-75` (hero + version/GitHub/License `ActionList`). (`padding` import stays — still used; `stringResource` still used.)
  Strings: delete `about_sponsor`, `about_sponsor_github`, `about_sponsor_kofi`, `about_sponsor_bmac`, `about_sponsor_patreon` (`values/strings.xml:256-260` — locate per file via `grep -n about_sponsor`) in all 13 locale files.
  In `AppNavigationTest.kt`, delete the sponsor test (`:497-515`, `aboutSponsorLinkDispatchesOneTimeExternalUrlIntent`, which clicks `R.string.about_sponsor_github` and asserts `AppIntent.OpenExternalUrl("https://github.com/sponsors/exodidio")`), keeping the github test (`:477-495` `aboutLinkDispatchesOneTimeExternalUrlIntent` asserting `AppIntent.OpenExternalUrl("https://github.com/Exodi-dio/tune")`).
- [ ] **Step 4: Re-run gate**
  Run: push via `gh api`, `gh workflow run ci.yml --ref strip/task-7-sponsor && gh run watch` — Expected: PASS; `gh workflow run tmp-grep-gate.yml --ref strip/task-7-sponsor && gh run watch` — Expected: PASS. Delete the temp workflow file afterwards.
- [ ] **Step 5: Commit**
  Commit message: `chore: remove sponsor links from about screen`.

---

### Task 8: Release `v0.2.0-debug.1` (prerelease APK from green main)

**Files:**
- Modify: none (release task — `gh release` commands only, no commit)

**Interfaces:**
- Consumes: green `main` HEAD + its `ci` run artifacts (`tune-debug-apk`)
- Produces: prerelease `v0.2.0-debug.1` with APK asset, published (non-draft), link returned

- [ ] **Step 1: Verify preconditions (green main)**
  ```bash
  green_sha=$(gh api repos/Exodi-dio/tune/commits/main --jq .sha)
  gh run list --workflow=ci.yml --branch=main --status=success --limit 1
  ```
  Expected: the newest successful `ci` run's head SHA equals `$green_sha`, and its jobs include `Unit tests` (`:sharedLogic:testAndroidHostTest`), `Android unit tests` (`:androidApp:testDevDebugUnitTest`), `Assemble debug` (`:androidApp:assembleDevDebug`), `Branding gate`, and `Snapshot debug APK` — all green. If HEAD is not green, STOP (merge/await the other plan tracks first; never release a red SHA). Confirm no `v0.2.0-debug.1` tag exists yet: `gh api repos/Exodi-dio/tune/releases/tags/v0.2.0-debug.1` must 404.
- [ ] **Step 2: Fetch the green APK**
  Run:
  ```bash
  run_id=$(gh run list --workflow=ci.yml --branch=main --status=success --limit 1 --json databaseId --jq '.[0].databaseId')
  mkdir -p /tmp/opencode/release-v0.2.0-debug.1
  gh run download "$run_id" -n tune-debug-apk -D /tmp/opencode/release-v0.2.0-debug.1
  ls -l /tmp/opencode/release-v0.2.0-debug.1
  sha256sum /tmp/opencode/release-v0.2.0-debug.1/*.apk | tee /tmp/opencode/release-v0.2.0-debug.1/SHA256SUMS
  ```
  Expected: exactly one `.apk`; record its name, byte size, and sha256.
- [ ] **Step 3: Create the prerelease (draft) and upload the asset**
  Run (same flow as `v0.1.0-debug.1`, created as draft so the asset can be verified before publishing):
  ```bash
  gh release create 'v0.2.0-debug.1' /tmp/opencode/release-v0.2.0-debug.1/*.apk#Tune-debug-APK     --target "$green_sha" --prerelease --draft     --title 'Tune v0.2.0-debug.1'     --notes "Debug prerelease: desktop-sync strip, sponsor removal, lyrics default AutoFetch. Built from main @ $green_sha (green CI). Debug-signed; installable for on-device testing."
  ```
  Expected: release created, asset upload reported OK.
- [ ] **Step 4: Verify asset size/state/digest, then publish**
  Run:
  ```bash
  gh api repos/Exodi-dio/tune/releases/tags/v0.2.0-debug.1 --jq '{tag: .tag_name, draft: .draft, prerelease: .prerelease, target: .target_commitish, assets: [.assets[] | {id, name, size, state}]}'
  asset_id=$(gh api repos/Exodi-dio/tune/releases/tags/v0.2.0-debug.1 --jq '.assets[0].id')
  gh api repos/Exodi-dio/tune/releases/assets/"$asset_id" --jq '{name, size, state, digest}'
  ```
  Expected: `size` equals the local APK byte size, `state == "uploaded"`, and the asset `digest` (`sha256:...`) matches `/tmp/opencode/release-v0.2.0-debug.1/SHA256SUMS`. Only then publish:
  ```bash
  gh release edit 'v0.2.0-debug.1' --draft=false
  gh release view 'v0.2.0-debug.1' --json tagName,isDraft,isPrerelease,assets
  ```
  Expected: `isDraft == false`, `isPrerelease == true`, one asset.
- [ ] **Step 5: Return the link (no commit)**
  No commit for this task. Report back exactly: `https://github.com/Exodi-dio/tune/releases/tag/v0.2.0-debug.1` plus the asset name, size, and sha256.

