package com.exodidio.tune.pairing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.exodidio.tune.pairing.PairingEndpoint
import com.exodidio.tune.pairing.PairedDesktop

interface SyncSession {
    val isConnected: StateFlow<Boolean>
    val connectedEndpoint: StateFlow<PairingEndpoint?>
    val syncRequests: Flow<String>
    /** Playlist reconciliation is deliberately separate from asset-download requests. */
    val playlistReconciliationRequests: Flow<String>

    /** Reconnects only while [reconnect] remains enabled for this in-memory route. */
    fun connect(desktop: PairedDesktop, endpoint: PairingEndpoint, mobileId: String, reconnect: Boolean)
    fun stopReconnecting()
    suspend fun publish(topic: String, payload: String)
    fun disconnect()
}
