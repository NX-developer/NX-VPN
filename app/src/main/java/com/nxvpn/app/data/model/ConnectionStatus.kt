package com.nxvpn.app.data.model

/** High-level state of the VPN tunnel, surfaced to the UI. */
sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data class Connecting(val profile: ServerProfile) : ConnectionStatus
    data class Connected(val profile: ServerProfile, val connectedAtMillis: Long) : ConnectionStatus
    data object Disconnecting : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus

    val activeProfile: ServerProfile?
        get() = when (this) {
            is Connecting -> profile
            is Connected -> profile
            else -> null
        }
}

/** Cumulative byte counters for the active tunnel. */
data class TrafficStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
)
