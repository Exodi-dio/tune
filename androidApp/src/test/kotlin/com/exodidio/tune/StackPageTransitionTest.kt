package com.exodidio.tune

import com.exodidio.tune.ui.navigation.PageKey
import com.exodidio.tune.ui.navigation.isForwardTransition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackPageTransitionTest {
    @Test
    fun pushesInStackAreIdentifiedAsForwardTransitions() {
        val rootKey = PageKey(AppDestination.Settings, AppStackPage.Root, index = 0)
        val appearanceKey = PageKey(AppDestination.Settings, AppStackPage.SettingsAppearance, index = 1)
        val playbackKey = PageKey(AppDestination.Settings, AppStackPage.SettingsPlayback, index = 2)

        assertTrue(isForwardTransition(target = appearanceKey, initial = rootKey))
        assertTrue(isForwardTransition(target = playbackKey, initial = appearanceKey))
    }

    @Test
    fun popsInStackAreIdentifiedAsBackwardTransitions() {
        val rootKey = PageKey(AppDestination.Settings, AppStackPage.Root, index = 0)
        val appearanceKey = PageKey(AppDestination.Settings, AppStackPage.SettingsAppearance, index = 1)
        val playbackKey = PageKey(AppDestination.Settings, AppStackPage.SettingsPlayback, index = 2)

        assertFalse(isForwardTransition(target = appearanceKey, initial = playbackKey))
        assertFalse(isForwardTransition(target = rootKey, initial = appearanceKey))
    }

    @Test
    fun destinationChangesAreNotIdentifiedAsStackTransitions() {
        val homeKey = PageKey(AppDestination.Home, AppStackPage.Root, index = 0)
        val libraryKey = PageKey(AppDestination.Library, AppStackPage.Root, index = 0)

        assertFalse(homeKey.destination == libraryKey.destination)
    }

    @Test
    fun albumDetailsPushesForwardFromTheAlbumList() {
        val stack = listOf(
            AppStackPage.Root,
            AppStackPage.LibraryAlbums,
            AppStackPage.AlbumDetails,
        )
        val albumsKey = stack.dropLast(1).currentStackPage(AppDestination.Library)
        val albumDetailsKey = stack.currentStackPage(AppDestination.Library)

        assertTrue(isForwardTransition(target = albumDetailsKey, initial = albumsKey))
    }
}