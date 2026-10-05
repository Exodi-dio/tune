#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 OUTPUT_DIRECTORY" >&2
  exit 64
fi

: "${RUNNER_TEMP:?RUNNER_TEMP is required}"

AMLL_REVISION="86200dead453bb067e554e989110cbadca8d4756"
TTML_TOOL_REVISION="d4953b351ae073c1447464790fff17aa9bc1d807"
REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIRECTORY="$1"
AMLL_DIRECTORY="$RUNNER_TEMP/applemusic-like-lyrics"
TTML_TOOL_DIRECTORY="$RUNNER_TEMP/amll-ttml-tool"

if [[ -e "$AMLL_DIRECTORY" || -e "$TTML_TOOL_DIRECTORY" ]]; then
  echo "AMLL checkout paths already exist in RUNNER_TEMP" >&2
  exit 65
fi
mkdir -p "$OUTPUT_DIRECTORY"

git clone --filter=blob:none \
  https://github.com/amll-dev/applemusic-like-lyrics.git \
  "$AMLL_DIRECTORY"
git -C "$AMLL_DIRECTORY" checkout --detach "$AMLL_REVISION"
[[ "$(git -C "$AMLL_DIRECTORY" rev-parse HEAD)" == "$AMLL_REVISION" ]]

git clone --filter=blob:none \
  https://github.com/amll-dev/amll-ttml-tool.git \
  "$TTML_TOOL_DIRECTORY"
git -C "$TTML_TOOL_DIRECTORY" checkout --detach "$TTML_TOOL_REVISION"
[[ "$(git -C "$TTML_TOOL_DIRECTORY" rev-parse HEAD)" == "$TTML_TOOL_REVISION" ]]

cd "$AMLL_DIRECTORY"
corepack enable
corepack pnpm@11.21.0 install --frozen-lockfile
corepack pnpm@11.21.0 --filter "@applemusic-like-lyrics/ttml" build
corepack pnpm@11.21.0 --filter "@applemusic-like-lyrics/lyric" build
corepack pnpm@11.21.0 --filter "@applemusic-like-lyrics/core" build

cp packages/ttml/dist/amll-ttml.mjs "$OUTPUT_DIRECTORY/"
cp packages/lyric/dist/amll-lyric.mjs "$OUTPUT_DIRECTORY/"
cp packages/core/dist/amll-core.mjs "$OUTPUT_DIRECTORY/"
cp packages/core/dist/style.css "$OUTPUT_DIRECTORY/"
cp "$REPOSITORY_ROOT/tools/amll/index.html" "$OUTPUT_DIRECTORY/"
cp "$REPOSITORY_ROOT/tools/amll/bridge.js" "$OUTPUT_DIRECTORY/"

printf '\n/* @applemusic-like-lyrics/core */\n' >> "$OUTPUT_DIRECTORY/amll-core.mjs"
printf '\n/* @applemusic-like-lyrics/ttml */\n' >> "$OUTPUT_DIRECTORY/amll-ttml.mjs"

AMLL_SOURCE_REVISION="$AMLL_REVISION" \
TTML_TOOL_SOURCE_REVISION="$TTML_TOOL_REVISION" \
AMLL_OUTPUT_DIRECTORY="$OUTPUT_DIRECTORY" \
node "$REPOSITORY_ROOT/tools/amll/verify-amll-assets.mjs"
