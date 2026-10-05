# AMLL Lyrics Beta 3 Design

## Goal

Ship a new Tune Android beta that replaces the current lyric presentation with
the AMLL `applemusic-like-lyrics` runtime, supports AMLL-compatible TTML,
keeps fullscreen artwork and fullscreen lyrics removed, restores the
romanization control in the lyrics panel, and publishes a signed APK as
`v1.2.0-beta.3` to `Exodi-dio/tune-betas`.

All coding, dependency installation, tests, compilation, and release actions
occur on GitHub. No local Gradle, emulator, Node, compiler, or test execution
is allowed for this project.

## Approved Scope

- Remove the fullscreen album-art and fullscreen-lyrics options completely.
  Their preferences, intents, state fields, settings controls, and presentation
  branches must not remain in the shipped player.
- Make AMLL the primary lyric parser and renderer:
  - Pin `amll-dev/applemusic-like-lyrics` at commit
    `86200dead453bb067e554e989110cbadca8d4756`.
  - Use `@applemusic-like-lyrics/ttml`, `@applemusic-like-lyrics/lyric`, and
    `@applemusic-like-lyrics/core`.
  - Preserve word-level timing, line timing, translations, transliterations,
    duet lines, background vocals, interlude behavior, and source seek requests.
- Use `amll-dev/amll-ttml-tool` as the TTML schema and editor-format reference:
  - Pin commit `d4953b351ae073c1447464790fff17aa9bc1d807`.
  - Accept sibling `.ttml` files with the same base name as the audio file,
    after an existing `.lrc` sibling.
  - Accept the AMLL TTML features represented by that tool: `p`/`span` timing,
    translations, transliterations, ruby, duet agents, background vocals, and
    metadata required for playback.
  - The desktop editor itself is not embedded. Its WASM UI remains a creation
    tool; Tune consumes and renders the TTML it produces.
- Restore the romanization button in the lyrics panel:
  - Existing Chinese and Korean offline romanization remains native.
  - The button appears whenever loaded lyrics support romanization or contain
    AMLL `romanLyric`/`x-roman` content.
  - The settings preference controls default activation, not button visibility.
  - TTML transliteration takes precedence over native conversion when present;
    otherwise the original `AndroidRomanizationEngine` converts supported text.
- Release as `v1.2.0-beta.3`, not a rewrite of `v1.2.0-beta.2`.

## Architecture

### Pinned AMLL assets

The GitHub release workflow checks out both pinned upstream commits, installs
Node/pnpm only inside the workflow, builds the three AMLL browser bundles and
styles, and copies them into Android assets before Gradle runs. The lockfile or
a generated integrity manifest records the exact dependency resolution.

The local `amll-ttml-tool` checkout is read only and is used to pin the TTML
fixtures and expected conversion behavior. Its WASM editor binary is not
packaged with Tune.

### AMLL bridge

`FullScreenPlayerLyricsPanel` keeps Tune's existing Compose panel shell and
romanization overlay, but the primary lyric list is an
`AndroidView` hosting a local WebView.

The bridge has four responsibilities:

1. Pass lyrics content and format hints to the AMLL parser.
2. Convert parser output into AMLL `LyricLine[]` before handing it to
   `LyricPlayer.setLyricLines`.
3. Forward playback position, seeking, resize, and lifecycle events.
4. Publish line/word changes needed by Tune's romanization overlay without
   copying lyric text into a second renderer.

The WebView runs local assets only, disables network access and JavaScript
file retrieval, and exposes a narrow JSON interface for the required events.
If WebView initialization or AMLL parsing fails, Tune displays the existing
plain lyric fallback instead of an empty panel.

### Lyrics normalization

Existing Tune lyric content remains the storage payload. The AMLL bridge
selects a parser by content:

- TTML XML uses `parseTTML`.
- `LRC` and enhanced LRC use AMLL's LRC parser.
- Unknown synchronized text falls back to line-level AMLL data.
- Plain text becomes one untimed AMLL line.

This keeps manual lyrics search, local file persistence, and remote provider
storage unchanged while replacing only parsing and presentation.

### Romanization state

`RomanizationViewModel` receives the AMLL line text only when the panel is
visible. It retains the existing generation, cancellation, and idle-release
logic.

The panel shows `RomanizationToggle` based on capability:

- a loaded AMLL line has `romanLyric`; or
- the native inspection reports Mandarin/Han or Korean support.

`lyricsSettings.romanizationEnabled` initializes and persists whether
romanized output is active. It never gates capability detection or hides the
button. Existing settings values are migrated without losing other lyrics
preferences.

## Error Handling

- Invalid or malformed TTML: log a warning, report parse failure to the
  bridge, and render available plain/provider lyrics.
- Empty lyrics: keep Tune's existing empty state.
- WebView unavailable or JavaScript disabled: use the native line list.
- Playback seek: ignore stale seeks by track ID and seek request ID; preserve
  current browsing and active-line behavior.
- Track changes: dispose the prior player, clear cached text, cancel
  romanization work, and initialize only the new track.
- Configuration changes: retain only playback/selection state that already
  survives Activity recreation; never retain lyric text in a singleton.
- Missing upstream bundle: fail the GitHub build before Gradle starts.

## Testing

Tests run only on GitHub Actions.

- Parser tests use fixtures from both pinned repositories and cover:
  word timings, line timings, translations, transliterations, ruby, duet,
  background vocals, malformed XML, and LRC/plain fallback.
- Unit tests cover bridge payload validation, stale track/request events, and
  preference migration/default activation.
- Compose tests verify:
  - the AMLL panel uses the native line list when WebView/AMLL initialization
    fails, and the existing empty state when lyrics are blank;
  - romanization appears for supported native lyrics;
  - TTML transliteration is shown without native conversion;
  - the settings switch no longer hides a supported control;
  - fullscreen artwork/lyrics controls and state are absent.
- The cloud workflow runs shared-logic and Android unit tests before release
  assembly. Release assembly uses one signed universal APK.

## Release

The final workflow runs on GitHub Actions with tag input
`v1.2.0-beta.3`:

1. Build pinned AMLL assets.
2. Run unit tests.
3. Assemble and validate exactly one signed universal prod APK.
4. Upload the workflow artifact for verification.
5. Publish the APK to `Exodi-dio/tune-betas` as a pre-release named
   `Tune v1.2.0-beta.3`, with `Tune-v1.2.0-beta.apk` as the asset.

The workflow must download or otherwise verify that same artifact before
publishing; it must never publish a stale APK already present in the
tune-betas repository.

The release notes list AMLL lyric rendering, TTML support, restored
romanization control, and removal of broken fullscreen artwork/lyrics options.
`NOTICE` records both upstream repositories, exact commits, licenses, and
source links.

## Success Criteria

- `v1.2.0-beta.3` exists on `Exodi-dio/tune-betas` with one universal APK.
- The release artifact and published asset have matching checksums.
- GitHub Actions tests and release build are green.
- No fullscreen artwork/lyrics preferences or controls remain.
- AMLL renders synced and word-level lyrics; sibling TTML loads successfully.
- The romanization button is visible for supported lyrics after a fresh install.
- No local build or test command is run while implementing this feature.
