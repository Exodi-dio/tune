package com.exodidio.tune.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmllBridgeControllerTest {
    private var now = 1_000L
    private val commands = mutableListOf<String>()
    private val results = mutableListOf<AmllBridgeResult>()

    private fun controller() = AmllBridgeController { now }.also {
        it.commandHandler = commands::add
        it.resultHandler = results::add
    }

    private fun ready(trackId: String? = "track-1") = """
        {"type":"ready","trackId":${trackId?.let { """"$it""" } ?: "null"},"lines":[]}
    """.trimIndent()

    @Test
    fun loadForNewTrackClearsPreviousBridgeState() {
        val controller = controller()
        controller.load("track-1", "[00:01.00]First", AmllLyricFormat.Lrc)
        val firstReady = controller.onBridgeMessage(ready("track-1"))
        assertTrue("Expected Ready but was $firstReady", firstReady is AmllBridgeResult.Ready)

        controller.load("track-2", "Second", AmllLyricFormat.Plain)

        assertTrue(controller.onBridgeMessage(ready("track-1")) is AmllBridgeResult.Fallback)
        assertTrue(controller.onBridgeMessage(ready("track-2")) is AmllBridgeResult.Ready)
    }

    @Test
    fun updatePositionForStaleTrackIsIgnored() {
        val controller = controller()
        controller.load("track-1", "First", AmllLyricFormat.Plain)
        commands.clear()
        now = 2_000L

        controller.updatePosition("track-2", 5_000L)

        assertTrue(commands.none { it.contains("position") })
        controller.updatePosition("track-1", 5_000L)
        assertTrue(commands.last().contains("position"))
        assertEquals(2_000L, command("position").getValue("timestamp").jsonPrimitive.content.toLong())
    }

    @Test
    fun requestSeekForOldRequestIsIgnored() {
        val controller = controller()
        controller.load("track-1", "First", AmllLyricFormat.Plain)
        commands.clear()

        controller.requestSeek("track-1", 4_000L, 7L)
        controller.requestSeek("track-1", 9_000L, 6L)

        assertEquals(1, commands.count { it.contains("seek") })
        assertEquals(7L, command("seek").getValue("requestId").jsonPrimitive.content.toLong())
        controller.requestSeek("track-2", 12_000L, 8L)
        assertEquals(1, commands.count { it.contains("seek") })
    }

    @Test
    fun readyPayloadIsForwardedOnce() {
        val controller = controller()
        controller.load("track-1", "First", AmllLyricFormat.Plain)

        val firstReady = controller.onBridgeMessage(ready("track-1"))
        assertTrue("Expected Ready but was $firstReady", firstReady is AmllBridgeResult.Ready)
        assertTrue(controller.onBridgeMessage(ready("track-1")) is AmllBridgeResult.Fallback)
        assertEquals(1, results.count { it is AmllBridgeResult.Ready })
    }

    @Test
    fun fallbackPayloadDoesNotReachReadyState() {
        val controller = controller()
        controller.load("track-1", "<tt><body>", AmllLyricFormat.Ttml)

        val fallback = controller.onBridgeMessage("""{"type":"fallback","trackId":"track-1","reason":"invalid TTML"}""")

        assertTrue(fallback is AmllBridgeResult.Fallback)
        assertFalse(controller.isReady)
        val ready = controller.onBridgeMessage(ready("track-1"))
        assertTrue("Expected Ready but was $ready", ready is AmllBridgeResult.Ready)
    }

    @Test
    fun romanizationEnabledIsForwardedToAmlL() {
        val controller = controller()
        controller.load("track-1", "First", AmllLyricFormat.Plain)
        commands.clear()

        controller.setRomanizationEnabled(true)

        assertEquals("romanization", command("romanization").getValue("type").jsonPrimitive.content)
        assertTrue(command("romanization").getValue("enabled").jsonPrimitive.booleanOrNull == true)
    }

    @Test
    fun disposeStopsFurtherMessages() {
        val controller = controller()
        controller.load("track-1", "First", AmllLyricFormat.Plain)
        commands.clear()
        results.clear()

        controller.dispose()
        controller.updatePosition("track-1", 1_000L)
        val result = controller.onBridgeMessage(ready("track-1"))

        assertTrue(result is AmllBridgeResult.Fallback)
        assertTrue(commands.isEmpty())
        assertTrue(results.isEmpty())
    }

    private fun command(type: String): JsonObject = commands
        .last { Json.parseToJsonElement(it).jsonObject.getValue("type").jsonPrimitive.content == type }
        .let { Json.parseToJsonElement(it).jsonObject }
}
