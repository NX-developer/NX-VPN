package com.nxvpn.app.data.model

import kotlinx.serialization.Serializable

/** The tunnelling protocol a [ServerProfile] uses. */
@Serializable
enum class VpnProtocol(val displayName: String) {
    WIREGUARD("WireGuard"),
    OPENVPN("OpenVPN");

    companion object {
        /** Best-effort detection of the protocol from raw config text. */
        fun detect(config: String): VpnProtocol {
            val lower = config.lowercase()
            val looksLikeWireGuard = "[interface]" in lower && "privatekey" in lower
            return if (looksLikeWireGuard) WIREGUARD else OPENVPN
        }
    }
}
