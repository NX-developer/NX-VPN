package com.nxvpn.app.vpn

import com.wireguard.android.backend.Tunnel

/** Thin [Tunnel] implementation that forwards WireGuard state changes to a callback. */
class NxTunnel(
    private val tunnelName: String,
    private val onState: (Tunnel.State) -> Unit,
) : Tunnel {
    override fun getName(): String = tunnelName
    override fun onStateChange(newState: Tunnel.State) = onState(newState)
}
