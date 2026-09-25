package com.exodidio.tune.ui.navigation

import com.exodidio.tune.AppDestination
import com.exodidio.tune.ui.components.MaterialSymbols
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationMetadataTest {
    @Test
    fun libraryDestinationUsesMusicVideoSymbol() {
        assertEquals(MaterialSymbols.Library, AppDestination.Library.symbol)
        assertEquals("music_video", AppDestination.Library.symbol)
    }

    @Test
    fun mapsDestinationsToCorrectSymbols() {
        assertEquals(
            listOf(AppDestination.Home, AppDestination.Insight, AppDestination.Library, AppDestination.Settings),
            AppDestination.entries,
        )
        assertEquals(MaterialSymbols.Home, AppDestination.Home.symbol)
        assertEquals(MaterialSymbols.BarChart, AppDestination.Insight.symbol)
        assertEquals(MaterialSymbols.Library, AppDestination.Library.symbol)
        assertEquals(MaterialSymbols.Settings, AppDestination.Settings.symbol)
    }
}
