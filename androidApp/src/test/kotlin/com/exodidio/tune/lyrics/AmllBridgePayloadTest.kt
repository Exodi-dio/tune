package com.exodidio.tune.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmllBridgePayloadTest {
    @Test
    fun validReadyPayloadPreservesWordTimings() {
        val payload = readyPayload(
            buildJsonObject {
                put("startTime", 1_000L)
                put("endTime", 4_000L)
                put(
                    "words",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("startTime", 1_100L)
                                put("endTime", 1_500L)
                                put("word", "Hello")
                            },
                            buildJsonObject {
                                put("startTime", 1_500L)
                                put("endTime", 2_000L)
                                put("word", "world")
                            },
                        ),
                    ),
                )
            },
        )

        val result = parseAmllBridgePayload(payload)

        assertTrue(result is AmllBridgeResult.Ready)
        val line = (result as AmllBridgeResult.Ready).lines.single()
        assertEquals(1_000L, line.startTime)
        assertEquals(4_000L, line.endTime)
        assertEquals(listOf(1_100L, 1_500L), line.words.map { it.startTime })
        assertEquals(listOf(1_500L, 2_000L), line.words.map { it.endTime })
        assertEquals(listOf("Hello", "world"), line.words.map { it.word })
    }

    @Test
    fun validReadyPayloadPreservesTranslationAndRomanization() {
        val payload = readyPayload(
            buildJsonObject {
                put("startTime", 2_000L)
                put("endTime", 3_000L)
                put("translatedLyric", "你好")
                put("romanLyric", "nǐ hǎo")
                put("isDuet", true)
                put("isBG", true)
                put(
                    "words",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("startTime", 2_000L)
                                put("endTime", 3_000L)
                                put("word", "你好")
                                put("romanWord", "nǐ")
                            },
                        ),
                    ),
                )
            },
        )

        val result = parseAmllBridgePayload(payload)

        assertTrue(result is AmllBridgeResult.Ready)
        val line = (result as AmllBridgeResult.Ready).lines.single()
        assertEquals("你好", line.translatedLyric)
        assertEquals("nǐ hǎo", line.romanLyric)
        assertEquals(true, line.isDuet)
        assertEquals(true, line.isBG)
        assertEquals("nǐ", line.words.single().romanWord)
    }

    @Test
    fun missingLineOrWordTimingReturnsFallback() {
        val missingLineTiming = readyPayload(
            buildJsonObject {
                put("endTime", 3_000L)
                put("words", JsonArray(listOf(validWord())))
            },
        )
        val missingWordTiming = readyPayload(
            buildJsonObject {
                put("startTime", 1_000L)
                put("endTime", 3_000L)
                put("words", JsonArray(listOf(buildJsonObject { put("word", "Hello") })))
            },
        )

        assertTrue(parseAmllBridgePayload(missingLineTiming) is AmllBridgeResult.Fallback)
        assertTrue(parseAmllBridgePayload(missingWordTiming) is AmllBridgeResult.Fallback)
    }

    @Test
    fun invalidJsonReturnsFallback() {
        assertTrue(parseAmllBridgePayload("{not json") is AmllBridgeResult.Fallback)
    }

    @Test
    fun textEscapesRemainDataNotMarkup() {
        val escaped = """<script>alert("x")</script> & "quoted" """
        val payload = readyPayload(
            buildJsonObject {
                put("startTime", 1_000L)
                put("endTime", 2_000L)
                put("translatedLyric", escaped)
                put(
                    "words",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("startTime", 1_000L)
                                put("endTime", 2_000L)
                                put("word", escaped)
                            },
                        ),
                    ),
                )
            },
        )

        val result = parseAmllBridgePayload(payload)

        assertTrue(result is AmllBridgeResult.Ready)
        val line = (result as AmllBridgeResult.Ready).lines.single()
        assertEquals(escaped, line.translatedLyric)
        assertEquals(escaped, line.words.single().word)
    }

    private fun validWord() = buildJsonObject {
        put("startTime", 1_000L)
        put("endTime", 2_000L)
        put("word", "Hello")
    }

    private fun readyPayload(line: JsonObject): String = Json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("type", "ready")
            put("lines", JsonArray(listOf(line)))
        },
    )
}
