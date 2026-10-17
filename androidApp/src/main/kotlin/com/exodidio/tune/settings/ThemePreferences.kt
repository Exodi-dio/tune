package com.exodidio.tune.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.exodidio.tune.AppDestination

private val Context.themeDataStore by preferencesDataStore(name = "appearance")
private val ThemeModeKey = stringPreferencesKey("theme_mode")
private val ReduceTransparencyKey = booleanPreferencesKey("reduce_transparency")
private val PlayerThemeKey = stringPreferencesKey("player_theme")
private val ShowVolumeSliderKey = booleanPreferencesKey("show_volume_slider")
private val FullscreenArtworkKey = booleanPreferencesKey("fullscreen_artwork")
private val FullscreenLyricsKey = booleanPreferencesKey("fullscreen_lyrics")
private val VisibleTabsKey = stringSetPreferencesKey("visible_tabs")
private val KeepScreenOnKey = booleanPreferencesKey("keep_screen_on")

enum class ThemeMode(val storageValue: String, val labelRes: Int) {
    System("system", com.exodidio.tune.R.string.theme_system),
    Light("light", com.exodidio.tune.R.string.theme_light),
    Dark("dark", com.exodidio.tune.R.string.theme_dark),
    Amoled("amoled", com.exodidio.tune.R.string.theme_amoled),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull { it.storageValue == value } ?: System
    }
}

enum class PlayerTheme(val storageValue: String) {
    Standard("standard"),
    Adaptive("adaptive"),
    ;

    companion object {
        fun fromStorage(value: String?): PlayerTheme = entries.firstOrNull { it.storageValue == value } ?: Standard
    }
}

interface ThemeModeStore {
    val themeMode: Flow<ThemeMode>
    val reduceTransparency: Flow<Boolean>
    val playerTheme: Flow<PlayerTheme>
    val showVolumeSlider: Flow<Boolean>
    val fullscreenArtwork: Flow<Boolean>
    val fullscreenLyrics: Flow<Boolean>
    val visibleTabs: Flow<List<AppDestination>>
    val keepScreenOn: Flow<Boolean>

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setReduceTransparency(enabled: Boolean)
    suspend fun setPlayerTheme(theme: PlayerTheme)
    suspend fun setShowVolumeSlider(enabled: Boolean)
    suspend fun setFullscreenArtwork(enabled: Boolean)
    suspend fun setVisibleTabs(visibleTabs: List<AppDestination>)
    suspend fun setFullscreenLyrics(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
}

class ThemePreferences(
    private val context: Context,
) : ThemeModeStore {
    override val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { preferences: Preferences ->
        ThemeMode.fromStorage(preferences[ThemeModeKey])
    }
    override val reduceTransparency: Flow<Boolean> = context.themeDataStore.data.map { preferences: Preferences ->
        preferences[ReduceTransparencyKey] ?: false
    }
    override val playerTheme: Flow<PlayerTheme> = context.themeDataStore.data.map { preferences: Preferences ->
        PlayerTheme.fromStorage(preferences[PlayerThemeKey])
    }
    override val showVolumeSlider: Flow<Boolean> = context.themeDataStore.data.map { preferences: Preferences ->
        preferences[ShowVolumeSliderKey] ?: false
    }
    override val fullscreenArtwork: Flow<Boolean> = context.themeDataStore.data.map { preferences: Preferences ->
        preferences[FullscreenArtworkKey] ?: false
    }
    override val fullscreenLyrics: Flow<Boolean> = context.themeDataStore.data.map { preferences: Preferences ->
        preferences[FullscreenLyricsKey] ?: false
    }
    override val visibleTabs: Flow<List<AppDestination>> = context.themeDataStore.data.map { preferences: Preferences ->
        preferences[VisibleTabsKey]?.mapNotNull { AppDestination.fromStorage(it) }?.takeIf { it.isNotEmpty() }
            ?: AppDestination.entries.toList()
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[ThemeModeKey] = themeMode.storageValue
        }
    }

    override suspend fun setReduceTransparency(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[ReduceTransparencyKey] = enabled
        }
    }

    override suspend fun setPlayerTheme(theme: PlayerTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[PlayerThemeKey] = theme.storageValue
        }
    }

    override suspend fun setShowVolumeSlider(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[ShowVolumeSliderKey] = enabled
        }
    }

    override suspend fun setFullscreenArtwork(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[FullscreenArtworkKey] = enabled
        }
    }

    override suspend fun setFullscreenLyrics(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[FullscreenLyricsKey] = enabled
        }
    }

    override suspend fun setVisibleTabs(visibleTabs: List<AppDestination>) {
        context.themeDataStore.edit { preferences ->
            preferences[VisibleTabsKey] = visibleTabs.map { it.storageValue }.toSet()
        }
    }

    override val keepScreenOn: Flow<Boolean> = context.themeDataStore.data.map { preferences ->
        preferences[KeepScreenOnKey] ?: false
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[KeepScreenOnKey] = enabled
        }
    }
}
