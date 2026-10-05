import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const fixture = (name) => readFileSync(path.join(here, "fixtures", name), "utf8");

const amllCheckout = path.join(process.env.RUNNER_TEMP, "applemusic-like-lyrics");
const amllRequire = createRequire(path.join(amlmCheckoutPath(), "package.json"));
const { DOMParser } = amllRequire("@xmldom/xmldom");

globalThis.window = globalThis;
globalThis.DOMParser = DOMParser;
globalThis.document = {
  getElementById: () => null,
};

await import(pathToFileURL(path.join(here, "bridge.js")).href);
assert.equal(typeof window.amllParse, "function", "missing window.amllParse");

const parse = (format, content) => {
  const result = window.amllParse(format, content);
  assert.equal(result.ok, true, result.error);
  return result.lines;
};

{
  const lines = parse("ttml", fixture("word-ttml.ttml"));
  assert.equal(lines.length, 4);
  assert.equal(lines[0].words.length, 3);
  assert.ok(lines[0].translatedLyric.length > 0);
  assert.ok(lines[0].words.some((word) => word.romanWord.length > 0));
  assert.equal(lines.filter((line) => line.isDuet).length, 1);
  assert.equal(lines.filter((line) => line.isBG).length, 1);
}

{
  const lines = parse("ttml", fixture("line-ttml.ttml"));
  assert.equal(lines.length, 2);
  assert.ok(lines.every((line) => line.words.length === 1));
  assert.deepEqual(lines.map((line) => line.startTime), [1_000, 3_000]);
}

{
  const lines = parse("ttml", fixture("ruby-ttml.ttml"));
  assert.equal(lines.length, 1);
  assert.equal(lines[0].words.length, 3);
  assert.ok(lines[0].words[1].ruby.length > 0);
}

{
  const result = window.amllParse("ttml", fixture("tool-schema.ttml"));
  assert.equal(result.ok, true, result.error);
  assert.ok(result.lines.length >= 2);
}

{
  const lines = parse("lrc", "[00:01.000]Hello <00:01.500>World<00:02.000>");
  assert.equal(lines.length, 1);
  assert.equal(lines[0].startTime, 1_000);
  assert.equal(lines[0].endTime, 2_000);
  assert.ok(lines[0].words.length > 1);
}

{
  const result = window.amllParse("ttml", "<tt><body>");
  assert.equal(result.ok, false);
  assert.equal(typeof result.error, "string");
  assert.ok(result.error.length > 0);
}

{
  const lines = parse("plain", "First line\nSecond line");
  assert.equal(lines.length, 1);
  assert.equal(lines[0].startTime, 0);
  assert.equal(lines[0].endTime, Infinity);
}

console.log("AMLL parser tests passed");

function amlmCheckoutPath() {
  return path.join(process.env.RUNNER_TEMP, "applemusic-like-lyrics");
}
