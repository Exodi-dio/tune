package com.exodidio.tune.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.exodidio.tune.ui.theme.TuneColors
import org.junit.Assert.assertEquals
import org.junit.Test

class TuneTrackSliderTest {
    private val colors = TuneColors(
        background = Color.Unspecified,
        playerBackdrop = Color.Unspecified,
        card = Color.Unspecified,
        glass = Color.Unspecified,
        glassOpaque = Color.Unspecified,
        glassElevated = Color.Unspecified,
        sliderInactive = Color.Unspecified,
        buttonSecondary = Color.Unspecified,
        textFieldClear = Color.Unspecified,
        borderGlass = Color.Unspecified,
        textMain = Color.Unspecified,
        textMuted = Color.Unspecified,
        primary = Color.Unspecified,
        onPrimary = Color.White,
        foregroundSubtle = Color.White.copy(alpha = 0.46f),
        success = Color.Unspecified,
        navigationActive = Color.Unspecified,
    )

    @Test
    fun filledTrackColorMatchesItsRestingAndInteractingStates() {
        assertEquals(
            lerp(colors.foregroundSubtle, colors.onPrimary, 0.5f),
            sliderFilledTrackColor(colors, isInteracting = false),
        )
        assertEquals(colors.onPrimary, sliderFilledTrackColor(colors, isInteracting = true))
    }
}
