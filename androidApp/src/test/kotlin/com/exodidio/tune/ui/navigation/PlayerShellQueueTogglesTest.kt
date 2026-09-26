package com.exodidio.tune.ui.navigation

import com.exodidio.tune.player.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Fix wave (C1): pins the queue header toggle -> callback mapping. The header
// buttons call onShuffleChange(toggledShuffle(queue.shuffle)) and
// onRepeatModeChange(nextRepeatMode(queue.repeatMode)).
class PlayerShellQueueTogglesTest {
    @Test
    fun shuffleToggleNegatesCurrentState() {
        assertTrue(toggledShuffle(false))
        assertFalse(toggledShuffle(true))
    }

    @Test
    fun repeatToggleCyclesOffAllOneOff() {
        assertEquals(RepeatMode.All, nextRepeatMode(RepeatMode.Off))
        assertEquals(RepeatMode.One, nextRepeatMode(RepeatMode.All))
        assertEquals(RepeatMode.Off, nextRepeatMode(RepeatMode.One))
    }

    @Test
    fun toggleCallbacksReceiveMappedValues() {
        var shuffle: Boolean? = null
        var repeat: RepeatMode? = null
        val queue = com.exodidio.tune.player.PlaybackQueueSnapshot(shuffle = false, repeatMode = RepeatMode.Off)
        val onShuffleChange: (Boolean) -> Unit = { shuffle = it }
        val onRepeatModeChange: (RepeatMode) -> Unit = { repeat = it }
        // Same expressions as the header buttons.
        onShuffleChange(toggledShuffle(queue.shuffle))
        onRepeatModeChange(nextRepeatMode(queue.repeatMode))
        assertEquals(true, shuffle)
        assertEquals(RepeatMode.All, repeat)
    }
}
