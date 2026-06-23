package com.nxvpn.app.data.model

import kotlinx.serialization.Serializable

/**
 * A user-imported VPN server.
 *
 * The full tunnel definition lives in [config] verbatim (a WireGuard `.conf` or an
 * OpenVPN `.ovpn`), so NX-VPN never has to re-serialise something the backend already
 * understands. The remaining fields are purely for display in the UI.
 */
@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val protocol: VpnProtocol,
    val config: String,
    val countryCode: String = "",
    val flagEmoji: String = "🌐",
    val endpoint: String = "",
) {
    val subtitle: String
        get() = buildString {
            append(protocol.displayName)
            if (endpoint.isNotBlank()) append(" · ").append(endpoint)
        }
}
