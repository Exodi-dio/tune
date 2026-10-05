import { parseLrc } from "./amll-lyric.mjs";
import { parseTTML } from "./amll-ttml.mjs";

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
          endTime: Infinity,
          word: content.trim(),
        },
      ],
      translatedLyric: "",
      romanLyric: "",
      isBG: false,
      isDuet: false,
      startTime: 0,
      endTime: Infinity,
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
