import { parseLrc } from "./amll-lyric.mjs";
import { parseTTML } from "./amll-ttml.mjs";

let playerPromise;
let player;
let activeTrackId = null;
let currentPositionMs = 0;
let romanizationEnabled = false;
let disposed = false;
let previousFrameTime;
let playerGeneration = 0;

const failure = (error) => ({
  ok: false,
  error: error instanceof Error ? error.message : String(error),
});

const plainLine = (content) => ({
  ok: true,
  lines: [
    {
      words: [
        {
          startTime: 0,
          endTime: 0,
          word: content.trim(),
        },
      ],
      translatedLyric: "",
      romanLyric: "",
      isBG: false,
      isDuet: false,
      startTime: 0,
      endTime: 0,
    },
  ],
});

window.amllParse = (format, content) => {
  try {
    if (typeof content !== "string") {
      throw new TypeError("Lyric content must be a string");
    }

    switch (String(format).toLowerCase()) {
      case "ttml":
        return { ok: true, lines: parseTTML(content).lines };
      case "lrc":
        return { ok: true, lines: parseLrc(content) };
      case "plain":
        return plainLine(content);
      default:
        return { ok: true, lines: parseLrc(content) };
    }
  } catch (error) {
    return failure(error);
  }
};

window.amllReady = true;

const post = (payload) => {
  const bridge = globalThis.TuneAmllBridge;
  if (bridge?.postMessage) {
    bridge.postMessage(JSON.stringify(payload));
  }
};

const reportFallback = (trackId, reason) => {
  post({ type: "fallback", trackId, reason: String(reason) });
};

const ensurePlayer = async () => {
  const generation = playerGeneration;
  playerPromise ??= import("./amll-core.mjs").then(({ LyricPlayer }) => {
    const container = document.getElementById("lyric-player");
    if (!container) {
      throw new Error("AMLL player container is missing");
    }
    if (disposed || generation !== playerGeneration) return null;
    player?.dispose();
    player = new LyricPlayer();
    container.replaceChildren(player.getElement());
    container.dataset.tuneRomanization = romanizationEnabled ? "on" : "off";
    player.resume();
    return player;
  });
  return playerPromise;
};

const markRomanizationElements = () => {
  const container = document.getElementById("lyric-player");
  if (!container) return;
  for (const element of container.querySelectorAll("div")) {
    if (
      element.children.length === 3 &&
      Array.from(element.children).every((child) => child.tagName === "DIV") &&
      !element.dataset.tuneLyricLine
    ) {
      element.dataset.tuneLyricLine = "true";
      const romanElement = element.children[2];
      romanElement.dataset.tuneRoman = "true";
      romanElement.style.display = romanizationEnabled ? "" : "none";
    }
  }
};

const animate = (timestamp) => {
  if (disposed) return;
  const delta = previousFrameTime === undefined ? 0 : timestamp - previousFrameTime;
  previousFrameTime = timestamp;
  markRomanizationElements();
  player?.update(delta);
  requestAnimationFrame(animate);
};

const load = async ({ trackId, content, format }) => {
  disposed = false;
  activeTrackId = trackId;
  currentPositionMs = 0;
  try {
    const parsed = window.amllParse(format, content);
    if (!parsed.ok) {
      reportFallback(trackId, parsed.error);
      return;
    }
    const amllPlayer = await ensurePlayer();
    if (!amllPlayer || disposed || activeTrackId !== trackId) return;
    amllPlayer.setLyricLines(parsed.lines, 0);
    amllPlayer.setCurrentTime(0, true);
    post({
      type: "ready",
      trackId,
      lines: parsed.lines,
    });
  } catch (error) {
    if (!disposed && activeTrackId === trackId) {
      reportFallback(trackId, error);
    }
  }
};

const setPosition = ({ trackId, positionMs }) => {
  if (disposed || trackId !== activeTrackId) return;
  currentPositionMs = Number(positionMs) || 0;
  player?.setCurrentTime(currentPositionMs);
};

const seek = ({ trackId, positionMs }) => {
  if (disposed || trackId !== activeTrackId) return;
  currentPositionMs = Number(positionMs) || 0;
  player?.setCurrentTime(currentPositionMs, true);
};

const setRomanization = ({ enabled }) => {
  romanizationEnabled = Boolean(enabled);
  const container = document.getElementById("lyric-player");
  if (container) {
    container.dataset.tuneRomanization = romanizationEnabled ? "on" : "off";
  }
  for (const romanElement of container?.querySelectorAll("[data-tune-roman]") ?? []) {
    romanElement.style.display = romanizationEnabled ? "" : "none";
  }
  markRomanizationElements();
};

const dispose = () => {
  disposed = true;
  playerGeneration += 1;
  activeTrackId = null;
  currentPositionMs = 0;
  player?.dispose();
  player = undefined;
  playerPromise = undefined;
  document.getElementById("lyric-player")?.replaceChildren();
};

window.TuneAmll = Object.freeze({
  dispatch(command) {
    const payload = typeof command === "string" ? JSON.parse(command) : command;
    switch (payload.type) {
      case "load":
        return load(payload);
      case "position":
        return setPosition(payload);
      case "seek":
        return seek(payload);
      case "romanization":
        return setRomanization(payload);
      case "dispose":
        return dispose();
      default:
        return undefined;
    }
  },
});

if (document.getElementById("lyric-player")) {
  requestAnimationFrame(animate);
}
