# Tune — agent instructions

## PERMANENT RULE: GitHub cloud only

**Anything and everything — coding, building, tests, compilations, runs,
verification — happens on GitHub cloud. No hardware use. Ever.**

- Never run gradle, npm/pnpm, adb, emulators, compilers, linters, or tests
  on a local machine for this project. Not once.
- Coordinate via `gh`: repos, Contents API, Actions logs, releases.
- Do not keep local working copies of this repo for building. Read-only
  reference clones of upstream sources (tune, Bitchord) are for reading
  only — never build or run them either.
- All code lands via the GitHub API or `git push` (transport only), then
  GitHub Actions builds, tests, and releases it.

## Project facts

- Standalone local Android music player (Kotlin Multiplatform:
  `androidApp` + `sharedLogic`). No iOS, no desktop, no sync.
- Playback core (FFmpeg + AAudio JNI) is proven — do not replace it.
- Package `com.exodidio.tune`, GPLv3, attribution in NOTICE.
- Keep CI green. Stack changes in small commits. Tag test releases `v*`.
- Branding gate: CI fails on `tune|exodidio` outside NOTICE/attribution.
- Icon source: `brand/icon-source.png` (uploaded by owner) → `icon.yml`
  workflow generates the transparent icon set.
