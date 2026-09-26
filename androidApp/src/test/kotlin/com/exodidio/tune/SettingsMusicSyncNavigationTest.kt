package com.exodidio.tune

import com.exodidio.tune.settings.ThemeMode
import com.exodidio.tune.settings.ThemeModeStore
import com.exodidio.tune.ui.navigation.titleRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsMusicSyncNavigationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `music sync page belongs to the settings destination`() {
        assertEquals(AppDestination.Settings, AppStackPage.SettingsMusicSync.destination)
    }

    @Test
    fun `music sync page resolves to the music sync title`() {
        assertEquals(R.string.music_sync_title, AppStackPage.SettingsMusicSync.titleRes(AppDestination.Settings))
    }

    @Test
    fun `opening the music sync page selects settings and pushes the page`() = runTest {
        val viewModel = MainViewModel(MusicSyncFakeThemeModeStore())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.dispatch(AppIntent.OpenPage(AppStackPage.SettingsMusicSync))
        advanceUntilIdle()

        assertEquals(AppDestination.Settings, viewModel.uiState.value.selectedDestination)
        assertEquals(AppStackPage.SettingsMusicSync, viewModel.uiState.value.currentPage)

        viewModel.dispatch(AppIntent.NavigateBack)
        advanceUntilIdle()

        assertEquals(AppStackPage.Root, viewModel.uiState.value.currentPage)
    }
}

private class MusicSyncFakeThemeModeStore(initialThemeMode: ThemeMode = ThemeMode.System) : ThemeModeStore {
    private val mutableThemeMode = MutableStateFlow(initialThemeMode)
    private val mutableReduceTransparency = MutableStateFlow(false)
    override val themeMode: Flow<ThemeMode> = mutableThemeMode
    override val reduceTransparency: Flow<Boolean> = mutableReduceTransparency
    override suspend fun setThemeMode(themeMode: ThemeMode) {
        mutableThemeMode.value = themeMode
    }
    override suspend fun setReduceTransparency(enabled: Boolean) {
        mutableReduceTransparency.value = enabled
    }
}
