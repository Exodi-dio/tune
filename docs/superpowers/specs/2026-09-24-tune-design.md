# Tune — design spec (2026-09-24)

Status: design approved (amended: name Tune). Awaiting spec review.

## 1. Goal

Rework airmedy's Android app into **Tune**: a standalone, local-only music
player. Ship an installable debug APK from GitHub Releases. The About
screen credits "Reworked by exodi_dio". Brand: red music-note icon
(owner-supplied), name Tune everywhere, in and out of the app.

Success criteria:

- `Exodi-dio/tune` public repo, GPLv3.
- CI green: FFmpeg arm64-v8a builds on runner,
  `:androidApp:assembleDevDebug` succeeds, unit tests pass.
- Release `v0.1.0-test` with an installable (debug-signed) APK.
- End-to-end local playback: scan → library → queue → play, all
  FFmpeg-supported formats, full metadata, synced lyrics in any language.
- Bitchord-style artist screen with transitions, no crashes.
- Zero Airmedy / PC-sync references in UI, strings, package, icons.

## 2. Non-goals (v0.1)

- iOS (frozen upstream; removed), desktop/Wails/Go, remote control.
- Release (Play-store) signing — debug APK only for now.
- Streaming sources, accounts, subscriptions, bios.
- Bundled SF Pro font files (Apple license) — type-scale values only.

## 3. Base & layout

- Repo root = airmedy `mobile/` subtree (androidApp/, sharedLogic/,
  gradle/, gradlew, settings).
- Drop `iosApp/` (frozen) and desktop-only docs. Keep `tools/`
  font/romanization assets that are in use.
- Fresh single import commit; provenance + GPLv3 attributions in NOTICE.

## 4. Identity & brand

- Display name Tune (all 12 locales), applicationId `com.exodidio.tune`
  (+`.dev` flavor), versionName 0.1.0.
- Red accent family matching the icon; dark/light themes + artwork
  palette retained.
- Icon: placeholder adaptive vector first. Owner uploads the real PNG to
  `brand/icon-source.png` (via GitHub web); the `icon.yml` workflow keys
  out the black background to transparent and generates all mipmap
  densities + adaptive foreground/background/monochrome + about drawable,
  committing to a branch.

## 5. Remove PC sync

Delete: `pairing/` adapters, `sync/` Android adapters (puller, service,
playlist reconciliation, artwork staging), `SyncViewModel`,
`SyncContent`, `SyncScannerContent`; the `dataSync` manifest service;
HiveMQ/mDNS/QR-scan deps (verify each usage before dropping);
sync/pairing strings in all locales; sharedLogic pairing/sync protocol
files. Listening stats become local-only.

## 6. Local library (new, biggest item)

- `MediaStore` scanner with `READ_MEDIA_AUDIO` permission flow
  (minSdk 31): query audio media, observe changes, background import with
  progress UI in the nav slot the sync screen occupied.
- Metadata: `MediaMetadataRetriever` primary (title/artist/album/genre/
  year/track/duration/embedded art); existing FFmpeg JNI as fallback for
  exotic formats; map format/codec/bit-depth/sample-rate to the existing
  quality labels.
- Artwork: embedded extraction to disk cache; fallback art otherwise.
- Storage: new `LibraryDatabase` v1 (tracks, artists, albums, local
  playlists). Fresh install, no migration from the sync schema.

## 7. Playback (unchanged core)

FFmpeg + AAudio JNI stack as-is; queue/playback wired to the local Room
data. Keep EQ/normalization/gapless/crossfade prefs, foreground service,
notification.

## 8. Lyrics (all languages)

Keep local `.lrc` + embedded + LRCLIB + KuGou auto-fetch + manual
search (none are English-only; verify no language filter in matching
code). Provider toggles stay in UI. Romanization unchanged (opt-in).

## 9. Artist screen (Bitchord port, local data)

New artist screen modeled on Bitchord `DetailScreen` artist branch,
bound to local artist data:

- Keep: parallax collapsing header, artwork-palette crossfade, Haze
  glass + scrims, `AnimatedContent` list↔grid, top-songs rail
  (4-per-column), discography shelves + show-all grid, in-list search,
  skeletons.
- Adapt: stats chips → track/album counts + duration; actions → Play-all
  + Shuffle (no Subscribe); bio section → "Appears on" shelf; port theme
  tokens + glass components with existing fonts.
- All artist rows route to the new screen; back stack correct.

## 10. About (new)

Tune hero (new icon), "Reworked by exodi_dio", version, GPLv3 + source
links. CI grep-gate fails on `airmedy|misa198` outside NOTICE.

## 11. Cloud pipeline (permanent)

- `ci.yml` (push/PR): JDK 21 + Android SDK + NDK + CMake, cached FFmpeg
  arm64-v8a build, `:sharedLogic:testAndroidHostTest`,
  `:androidApp:assembleDevDebug`, branding grep-gate, debug APK artifact.
- `release.yml` (tag `v*`): rebuild, `aapt dump badging` verify (label
  Tune, package `com.exodidio.tune`), publish APK via release action.
- `icon.yml` (on `brand/icon-source.png` change): generate icon set.

## 12. Rollout (each step green on CI)

1. Base import + CI; baseline debug APK proves the pipeline.
2. Identity rename + placeholder icon.
3. Sync removal (compiles; library empty-state).
4. Local library + playback wiring.
5. Lyrics verification + About + grep-gate.
6. Artist screen port.
7. Tag `v0.1.0-test` → release APK → link.

## 13. Risks

- Runner FFmpeg build time (cached after first).
- Haze/Compose BOM alignment for ported components.
- MediaStore permission UX (Android 12–16).
- No device in CI: UI verified on-device by owner.
