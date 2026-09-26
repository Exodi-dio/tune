# SDD ledger — plans: docs/superpowers/plans/2026-09-26-tune-player-shell.md, 2026-09-26-tune-queue-lyrics.md
# Spec: docs/superpowers/specs/2026-09-26-player-port-design.md
# Integration branch: impl/v0.3-player (from main 9b2f61f). Task branches: impl/v0.3-player/<batch>.
# Harness notes: `task` tool has no model parameter — all subagents at session default.
# Local clones (/root/tune stale pre-v0.2.0, /root/bitchord) are read-only reference; code lands via `gh api`.
# Review packages via `gh api compare` → /tmp/opencode/sdd-v03/<track>/review-*.diff. Ledger also in git at .superpowers/sdd-v03/progress.md (ci paths-ignored).

## Standing rulings (carried from v0.2.0 session)
- Ruling: task branches off integration head, fast-forward on clean review; main merge + release pre-authorized by approved plan. Cost if wrong: visible rework in git.
- Ruling: sequential implementation only, linear history. Cost if wrong: rebase + re-verify.
- Ruling: TDD history = RED-evidence commit + ONE final conventional commit per task. Cost if wrong: noisier history; run IDs preserve evidence.
- Ruling: controller `impl/*` branch scheme overrides brief advisory names. Cost if wrong: none.
- Ruling: ci.yml push=[main] only → verify via `POST actions/workflows/366658414/dispatches -F ref=<branch>` + `gh run watch`. New workflow files do NOT auto-register. Cost if wrong: wasted cycles.
- Ruling: JUnit org.junit only (no kotlin.test in module); androidTest review-gated (CI doesn't compile it). Cost if wrong: uncompilable tests.
- Ruling: no force-push anywhere; abandon-and-replace branches instead. Cost if wrong: extra branches (deleted at end).

## Preflight conflict scan
| Pair | Producer vs consumer | Finding |
|---|---|---|
| shell T1 vs queue T1-T5 | T1 produces mount contract (PlayerShellPanel, PlayerShellQueueMount/PlayerShellLyricsMount, PlayerShell); queue plan amended to consume it | Consistent by amendment. No action. |
| queue T5 vs shell T6 | BOTH delete FullScreenPlayerQueuePanel/LyricsPanel | Ruling: combined final batch (queue T5 + shell T6, one dispatch, both briefs). Cost if wrong: double-delete conflict. |
| shell T3 vs tree | Consumes TuneMarqueeText/TuneTrackSlider/FullScreenTransportButton/FullScreenControlSlot signatures presumed from exploration | Ruling: implementer verifies signatures at ref main before writing; adapts minimally with disclosure if drifted. |
| shell T4 vs tree | Consumes rememberFullscreenArtwork/FullScreenArtwork/liquidGlassBackground presumed | Same ruling as above. |
| shell T1 vs tree | References FullScreenPlayerPanel/FullScreenQueuePanel/FullScreenPlayerLyricsPanel names presumed | Same ruling. |
| queue T4 vs tree | Consumes FullScreenPlayerLyricsPanel helpers (parsePlayerLyrics, PlayerLyricLine, blur/distance helpers) presumed | Same ruling. |
| queue T2 vs tree | Consumes metadataObject()/RepeatMode/MoodRadio read-only | Low risk; same ruling. |
| shell T6 vs queue T1-T4 | T6 deletes old panels queue content still references until queue T5 | Ordering: queue T5 lands WITH shell T6 in final batch. Covered. |

## Execution order
shell T1 → queue T1+T2 → queue T3+T4 → shell T2+T3 → shell T4+T5 → (queue T5 + shell T6)

## Progress
- Setup: workspace /tmp/opencode/sdd-v03/{shell,queue}; plans fetched; impl/v0.3-player created at 9b2f61f.
## Branch-name ruling
- Ruling: refs like impl/v0.3-player/task-N are uncreatable (422) while impl/v0.3-player exists as a branch. All task branches use flat names impl/v0.3-player-<batch>. Cost if wrong: none.
## Shell Task 1 completion
- Shell Task 1: complete (commits e44a202..5802758, review clean). Contract signatures verbatim for sibling use.
## Queue batch 1 (T1+T2) completion
- Ruling: T1 extra fix commit waived (forced by brief self-contradiction + no-force-push; behavior preserved per review).
- Fix round 1/5: genreTokens guard ADDRESSED, no breakage (re-review PASS).
- Queue batch 1: complete (commits 45114dc..c087595c via queue-1 + fix, review clean after 1 fix round).
## Queue batch 2 (T3+T4) completion
- Fix round 1/5: F1 tap-to-seek, F2 string resource, F3 history — ALL ADDRESSED, no breakage.
- Queue batch 2: complete (review clean after 1 fix round).
## Shell batch 2 (T2+T3) completion
- Shell batch 2: complete (commits 8ca80d5..8205a63d, review clean). Carries to Task 5 batch: seek→lyrics pending-seek sync wiring + integration test; crossfade threading (outgoing/incoming/progress); extract sleeve constants; reuse shared time formatter.
## Shell batch 3 (T4+T5) completion
- Shell batch 3: complete (commits 3cc07d1..fa64fa22, review clean). Carries to FINAL batch: confirmSeek wiring; backdrop/back-handler insertion; full-bleed toggle wiring (PlayerPreferences→isFullBleedEnabled→backdrop).
