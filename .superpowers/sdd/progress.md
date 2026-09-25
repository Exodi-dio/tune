# SDD ledger — plans: docs/superpowers/plans/2026-09-25-tune-performance.md, 2026-09-25-tune-settings-sync.md, 2026-09-25-tune-strip-sponsor-release.md
# Spec: docs/superpowers/specs/2026-09-25-performance-settings-sync-design.md
# Integration branch: impl/v0.2.0 (from main ea889f9). Task branches: impl/<track>/task-N.
# Harness notes: `task` tool has no model parameter — all subagents run at session default.
# Local clone /root/tune is stale read-only reference; all code lands via `gh api` git-database.
# Review packages built via `gh api compare` (script /tmp/opencode/sdd/compare-package.sh), not review-package (no local objects).

## Environment rulings
- Ruling: implement on task branches off impl/v0.2.0, fast-forward impl/v0.2.0 per clean task, final whole-branch review, then fast-forward main + release — main merge/release pre-authorized by approved plan Section 8 and the "begin next pre-release build" directive. Cost if wrong: rework visible in git history; release only fires on green main CI.
- Ruling: sequential implementation only (no parallel implementers); each task branch starts at current impl/v0.2.0 head, so history stays linear and cross-task file overlaps cannot conflict. Cost if wrong: rebase and re-verify.

## Preflight conflict scan (shared files / interfaces across tasks)
| Pair | Producer vs consumer | Finding |
|---|---|---|
| perf T1 vs perf T2,T3 | T1 produces ArtworkThumbnailCache API; T2/T3 consume verbatim | Plan text consistent (names match). No action. |
| perf T8 vs strip T5 | T8 annotates AppDestinationModels; T5 deletes onSourceSelected+import there | Different lines, sequential plans (perf first). Ruling: strip T5 implementer branches from then-current head; no merge conflict by construction. |
| perf T9+T10 vs strip T5 | T9/T10 touch PlaylistDetailsContent/InsightContent; T5 touches InsightContent too | Different regions, sequential. Same ruling as above. |
| perf T4 (App.kt favorite) vs strip T4 | Strip T4 touches only string value at App.kt display site, no App.kt code edit | No overlap. |
| settings T1 vs strip T6,T7 | Both modify AppNavigationTest (T1 appends music-sync test; T6/T7 delete sync/sponsor tests) | Different regions; settings plan runs before strip plan. Ruling: strip implementers branch from then-current head. |
| settings T1 (MaterialSymbols.Sync use) vs strip T3 | Strip draft deleted Sync; settings needs it live | Ruling (already applied to committed plan): strip T3 keeps `MaterialSymbols.Sync`, deletes only DesktopWindows + formatter. |
| settings T4 (empty-state copy) vs strip T2 (dead strings) | T4 rewrites library_empty_*/playlists_empty_description/playlist_sync_failed; T2 deletes 43 sync_* keys | Key lists disjoint (strip T2 explicitly excludes T4's keys). Ruling: no overlap; grep gates must not match each other's keys. |
| strip T4 (lyrics_source_desktop delete) vs settings plan | Settings plan never references that key | No overlap. |
| settings T2 (MusicSyncContent create) vs strip T6 (AppNavigationTest edits) | T6 compiles androidTest incl. new MusicSyncContentTest | Sequential (settings first). Fine. |
| Self-check per plan | Tests specified match code specified; TDD red-green expressible as CI unit-test jobs; UI-only changes use quoted-line review + assemble | Clean. One deviation: settings T3 Step 2 expects CI PASS while on-device red — plan states this explicitly. Accepted. |

## Stall-proofing + re-batch rulings
- Ruling: ledger lives in git at `.superpowers/sdd/progress.md` on impl/v0.2.0 (paths-ignored by ci, no builds triggered), updated at every merge. A stalled session resumes with zero loss: read ledger + `git log`. Cost if wrong: one extra docs commit per task.
- Ruling: re-batch remaining 20 tasks into 8 batch dispatches (perf T3+T4, T5+T6, T7+T8+T9, T10; settings T1-T4; strip T1-T3+T7; strip T4+T5; strip T6 then T8 release). Same gates per batch (brief = spec, RED→GREEN via dispatched CI, one review per batch). Cost if wrong: larger fix loops; batches are cohesive so risk is contained.
## Task 1 rulings + completion
- Ruling: 2-commit history accepted — plan Steps 2+5 inherently produce a RED-evidence push plus the final perf: commit; one-commit rule binds the final conventional commit only. Cost if wrong: noisier history; run IDs 36171251211/36171602192 preserve evidence.
- Ruling: branch impl/perf/task-1 accepted — controller integration-branch scheme overrides brief advisory name; downstream consumes SHAs/APIs. Cost if wrong: none.
- Verified: RED failure on 1e61796, GREEN success on 74f9d4c; ci.yml push=[main] only so all task branches verify via workflow_dispatch; 74f9d4c parent chain 1e61796->ea889f9; unit tests are JUnit (libs.junit, no kotlin-test) so org.junit adaptation was correct.
- Task 1: minor (deferred): claimInFlight no-retry null + cancellation skips releaseInFlight (plan-mandated, rare, non-data-loss).
- Task 1: complete (commits ea889f9..74f9d4c, 1 parked minor, review otherwise clean)
## Task 2 completion
- Task 2: complete (commits 74f9d4c..2499bf0, review clean). Verified: range = exactly [032d115 RED, 2499bf0 perf:]; impl/v0.2.0 untouched until merge; put() releases in-flight in finally (no success-path leak).
## Batch 1 (perf T3+T4) rulings + completion
- Ruling: SyncDatabase 'Missing' waived — brief lists the file but prescribes zero edits; brief steps are the authority. No fix.
- Ruling: coverage-limitation findings parked as deferred minors (Compose recomposition enforcement, RGB_565/get-put assertions) — no scope expansion mid-batch; final review triages. Cost if wrong: partial regression safety on two behaviors.
- Ruling: batch-branch scheme stands (see Task 1 ruling).
- Verified: RED 36177626806 on 9cf053b, GREEN 36178016974 on d4c305b, RED 36178432338 on deb1742, GREEN 36179013234 on 6eb5ac3.
- Batch 1: complete (commits 1e7dd02..6eb5ac3, 2 parked minors, review otherwise clean)
