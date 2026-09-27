<p align="center"><img src="brand/icon-source.png" width="160" alt="Tune app icon" /></p>

# Tune

[![Latest release](https://img.shields.io/github/v/release/Exodi-dio/tune)](https://github.com/Exodi-dio/tune/releases/latest)
[![License: GPL-3.0](https://img.shields.io/github/license/Exodi-dio/tune)](LICENSE)
[![Android 12+](https://img.shields.io/badge/platform-Android%2012%2B-3DDC84?logo=android&logoColor=white)](https://github.com/Exodi-dio/tune/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/Exodi-dio/tune/ci.yml?branch=main)](.github/workflows)

Tune is an Apple Music-inspired local music player for Android.
It plays the music stored on your device. There is no account, no streaming
service, no sync, and no advertising or analytics.

## Screenshots

| Now playing | Queue |
| ----------- | ----- |
| <img src="assets/screenshots/now-playing.jpg" width="300" alt="Now playing" /> | <img src="assets/screenshots/queue.jpg" width="300" alt="Queue" /> |

| Lyrics | Library |
| ------ | ------- |
| <img src="assets/screenshots/lyrics.jpg" width="300" alt="Synced lyrics" /> | <img src="assets/screenshots/library.jpg" width="300" alt="Library" /> |

| Albums | Album details |
| ------ | ------------- |
| <img src="assets/screenshots/albums.jpg" width="300" alt="Albums" /> | <img src="assets/screenshots/album-details.jpg" width="300" alt="Album details" /> |

| Home |
| ---- |
| <img src="assets/screenshots/home.jpg" width="300" alt="Home" /> |

## Getting the app

Download the latest release from the
[Releases page](https://github.com/Exodi-dio/tune/releases/latest).
Four builds are published for every version — pick the one that fits your
device, or take the universal build if you are unsure:

| File | For |
| ---- | --- |
| `Tune-v1.0.0.apk` | Universal — every device |
| `Tune-v1.0.0_arm64-v8a.apk` | Modern phones (most devices) |
| `Tune-v1.0.0_armeabi-v7a.apk` | Older 32-bit phones |
| `Tune-v1.0.0_x86_64.apk` | Emulators, tablets, Chromebooks |

The app can also update itself: Settings → About → Check for updates
downloads the build that matches your device and opens the installer.
Requires Android 12 or newer.

## Library 📚

- Scans the audio files on your device (Settings → Music sync) with an
  explicit permission request; nothing leaves the phone.
- Browse and search tracks, artists, albums, genres, composers, and
  playlists, plus a recently-added section and detail pages for each.
- Album artwork resolves from embedded file art, falling back to track
  art where none is embedded. Artist and composer artwork resolves from
  scanned art, and artist pictures can be replaced with your own.

## Playback ▶️

- Play queue with play next, append, remove, reorder, and clear, plus
  shuffle and repeat (off, one, all).
- Favorites with a dedicated Favorites playlist.
- Mini player and fullscreen player with seek, volume, and quality badge.

## Audio 🎧

- Decoding is handled by an embedded FFmpeg build, so common local
  formats play: FLAC, MP3, M4A/AAC, OGG Vorbis, Opus, WAV, AIFF, APE,
  WavPack, DSF/DFF, WMA, and others.
- 10-band equalizer with presets, volume normalization (track or album,
  LUFS target, anti-clip), crossfade up to 12 seconds, and automatic
  gapless transitions.
- Lossy / Lossless / Hi-Res / DSD quality badges, toggleable.

## Lyrics 📝

- Synced and plain lyrics fetched from lrclib and Kugou, both toggleable.
- Built-in finder: when several songs share a title, search results let
  you pick the correct lyrics for your track.
- Offline romanization for Chinese and Korean lyrics.

## Playlists and artists 🎨

- Playlists can be created, renamed, reordered, and deleted, with custom
  cover art picked from your device (track-art mosaic otherwise).
- Artists can be given custom pictures the same way.
- Favorites sync to Last.fm "loved tracks" when Last.fm is connected.

## Appearance ✨

- System, light, and dark themes, edge-to-edge layout, and a
  reduce-transparency option.
- Interface available in 12 languages: German, English, Spanish, French,
  Italian, Japanese, Korean, Portuguese, Russian, Thai, Vietnamese, and
  Chinese.

## Integrations 🔌

- Last.fm: connect an account for scrobbling, now-playing updates, and
  loved tracks. This is the only third-party account in the app, and it
  is entirely optional.

## Offline behavior 📴

Everything local works without a network connection: library, playback,
queue, playlists, favorites, artwork edits, audio processing, cached
lyrics, and settings. Lyrics fetching, Last.fm, and update checks are
the only features that need the internet.

## Building 🛠️

All builds, tests, and verification run in GitHub Actions — nothing in
this project is built on a developer machine. See
[.github/workflows](.github/workflows) and [AGENTS.md](AGENTS.md).

## Acknowledgments 🙏

Tune's interface is built around the [Airmedy](https://github.com/misa198/airmedy)
design language by [misa198](https://github.com/misa198). Airmedy is
GPL-3.0 software; further provenance is recorded in [NOTICE](NOTICE).

## License 📄

GPL-3.0 — see [LICENSE](LICENSE).

