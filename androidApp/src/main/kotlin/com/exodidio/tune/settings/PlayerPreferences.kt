package com.exodidio.tune.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playerDataStore by preferencesDataStore(name = "player")
private val FullBleedArtworkKey = booleanPreferencesKey("full_bleed_artwork")

class PlayerPreferences(
    private val context: Context,
) {
    val fullBleedArtwork: Flow<Boolean> = context.playerDataStore.data.map { preferences ->
        preferences[FullBleedArtworkKey] ?: true
    }

    suspend fun setFullBleedArtwork(enabled: Boolean) {
        context.playerDataStore.edit { preferences ->
            preferences[FullBleedArtworkKey] = enabled
        }
    }
}
