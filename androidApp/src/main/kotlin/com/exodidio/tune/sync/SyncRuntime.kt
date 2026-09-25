package com.exodidio.tune.sync

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

sealed interface AndroidSyncState {
    data object Idle : AndroidSyncState
    data class Running(val planId: String? = null, val completed: Int = 0, val total: Int = 0) : AndroidSyncState
    data class Failed(val message: String, val requiredBytes: Long? = null, val availableBytes: Long? = null) : AndroidSyncState
    data class Completed(val planId: String) : AndroidSyncState
}

internal object AndroidSyncRuntime {
    private lateinit var store: AndroidLibrarySyncStore
    private lateinit var appContext: Context
    private val _state = MutableStateFlow<AndroidSyncState>(AndroidSyncState.Idle)
    val state: StateFlow<AndroidSyncState> = _state

    fun initialize(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        store = AndroidLibrarySyncStore(SyncDatabase.create(appContext), appContext.filesDir)
    }

    fun running(planId: String? = null, completed: Int = 0, total: Int = 0) { _state.value = AndroidSyncState.Running(planId, completed, total) }
    fun failed(message: String, requiredBytes: Long? = null, availableBytes: Long? = null) {
        _state.value = AndroidSyncState.Failed(message, requiredBytes, availableBytes)
    }
    fun completed(planId: String) { _state.value = AndroidSyncState.Completed(planId) }
    fun idle() { _state.value = AndroidSyncState.Idle }
    suspend fun awaitNoForegroundSync() { state.first { it !is AndroidSyncState.Running } }
    fun tracks(): Flow<List<LibraryTrack>> = if (::appContext.isInitialized) store.tracks else flowOf(emptyList())
    internal fun syncStore(): AndroidLibrarySyncStore = store
}
