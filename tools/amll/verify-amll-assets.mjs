import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const expectedAmllRevision =
  "86200dead453bb067e554e989110cbadca8d4756";
const expectedTtmlToolRevision =
  "d4953b351ae073c1447464790fff17aa9bc1d807";
const outputDirectory = resolve(
  process.env.AMLL_OUTPUT_DIRECTORY ??
    "androidApp/build/generated/amllAssets/amll",
);

if (process.env.AMLL_SOURCE_REVISION !== expectedAmllRevision) {
  console.error(
    `AMLL_SOURCE_REVISION must be ${expectedAmllRevision}; received ${process.env.AMLL_SOURCE_REVISION ?? "<missing>"}`,
  );
  process.exit(2);
}

if (process.env.TTML_TOOL_SOURCE_REVISION !== expectedTtmlToolRevision) {
  console.error(
    `TTML_TOOL_SOURCE_REVISION must be ${expectedTtmlToolRevision}; received ${process.env.TTML_TOOL_SOURCE_REVISION ?? "<missing>"}`,
  );
  process.exit(2);
}

const requiredFiles = [
  "index.html",
  "bridge.js",
  "amll-core.mjs",
  "style.css",
  "amll-lyric.mjs",
  "amll-ttml.mjs",
];
const missingFiles = requiredFiles.filter(
  (file) => !existsSync(resolve(outputDirectory, file)),
);
if (missingFiles.length > 0) {
  console.error(`Missing AMLL assets: ${missingFiles.join(", ")}`);
  process.exit(3);
}

const coreBundle = readFileSync(resolve(outputDirectory, "amll-core.mjs"), "utf8");
const ttmlBundle = readFileSync(resolve(outputDirectory, "amll-ttml.mjs"), "utf8");
if (
  !coreBundle.includes("@applemusic-like-lyrics/core") ||
  !ttmlBundle.includes("@applemusic-like-lyrics/ttml")
) {
  console.error("AMLL package markers are missing from generated bundles");
  process.exit(4);
}

console.log(`Verified AMLL assets in ${outputDirectory}`);
