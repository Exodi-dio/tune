package com.exodidio.tune.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.scanFolderDataStore by preferencesDataStore(name = "scan_folders")
private val ScanFoldersKey = stringSetPreferencesKey("scan_folders")
private val ScanFolderSingleKey = stringPreferencesKey("scan_folder_single")

/**
 * Persisted scan folder whitelist (Part B). Stores the raw `OpenDocumentTree`
 * tree URI strings (or file paths when already resolved); see
 * [scanFilePrefixes] for the URI -> filesystem mapping used at scan time.
 * Empty set means "scan everything" (current behavior).
 */
internal class ScanFolderPreferences(private val context: Context) {
    val folders: Flow<Set<String>> = context.scanFolderDataStore.data.map { preferences ->
        preferences[ScanFoldersKey] ?: emptySet()
    }

    suspend fun setFolders(folders: Set<String>) {
        context.scanFolderDataStore.edit { preferences ->
            preferences[ScanFoldersKey] = folders.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
    }

    suspend fun addFolder(folder: String) {
        val value = folder.trim().takeIf { it.isNotBlank() } ?: return
        context.scanFolderDataStore.edit { preferences ->
            preferences[ScanFoldersKey] = (preferences[ScanFoldersKey] ?: emptySet()) + value
        }
    }

    suspend fun removeFolder(folder: String) {
        context.scanFolderDataStore.edit { preferences ->
            preferences[ScanFoldersKey] = (preferences[ScanFoldersKey] ?: emptySet()) - folder
            if (preferences[ScanFolderSingleKey] == folder) preferences.remove(ScanFolderSingleKey)
        }
    }

    /**
     * Single selected scan directory (v1.2.0). Empty/null means "scan everything".
     * Migrates from the legacy multi-folder whitelist by keeping the first entry.
     */
    val folder: Flow<String?> = context.scanFolderDataStore.data.map { preferences ->
        preferences[ScanFolderSingleKey]?.takeIf { it.isNotBlank() }
            ?: preferences[ScanFoldersKey]?.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    suspend fun setFolder(folder: String?) {
        val value = folder?.trim().takeIf { !it.isNullOrBlank() }
        context.scanFolderDataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(ScanFolderSingleKey)
            } else {
                preferences[ScanFolderSingleKey] = value
                // Keep legacy whitelist in sync (single entry) for scanner compat.
                preferences[ScanFoldersKey] = setOf(value)
            }
        }
    }

    suspend fun clearFolder() = setFolder(null)
}
