package com.exodidio.tune.lyrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class AmllBridgeWord(
    val startTime: Long,
    val endTime: Long,
    val word: String,
    val romanWord: String? = null,
)

@Serializable
internal data class AmllBridgeLine(
    val startTime: Long,
    val endTime: Long,
    val words: List<AmllBridgeWord>,
    val translatedLyric: String? = null,
    val romanLyric: String? = null,
    val isDuet: Boolean? = null,
    val isBG: Boolean? = null,
)

internal sealed interface AmllBridgeResult {
    data class Ready(val lines: List<AmllBridgeLine>) : AmllBridgeResult
    data class Fallback(val reason: String) : AmllBridgeResult
}

internal enum class AmllLyricFormat {
    Ttml,
    Lrc,
    Plain,
}

@Serializable
private data class AmllBridgeReadyPayload(
    val type: String,
    val lines: List<AmllBridgeLine>,
)

@Serializable
private data class AmllBridgeFallbackPayload(
    val type: String,
    val reason: String,
)

private val bridgeJson = Json

internal fun parseAmllBridgePayload(json: String): AmllBridgeResult {
    return try {
        val root = bridgeJson.parseToJsonElement(json) as? JsonObject
            ?: return AmllBridgeResult.Fallback("Bridge payload must be an object")
        val type = (root["type"] as? JsonPrimitive)?.content
            ?: return AmllBridgeResult.Fallback("Bridge payload is missing its type")

        when (type) {
            "ready" -> {
                val payload = bridgeJson.decodeFromString(
                    AmllBridgeReadyPayload.serializer(),
                    root.toString(),
                )
                validateBridgeLines(payload.lines)?.let { return AmllBridgeResult.Fallback(it) }
                AmllBridgeResult.Ready(payload.lines)
            }
            "fallback" -> {
                val payload = bridgeJson.decodeFromString(
                    AmllBridgeFallbackPayload.serializer(),
                    root.toString(),
                )
                AmllBridgeResult.Fallback(payload.reason)
            }
            else -> AmllBridgeResult.Fallback("Unknown bridge payload type: $type")
        }
    } catch (_: Exception) {
        AmllBridgeResult.Fallback("Invalid AMLL bridge payload")
    }
}

private fun validateBridgeLines(lines: List<AmllBridgeLine>): String? {
    for (line in lines) {
        if (line.startTime < 0 || line.endTime < line.startTime) {
            return "Line timing is invalid"
        }

        val spans = mutableSetOf<Pair<Long, Long>>()
        for (word in line.words) {
            if (word.startTime < 0 || word.endTime <= word.startTime) {
                return "Word timing is invalid"
            }
            if (!spans.add(word.startTime to word.endTime)) {
                return "Duplicate word timing"
            }
        }
    }

    return null
}
