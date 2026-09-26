# Sub-project 1 — BitChord Player Shell Port (Now Playing + Queue + Lyrics)

Date: 2026-09-26. Status: design approved by product owner (sections 1-6; section 7 tag proposed).
Series: BitChord-to-Tune beta. Sub-project 1 of 4 (next: Home; then Artist + online metadata; then Playlist + custom covers).
Source: BitChord (GPL-3.0) — reimplement adapted to Tune models, preserve license notices. No verbatim code pasting.

## 1. Intent

Give Tune the buttery player feel of the reference while keeping Tune's identity:
Tune theme tokens, Tune font, Tune blur backdrop. Success: the player area
(sheet, gestures, queue, lyrics) feels like the reference; all data stays
local-library driven; release-signed build stays smooth on 2-3 GB budget phones.

## 2. Non-goals

No Spotify Canvas, no party/listening-together, no output-switcher beyond what
Tune has, no word-level lyric effects, no changes to playback engine semantics
(queue shuffle/repeat rules unchanged), no other screens.

## 3. Architecture

- Port the reference player shell (bottom sheet, collapsing deck, swipe/fling
  gestures, panel switching) as NEW files alongside the current player.
- Rewire every data read to `PlaybackModel` / `PlaybackQueueSnapshot` /
  `queueTracks: List<LibraryTrack>`. No InnerTube models cross the boundary.
- Old player files (`FullScreenPlayer*.kt`, `FullScreenPlayerQueuePanel.kt`,
  `FullScreenPlayerLyricsPanel.kt`, `MiniPlayer.kt` if chrome changes) stay
  until the port is green, then are deleted. No half-migrated states.
- Full-bleed artwork: Settings toggle, default on, phone-narrow only.

## 4. Now playing

Order: credits + scrubber + transport + volume + lyrics/output/queue row.
Artwork sleeve with pause-shrink and collapse-to-thumbnail when panels open.
Controls auto-hide in lyrics view after 5s idle unless interacting.
`keepScreenOn` while lyrics open. Back closes panel before dismissing player.
Last-open panel (lyrics/queue/none) restored per track.

## 5. Queue

Sections in order: Now playing (current row) → Next in queue (user,
reorderable, clearable) → Next from album/context → Autoplay (∞ header +
toggle). Tap-to-jump, drag-reorder within own section, remove, swipe actions
carried over from the current panel.
Autoplay (offline definition): when the user queue drains toward empty,
append up to 10 tracks picked by same-artist, then same-genre, then mood-radio
mix, excluding recently played. Repeat-one disables it. Toggle in header.

## 6. Lyrics

Line-level highlight + auto-scroll with lead time + tap-to-seek + browse mode
that suspends follow, in Tune's font and type scale. Sources unchanged
(existing providers + manual finder flow). No word-sweep effects.

## 7. Verification (GitHub cloud only, no local hardware ever)

- Unit tests: queue split (sections/order), autoplay pick (priority, cap,
  exclusions, repeat-one disable), panel state restore.
- `assembleDevDebug` + `assembleDevRelease` green; release-signed APK.
- Emulator run if the runner heals; otherwise the Reddit beta is the gate.

## 8. Release (proposed, confirm at build time)

Signed pre-release tag `v0.3.0-beta.1` from green `main`, APK asset uploaded,
link returned.

## 9. Risks

- Gesture thresholds tuned for the reference may feel off on Tune's content;
  keep them as constants for beta tuning.
- Autoplay mix quality is heuristic; cap and toggle bound the downside.
- Coroutine/ticker patterns from the reference must be re-expressed in Tune's
  playback plumbing, not pasted.

