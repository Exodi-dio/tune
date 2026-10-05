package com.exodidio.tune.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

internal class AmllBridgeController(
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var commandHandler: ((String) -> Unit)? = null
    var resultHandler: ((AmllBridgeResult) -> Unit)? = null
    var isReady: Boolean = false
        private set

    private var activeTrackId: String? = null
    private var lastSeekRequestId: Long = Long.MIN_VALUE
    private var disposed = false

    fun load(trackId: String, content: String, format: AmllLyricFormat) {
        if (disposed) return
        activeTrackId = trackId
        isReady = false
        lastSeekRequestId = Long.MIN_VALUE
        dispatch(
            buildJsonObject {
                put("type", "load")
                put("trackId", trackId)
                put("content", content)
                put("format", format.name.lowercase())
            },
        )
    }

    fun updatePosition(trackId: String, positionMs: Long) {
        if (disposed || trackId != activeTrackId) return
        dispatch(
            buildJsonObject {
                put("type", "position")
                put("trackId", trackId)
                put("positionMs", positionMs)
                put("timestamp", clock())
            },
        )
    }

    fun requestSeek(trackId: String, positionMs: Long, requestId: Long) {
        if (disposed || trackId != activeTrackId || requestId <= lastSeekRequestId) return
        lastSeekRequestId = requestId
        dispatch(
            buildJsonObject {
                put("type", "seek")
                put("trackId", trackId)
                put("positionMs", positionMs)
                put("requestId", requestId)
                put("timestamp", clock())
            },
        )
    }

    fun setRomanizationEnabled(enabled: Boolean) {
        if (disposed) return
        dispatch(
            buildJsonObject {
                put("type", "romanization")
                put("enabled", enabled)
            },
        )
    }

    fun onBridgeMessage(json: String): AmllBridgeResult {
        if (disposed) return AmllBridgeResult.Fallback("Bridge controller is disposed")

        val envelope = runCatching {
            Json.parseToJsonElement(json) as? JsonObject
        }.getOrNull()
        if (envelope == null) {
            val fallback = AmllBridgeResult.Fallback("Invalid AMLL bridge payload")
            resultHandler?.invoke(fallback)
            return fallback
        }

        val payloadTrackId = (envelope["trackId"] as? JsonPrimitive)?.contentOrNull
        if (payloadTrackId != null && payloadTrackId != activeTrackId) {
            return AmllBridgeResult.Fallback("Stale AMLL bridge track")
        }

        val payload = JsonObject(envelope - "trackId").toString()
        val result = parseAmllBridgePayload(payload)
        when {
            result is AmllBridgeResult.Ready && isReady ->
                return AmllBridgeResult.Fallback("Duplicate AMLL bridge readiness")
            result is AmllBridgeResult.Ready -> {
                isReady = true
                resultHandler?.invoke(result)
            }
            result is AmllBridgeResult.Fallback -> {
                isReady = false
                resultHandler?.invoke(result)
            }
        }
        return result
    }

    fun dispose() {
        disposed = true
        activeTrackId = null
        isReady = false
        commandHandler = null
        resultHandler = null
    }

    private fun dispatch(command: JsonObject) {
        commandHandler?.invoke(command.toString())
    }
}
