# Tune v0.2.0-debug.1 — Performance, Settings sync relocation, desktop strip, sponsor removal

Date: 2026-09-25. Status: design approved by product owner (all 7 sections).

## 1. Intent

The app works but is extremely laggy/choppy on low-end hardware even with
"reduce transparency" enabled; desktop-sync UI remnants remain; local music
scanning lives on the Library screen instead of Settings; the About screen
carries a Sponsor Tune block. Ship a prerelease that fixes all of the above
for 2-3 GB budget phones, built and verified entirely on GitHub cloud.

Success criteria:

- Cold start, Home render, and list scrolling are smooth on a 2 GB-RAM
  emulator profile; no main-thread bitmap decode remains anywhere.
- Peak bitmap-cache footprint is byte-budgeted, not count-budgeted.
- "Scan local files" is reachable only from Settings; Library is browse-only.
- No user-facing copy references a desktop; no dead desktop code, deps, or
  strings remain (per the audited lists).
- About screen has no sponsor block; GitHub and License entries remain.
- CI (unit tests, assemble, snapshot) plus a new low-RAM emulator smoke test
  are green; prerelease `v0.2.0-debug.1` is published with a working APK link.

## 2. Non-goals

No new features, no visual redesign, no release (non-debug) build changes,
no new subsystems. Haze blur and the current look stay.

## 3. Performance work

Target profile: 2-3 GB RAM budget phones, minSdk 31, arm64.

1. Artwork cache becomes byte-sized (`LruCache` sized from
   `ActivityManager.memoryClass`, ~1/8), keyed by path only so decoded
   bitmaps are reused across target sizes; add in-flight request dedup.
   (`ui/.../TrackRow.kt` artwork cache.)
2. `CreatePlaylistDialog` newly-picked artwork decodes off the main thread
   with `inSampleSize` into the shared cache (today: unbounded
   full-resolution decode during composition).
3. Second uncached `ARGB_8888` decoder (`FullScreenPlayerArtwork.kt`) routes
   through the shared sampled cache.
4. Every `metadataObject()` / `isFavorite()` call in composition is wrapped in
   `remember(track.metadataJson)`; `App.kt` queue favorite lookup is hoisted
   into `derivedStateOf`. Long term the projection should carry `isFavorite`
   as a column; out of scope for this release.
5. `LibrarySearchContent` and `LibraryAlbumsContent` per-item full-library
   scans are hoisted to `remember(uiState)` prebuilt maps; the O(N^2)
   `indexOfFirst` divider lookup is removed.
6. `FloatingNavigationBar` caches its indicator `Path` (invalidated only on
   size/offset change) and draws through a single `drawWithCache` layer;
   the double masked draw is simplified to one layer.
7. Marquee, shimmer, and queue playing-indicator infinite animations run only
   while actually animating (marquee only when text overflows).
8. The 200 ms playback ticker stays (no behavior change); per-tick work is
   eliminated by fixes 4-5 plus `@Immutable`/`@Stable` on the navigation
   aggregate models so Compose skipping works at the root.
9. `reduceTransparency=true` collapses `DetailHero` gradients/overlays and all
   Haze blur surfaces to opaque fills (detail pages today ignore the setting);
   the 30 dp drag blur in playlist reorder is replaced by an opaque drag
   affordance.
10. ProGuard gains `-assumenosideeffects` for `android.util.Log` (17
    interpolated `PlaybackService` calls evaluate in release today);
    `compose.uiToolingPreview` moves to `debugImplementation`;
    `contentType`+`key` are added to the nine lists missing them
    (`HomeContent`, `LibrarySearchContent`, `AlbumDetailsContent`,
    `PlaylistDetailsContent`, `TrackInfoContent`, `TrackContextMenu`,
    `FindLyricsContent`, `InsightContent`).
11. Preserve existing optimizations: per-page gated ViewModel collection,
    one-page-at-a-time rendering, `LibraryVirtualList` key+contentType,
    memoized alphabetical index, `RGB_565` thumbnails, off-main dominant
    color, off-main sorting, R8+shrinking.

## 4. Sync relocation to Settings

- New `AppStackPage.SettingsMusicSync` (+ `AppNavigationMetadata` title),
  new Settings root entry between Integration and About, new
  `MusicSyncContent` screen hosting the permission/scan logic moved out of
  `LibraryContent` (scanner setup, permission launcher, `startScan`).
- Library becomes browse-only; its action list loses the scan item.
- Empty-state copy rewritten to point at Settings scan instead of a desktop:
  `library_empty_title/description`, `playlists_empty_description`,
  `playlist_sync_failed` (+ all 13 locale files).
- Tests: update `LibraryContentTest`, add Settings-entry navigation test.

## 5. Desktop strip (remainder)

- Delete unused Camera/MLKit/MQTT/eddsa deps + version-catalog entries +
  MLKit/Firebase ProGuard keeps.
- Delete the 42 dead `sync_*`/`settings_sync` strings in all 13 locales.
- Delete dead `formatSyncStorageMegabytes`, `DesktopWindows`/`Sync` symbols.
- Lyrics default source becomes `AutoFetch`; remove the Desktop option from
  the selector (`LyricsPreferences`, `IntegrationContent`, `MainActivity`
  desktop-lyrics wiring).
- Remove the dead Insight Desktop/Other source filter and its `sharedLogic`
  pairing imports.
- Fix `AppNavigationTest` compile errors (references to deleted `SyncUiState`
  etc.) so `androidTest` compiles again; rename the sync-named home test.

## 6. Sponsor removal

- Delete the `LabeledCard` sponsor block + 4 URLs + orphaned `LabeledCard`
  import in `AboutContent.kt` (GitHub + License entries stay).
- Delete the 5 `about_sponsor*` strings in all 13 locale files.
- Delete the sponsor navigation test; keep the GitHub-link test.

## 7. Verification (GitHub cloud only, no local hardware ever)

- Existing gates stay green: `sharedLogic:testAndroidHostTest`,
  `androidApp:testDevDebugUnitTest`, `assembleDevDebug`, branding gate, APK
  snapshot (dictionaries still present).
- New: `connectedCheck` smoke test on a 2 GB-RAM emulator profile —
  cold start, Home render, open scan screen; plus a unit test pinning the
  artwork cache byte budget.
- Regression discipline per fix: red-green where a failing assertion is
  expressible (cache budget, remember-wrapping via recomposition-count or
  parse-count tests); review pass on UI-only changes.

## 8. Release

Prerelease tag `v0.2.0-debug.1` from the green commit, APK asset uploaded,
link returned — same flow as `v0.1.0-debug.1`.

## 9. Risks

- Emulator macro/connected runs on GitHub runners can flake; mitigate with
  retries and a smoke-level (not jank-metric) gate.
- Locale-string edits across 13 files are mechanical but wide; the plan
  batches them per file group with a resource-lint gate.

