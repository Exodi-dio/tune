# AMLL Lyrics Beta 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Tune's lyrics presentation with pinned AMLL tooling, restore
romanization controls, and publish one verified `v1.2.0-beta.3` universal APK
to `Exodi-dio/tune-betas`.

**Architecture:** GitHub Actions builds browser assets from the pinned
`applemusic-like-lyrics` checkout, Tune hosts those assets in a restricted
WebView, and a narrow bridge supplies lyric data and playback events. Existing
Tune lyric storage and native romanization remain, with the AMLL runtime as the
primary parser/renderer and the native line list as a WebView failure fallback.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView, JavaScript, pnpm,
GitHub Actions, `gh`.

**Spec:**
`docs/superpowers/specs/2026-10-05-amll-lyrics-beta-design.md`

## Global Constraints

- Never run Gradle, Node/pnpm, an emulator, a compiler, a linter, or tests on
  local hardware. Use `gh` to push commits, open/watch the PR CI run, inspect
  logs, download artifacts, and publish releases.
- Pin `amll-dev/applemusic-like-lyrics` at
  `86200dead453bb067e554e989110cbadca8d4756`.
- Pin `amll-dev/amll-ttml-tool` at
  `d4953b351ae073c1447464790fff17aa9bc1d807`.
- Publish exactly one universal APK as tag `v1.2.0-beta.3`.
- Preserve fullscreen artwork removal and fullscreen lyrics removal.
- Keep native Chinese/Korean romanization; TTML transliteration wins when
  present.
- Do not execute the desktop AMLL TTML editor or ship its WASM UI.

## Review Focus

- **Malformed TTML:** invalid XML must use the native fallback, never a blank
  player. Owned by Task 4, test
  `AmllBridgePayloadTest.invalidTtmlProducesFallback`.
- **Stale playback events:** seek/update events from an old track must be
  ignored. Owned by Task 4, test
  `AmllBridgeControllerTest.staleTrackEventIsIgnored`.
- **Romanization availability:** a supported control must remain visible after
  a fresh install even when the preference is false. Owned by Task 5, test
  `romanizationToggleRemainsVisibleWhenPreferenceIsInactive`.
- **AMLL bundle integrity:** a missing, stale, or wrong-commit bundle must fail
  before Gradle starts. Owned by Task 1, test
  `verify-amll-assets rejects a wrong source revision`.
- **Release artifact integrity:** the published asset must match the workflow
  artifact checksum. Owned by Task 7, test
  `verify-release-input rejects a checksum mismatch`.

## Cloud Test Loop

Create one pull request from `impl/v1.2.0-beta.2-lyrics-home` to `main` before
Task 1; reuse it for every cycle. Every RED step pushes only the new test or
assertion and waits for CI to fail for the expected reason. Every GREEN step
pushes the implementation and waits for CI to pass.

```bash
gh pr create --repo Exodi-dio/tune --base main \
  --head impl/v1.2.0-beta.2-lyrics-home \
  --title "AMLL lyrics beta 3" \
  --body "Implements docs/superpowers/specs/2026-10-05-amll-lyrics-beta-design.md"
RUN_ID="$(gh run list --repo Exodi-dio/tune --branch impl/v1.2.0-beta.2-lyrics-home \
  --event pull_request --limit 1 --json databaseId --jq '.[0].databaseId')"
gh run watch "$RUN_ID" --repo Exodi-dio/tune --exit-status
```

If CI compiles only on `pull_request`, use that event for every RED/GREEN
cycle. Never substitute a local test command.

### Task 1: Pinned AMLL asset pipeline

**Files:**

- Create: `scripts/build-amll-assets.sh`
- Create: `tools/amll/index.html`
- Create: `tools/amll/bridge.js`
- Create: `tools/amll/verify-amll-assets.mjs`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/pre-release-apks.yml`
- Modify: `androidApp/build.gradle.kts`

**Interfaces:**

- Consumes: no prior task.
- Produces: generated directory
  `androidApp/build/generated/amllAssets/amll` containing `index.html`,
  `bridge.js`, `amll-core.mjs`, `style.css`, `amll-lyric.mjs`, and
  `amll-ttml.mjs`.
- Produces: Gradle Android source set `amllGeneratedAssets` pointing at that
  directory.

- [ ] **Step 1: Add the failing asset verification test**

Create `tools/amll/verify-amll-assets.mjs` so it:

- reads `AMLL_SOURCE_REVISION`;
- reads `TTML_TOOL_SOURCE_REVISION`;
- exits 2 when it does not equal
  `86200dead453bb067e554e989110cbadca8d4756`;
- exits 2 when `TTML_TOOL_SOURCE_REVISION` does not equal
  `d4953b351ae073c1447464790fff17aa9bc1d807`;
- exits 3 when any required file listed in Interfaces is absent;
- exits 4 when `amll-core.mjs` or `amll-ttml.mjs` does not contain their
  package name marker;
- exits 0 only for the exact pin and complete bundle.

- [ ] **Step 2: Push RED and watch CI fail**

Push `tools/amll/verify-amll-assets.mjs` alone, then run the Cloud Test Loop.
Expected failure: wrong revision or missing bundle with exit code 2/3.

- [ ] **Step 3: Implement the pinned build script**

Create `scripts/build-amll-assets.sh` with `set -euo pipefail`. It must:

- accept output directory as `$1`;
- clone
  `https://github.com/amll-dev/applemusic-like-lyrics.git` into
  `$RUNNER_TEMP/applemusic-like-lyrics`;
- verify `git rev-parse HEAD` equals the exact AMLL pin;
- clone `https://github.com/amll-dev/amll-ttml-tool.git` into
  `$RUNNER_TEMP/amll-ttml-tool` and verify its exact pinned revision;
- enable the repository's `pnpm@11.21.0`, run
  `pnpm install --frozen-lockfile`, and build only `ttml`, `lyric`, and
  `core`;
- copy the three package `dist` outputs, CSS, `tools/amll/index.html`, and
  `tools/amll/bridge.js` into the requested directory;
- run `node tools/amll/verify-amll-assets.mjs` with both source revisions
  exported.

Also create the local shell `index.html` and `bridge.js` needed by the copy
step. `bridge.js` may initially expose only `window.amllReady = true`.

- [ ] **Step 4: Wire asset generation before Gradle**

In both workflows add `actions/setup-node@v4` with Node 22 and a step running:

```bash
scripts/build-amll-assets.sh \
  "$GITHUB_WORKSPACE/androidApp/build/generated/amllAssets/amll"
```

Add the generated directory to `androidApp`'s Android source sets. The step
must run before Gradle tests in both workflows.

- [ ] **Step 5: Push GREEN and watch CI pass**

Push implementation files and run the Cloud Test Loop. Expected: asset
verification and existing CI tests pass.

- [ ] **Step 6: Commit**

```bash
git add scripts/build-amll-assets.sh tools/amll .github/workflows \
  androidApp/build.gradle.kts
git commit -m "build: pin and generate AMLL runtime assets"
```

### Task 2: AMLL format parser fixtures

**Files:**

- Create: `tools/amll/test-parse.mjs`
- Create: `tools/amll/fixtures/word-ttml.ttml`
- Create: `tools/amll/fixtures/line-ttml.ttml`
- Create: `tools/amll/fixtures/tool-schema.ttml`
- Modify: `tools/amll/index.html`
- Modify: `tools/amll/bridge.js`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**

- Consumes: Task 1 generated AMLL bundles.
- Produces: `window.amllParse(format, content) -> object` with
  `{ ok: true, lines: AmllLyricLine[] }` or
  `{ ok: false, error: string }`.
- Produces: `window.amllCreatePlayer(host) -> object` with `setLyricLines`,
  `setCurrentTime`, `seek`, `resize`, and `dispose`.

- [ ] **Step 1: Add failing parser behavior tests**

Create `tools/amll/test-parse.mjs` with Node assertions:

- `word-ttml.ttml` yields three lines, three-word first line, translation and
  romanization text, one duet line, and background-vocal flag.
- `line-ttml.ttml` yields line timing with one word per line.
- enhanced LRC yields synchronized lines.
- malformed XML returns `{ ok: false }`.
- plain text yields one untimed line.

Copy the first two fixtures from the pinned AMLL repository's
`packages/ttml/tests/fixtures`. Author `tool-schema.ttml` against the pinned
tool's `src/types/ttml.ts` and processor `types/ttml.ts`; do not claim a tool
fixture corpus exists.

- [ ] **Step 2: Push RED and watch CI fail**

Push only the test and fixtures; add `node tools/amll/test-parse.mjs` to CI
after asset generation. Expected: missing `window.amllParse`.

- [ ] **Step 3: Implement parser selection in `bridge.js`**

Implement exact format routing:

- `ttml` -> `parseTTML`;
- `lrc` -> `parseLrc`;
- `plain` -> one `LyricLine` whose `startTime` is 0 and `endTime` is
  `Infinity`;
- every line converted to the AMLL core `LyricLine` shape documented in the
  approved spec.

Catch parser exceptions and return the failure shape; never throw through the
Android bridge.

- [ ] **Step 4: Push GREEN and watch CI pass**

Push implementation and run the Cloud Test Loop. Expected: parser tests and
Task 1 asset verification pass.

- [ ] **Step 5: Commit**

```bash
git add tools/amll .github/workflows/ci.yml
git commit -m "feat: parse TTML and lyric formats with AMLL"
```

### Task 3: Kotlin bridge payload contract

**Files:**

- Create: `androidApp/src/main/kotlin/com/exodidio/tune/lyrics/AmllBridgeModels.kt`
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/lyrics/AmllBridgePayloadTest.kt`

**Interfaces:**

- Consumes: Task 2 JSON output.
- Produces:
  `internal sealed interface AmllBridgeResult { data class Ready(val lines: List<AmllBridgeLine>); data class Fallback(val reason: String) }`.
- Produces:
  `internal fun parseAmllBridgePayload(json: String): AmllBridgeResult`.
- Produces:
  `internal enum class AmllLyricFormat { Ttml, Lrc, Plain }`.

- [ ] **Step 1: Add failing payload tests**

Add `AmllBridgePayloadTest` cases named:

- `validReadyPayloadPreservesWordTimings`;
- `validReadyPayloadPreservesTranslationAndRomanization`;
- `missingLineOrWordTimingReturnsFallback`;
- `invalidJsonReturnsFallback`;
- `textEscapesRemainDataNotMarkup`.

Use serialization output strings, not mocks.

- [ ] **Step 2: Push RED and watch CI fail**

Push tests only. Expected compilation failure because `AmllBridgeResult` and
`parseAmllBridgePayload` do not exist.

- [ ] **Step 3: Implement payload models and parser**

Add millisecond `Long` fields for line/word start/end and nullable
`translatedLyric`, `romanLyric`, `isDuet`, and `isBG`. Reject negative
timestamps, `end < start`, duplicate/zero-length valid word spans, missing
line bounds, malformed JSON, or unknown required fields by returning
`Fallback`.

- [ ] **Step 4: Push GREEN and watch CI pass**

Push implementation and run the Cloud Test Loop. Expected: Task 3 tests plus
all prior tests pass.

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/exodidio/tune/lyrics/AmllBridgeModels.kt \
  androidApp/src/test/kotlin/com/exodidio/tune/lyrics/AmllBridgePayloadTest.kt \
  androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerLyricsPanel.kt
git commit -m "feat: validate AMLL bridge payloads"
```

### Task 4: Restricted WebView controller

**Files:**

- Create: `androidApp/src/main/kotlin/com/exodidio/tune/lyrics/AmllBridgeController.kt`
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/lyrics/AmllBridgeControllerTest.kt`
- Modify: `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerTest.kt`
- Modify: `tools/amll/bridge.js`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerLyricsPanel.kt`

**Interfaces:**

- Consumes: `parseAmllBridgePayload` and generated local assets.
- Produces:
  `internal class AmllBridgeController(private val clock: () -> Long = System::currentTimeMillis)` with:
  `fun load(trackId: String, content: String, format: AmllLyricFormat)`,
  `fun updatePosition(trackId: String, positionMs: Long)`,
  `fun requestSeek(trackId: String, positionMs: Long, requestId: Long)`,
  `fun setRomanizationEnabled(enabled: Boolean)`,
  `fun onBridgeMessage(json: String): AmllBridgeResult`,
  `fun dispose()`.
- Produces: Compose `AmllLyricsWebView` with the same track/content/position
  inputs and an `onFallback: (String) -> Unit` callback.

- [ ] **Step 1: Add failing controller tests**

Add these named tests:

- `loadForNewTrackClearsPreviousBridgeState`;
- `updatePositionForStaleTrackIsIgnored`;
- `requestSeekForOldRequestIsIgnored`;
- `readyPayloadIsForwardedOnce`;
- `fallbackPayloadDoesNotReachReadyState`;
- `romanizationEnabledIsForwardedToAmlL`;
- `disposeStopsFurtherMessages`.

Add `FullScreenPlayerTest.usesNativeLyricsWhenAmllBridgeFallsBack` to assert
that a forced fallback keeps the native `plain_lyrics_list` visible.

- [ ] **Step 2: Push RED and watch CI fail**

Push tests only. Expected compile failure because controller is absent.
For the Compose test, dispatch `emulator-smoke.yml` on the pushed commit and
watch it fail because forced fallback is not implemented.

- [ ] **Step 3: Implement JavaScript and Kotlin controllers**

In `bridge.js`, create a singleton player on local `index.html`, parse through
Task 2, serialize ready results through a fixed `TuneAmll` JSON envelope, and
ignore every remote URL/navigation request.

In Kotlin, configure WebView with:

- local `amll/index.html` asset URL only;
- JavaScript enabled but `file`/network navigation disabled;
- no remote content access;
- lifecycle teardown on panel disposal;
- stale track/request IDs dropped before forwarding.

Wire `FullScreenPlayerLyricsPanel` so `Ready` uses the WebView and `Fallback`
uses the existing native line list. Keep the current empty/loading states.
Forward the current romanization selection through
`setRomanizationEnabled`.

- [ ] **Step 4: Push GREEN and watch CI pass**

Push implementation and run the Cloud Test Loop. Expected: Task 4 and all
earlier tests pass. Dispatch and watch `emulator-smoke.yml` for the Compose
fallback test.

- [ ] **Step 5: Commit**

```bash
git add tools/amll/bridge.js \
  androidApp/src/main/kotlin/com/exodidio/tune/lyrics/AmllBridgeController.kt \
  androidApp/src/test/kotlin/com/exodidio/tune/lyrics/AmllBridgeControllerTest.kt \
  androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerLyricsPanel.kt
git commit -m "feat: host AMLL lyrics in a restricted WebView"
```

### Task 5: Romanization control restoration

**Files:**

- Create: `androidApp/src/test/kotlin/com/exodidio/tune/lyrics/RomanizationPreferenceMigrationTest.kt`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/lyrics/LyricsPreferences.kt`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/lyrics/RomanizationViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/MainActivity.kt`
- Modify: `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerTest.kt`

**Interfaces:**

- Consumes: Task 3 bridge lines containing `romanLyric`.
- Produces:
  `internal fun defaultRomanizationEnabled(stored: Boolean?): Boolean = stored ?: true`.
- Produces:
  `RomanizationViewModel.setPreferred(enabled: Boolean)` which updates
  activation without changing capability/allowed state.
- Extends `RomanizationUiState` with
  `amllRomanization: Boolean = false`.
- Produces: panel visibility formula:
  `romanizationAllowed && current && (romanization.supported || romanization.amllRomanization)`.

- [ ] **Step 1: Add failing preference and Compose tests**

Add `RomanizationPreferenceMigrationTest`:

- `missingPreferenceDefaultsRomanizationActive`;
- `existingFalsePreferenceIsPreserved`;
- `existingTruePreferenceIsPreserved`.

Add `FullScreenPlayerTest.romanizationToggleRemainsVisibleWhenPreferenceIsInactive`:
pass supported native lyrics with `romanizationAllowed = true` and active state
false, then assert `romanization_toggle` exists, is unselected, and clicking it
selects it and shows secondary lyrics.

- [ ] **Step 2: Push RED and watch CI fail**

Push only tests. Expected failure: default migration function and active
false button behavior are not implemented.
Dispatch and watch `emulator-smoke.yml` for the Compose assertion.

- [ ] **Step 3: Implement preference separation**

Read missing `RomanizationKey` as `true`; preserve explicit stored `false` or
`true`. Always allow capability inspection in `MainActivity`. Wire the settings
switch to `setPreferred`, not `setAllowed(false)`.

When AMLL supplies `romanLyric`, pass those lines to the existing native
romanization input only for fallback conversion; display TTML romanization
directly when active. Never overwrite TTML text with native output.

- [ ] **Step 4: Push GREEN and watch CI pass**

Push implementation and run the Cloud Test Loop. Expected: Task 5 and prior
tests pass. Dispatch and watch `emulator-smoke.yml` for the Compose test.

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/exodidio/tune/lyrics/LyricsPreferences.kt \
  androidApp/src/main/kotlin/com/exodidio/tune/lyrics/RomanizationViewModel.kt \
  androidApp/src/main/kotlin/com/exodidio/tune/MainActivity.kt \
  androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerTest.kt \
  androidApp/src/test/kotlin/com/exodidio/tune/lyrics/RomanizationPreferenceMigrationTest.kt
git commit -m "feat: restore romanization control in lyrics"
```

### Task 6: Remove stale fullscreen behavior and tests

**Files:**

- Modify: `androidApp/src/androidTest/kotlin/com/exodidio/tune/AppNavigationTest.kt`
- Modify: `androidApp/src/androidTest/kotlin/com/exodidio/tune/ui/navigation/AdaptiveScreenshotsTest.kt`
- Modify: `androidApp/src/test/kotlin/com/exodidio/tune/MainViewModelTest.kt`
- Modify: `androidApp/src/test/kotlin/com/exodidio/tune/SettingsMusicSyncNavigationTest.kt`
- Modify: any remaining `fullscreenArtwork`/`fullscreenLyrics` references found
  by the repository search.

**Interfaces:**

- Consumes: commit `76438e1`, which removed product behavior.
- Produces: repository-wide absence of `fullscreenArtwork`, `fullscreenLyrics`,
  `SetFullscreenArtwork`, and `SetFullscreenLyrics`.

- [ ] **Step 1: Add failing absence assertion**

Add a GitHub CI shell assertion:

```bash
! rg -n 'fullscreenArtwork|fullscreenLyrics|SetFullscreenArtwork|SetFullscreenLyrics' \
  androidApp/src
```

- [ ] **Step 2: Push RED and watch CI fail**

Push only the assertion. Expected failure from existing test stubs and
`fullscreenArtworkMemoryKey` references.

- [ ] **Step 3: Remove stale test-only behavior**

Delete obsolete assertions, fake state fields, and screenshot settings. Keep
artwork caching functions whose names do not expose a user-facing fullscreen
option; rename only where the assertion would otherwise match.

- [ ] **Step 4: Push GREEN and watch CI pass**

Push cleanup and run the Cloud Test Loop. Expected: absence assertion and all
prior tests pass.

- [ ] **Step 5: Commit**

```bash
git add -u androidApp/src
git commit -m "test: remove stale fullscreen artwork and lyrics references"
```

### Task 7: Provenance and release publication

**Files:**

- Modify: `NOTICE`
- Modify: `.github/workflows/pre-release-apks.yml`
- Create: `tools/release/verify-release-input.mjs`
- Create: `tools/release/test-verify-release-input.mjs`

**Interfaces:**

- Consumes: green PR CI and signed universal APK artifact.
- Produces: GitHub Actions secret `TUNE_BETAS_TOKEN` in `Exodi-dio/tune`.
- Produces: pre-release `v1.2.0-beta.3` in `Exodi-dio/tune-betas` with asset
  `Tune-v1.2.0-beta.apk`.

- [ ] **Step 1: Add failing release verification tests**

Add `tools/release/test-verify-release-input.mjs` tests for:

- matching artifact and manifest SHA-256 -> exit 0;
- mismatched checksum -> exit 4;
- missing APK -> exit 3.

Push to PR CI and watch the missing test fail.

- [ ] **Step 2: Implement provenance and checksum verification**

Add both upstream repositories, exact commits, AGPL-3.0-only licenses, source
URLs, and bundled asset statement to `NOTICE`. Implement the checksum verifier
and call it in CI after downloading the artifact.

- [ ] **Step 3: Push GREEN for provenance**

Push and run the Cloud Test Loop. Expected checksum tests and all prior tests
pass.

- [ ] **Step 4: Configure cross-repository release credential**

Without printing the token:

```bash
gh auth token | gh secret set TUNE_BETAS_TOKEN --repo Exodi-dio/tune
```

Verify only presence:

```bash
gh secret list --repo Exodi-dio/tune | rg TUNE_BETAS_TOKEN
```

- [ ] **Step 5: Extend the Tune pre-release workflow**

After upload-artifact:

- validate the tag is `v1.2.0-beta.3`;
- reject an existing `v1.2.0-beta.3` in `Exodi-dio/tune-betas`;
- run `scripts/build-amll-assets.sh` before tests;
- download this run's `tune-prerelease-apks` artifact;
- run `tools/release/verify-release-input.mjs` against the artifact checksum;
- publish from this same job with
  `GH_TOKEN="$TUNE_BETAS_TOKEN" gh release create v1.2.0-beta.3 --repo Exodi-dio/tune-betas ... "$APK_PATH"`;
- download the published asset in the same job and require its SHA-256 to
  equal the workflow artifact checksum.

The release title is `Tune v1.2.0-beta.3`; asset name is
`Tune-v1.2.0-beta.apk`.

- [ ] **Step 6: Push and watch complete CI**

Push all release changes, run the Cloud Test Loop, and require green shared,
Android, asset, parser, bridge, and checksum tests.

- [ ] **Step 7: Merge the approved PR**

```bash
gh pr merge <pr-number> --repo Exodi-dio/tune --merge
```

Wait for the post-merge `ci` run to pass.

- [ ] **Step 8: Trigger and watch the release workflow**

```bash
gh workflow run pre-release-apks.yml \
  --repo Exodi-dio/tune \
  --ref main \
  -f tag=v1.2.0-beta.3
RUN_ID="$(gh run list --repo Exodi-dio/tune --workflow pre-release-apks.yml \
  --limit 1 --json databaseId --jq '.[0].databaseId')"
gh run watch "$RUN_ID" --repo Exodi-dio/tune --exit-status
```

Expected: exactly one signed universal APK artifact, matching checksum, and
green publication job.

- [ ] **Step 9: Verify the published release**

```bash
gh release view v1.2.0-beta.3 --repo Exodi-dio/tune-betas \
  --json tagName,name,isPrerelease,assets
gh release download v1.2.0-beta.3 --repo Exodi-dio/tune-betas \
  --pattern Tune-v1.2.0-beta.apk --dir "$RUNNER_TEMP/release-check"
sha256sum "$RUNNER_TEMP/release-check/Tune-v1.2.0-beta.apk"
```

Expected: prerelease exists, exactly one matching APK asset, and checksum
equals the workflow artifact.

- [ ] **Step 10: Final branch review**

Run through `gh`:

- `gh pr diff <pr-number>`;
- `gh run list` for the final main and release runs;
- `gh release view`;
- repository search for stale fullscreen symbols and local-build instructions.

Do not claim completion unless every listed command succeeded.

## Completion Report Requirements

Report:

- merged commit SHA;
- green PR and post-merge workflow IDs;
- release workflow ID;
- published release URL;
- artifact and release SHA-256 values;
- exact AMLL and TTML-tool source commits;
- any test failure observed and resolved.
