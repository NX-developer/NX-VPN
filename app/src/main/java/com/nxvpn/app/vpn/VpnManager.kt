package com.nxvpn.app.vpn

import android.content.Context
import com.nxvpn.app.data.model.ConnectionStatus
import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.data.model.TrafficStats
import com.nxvpn.app.data.model.VpnProtocol
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader

/**
 * Single entry point for bringing tunnels up and down.
 *
 * WireGuard is handled in-process by the bundled [GoBackend]. OpenVPN is delegated to the user's
 * installed "OpenVPN for Android" engine through [openVpn] (the AIDL external API); because that
 * flow needs Activity-launched consent dialogs, the OpenVPN connection is orchestrated by the
 * Activity, while disconnect/status routing stays centralised here.
 */
class VpnManager(context: Context) {

    private val appContext = context.applicationContext
    private val backend: Backend by lazy { GoBackend(appContext) }
    private var activeTunnel: NxTunnel? = null

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    /** OpenVPN delegate. Public so the Activity can drive its consent dialogs. */
    val openVpn = OpenVpnConnector(appContext) { _status.value = it }

    /** Brings up a WireGuard tunnel. OpenVPN is handled via [openVpn] from the Activity. */
    suspend fun connect(profile: ServerProfile, monotonicNow: Long) {
        when (profile.protocol) {
            VpnProtocol.WIREGUARD -> connectWireGuard(profile, monotonicNow)
            VpnProtocol.OPENVPN ->
                _status.value = ConnectionStatus.Error("OpenVPN connections are started via the OpenVPN engine.")
        }
    }

    private suspend fun connectWireGuard(profile: ServerProfile, monotonicNow: Long) {
        _status.value = ConnectionStatus.Connecting(profile)
        try {
            val config = withContext(Dispatchers.IO) {
                Config.parse(BufferedReader(StringReader(profile.config)))
            }
            val tunnel = NxTunnel("nxvpn") { /* state pushed below */ }
            withContext(Dispatchers.IO) {
                backend.setState(tunnel, Tunnel.State.UP, config)
            }
            activeTunnel = tunnel
            _status.value = ConnectionStatus.Connected(profile, monotonicNow)
        } catch (t: Throwable) {
            activeTunnel = null
            _status.value = ConnectionStatus.Error(t.message ?: "Failed to connect")
            throw t
        }
    }

    suspend fun disconnect() {
        // Route to whichever backend owns the active tunnel.
        if (_status.value.activeProfile?.protocol == VpnProtocol.OPENVPN) {
            openVpn.disconnect()
            return
        }
        val tunnel = activeTunnel ?: run {
            _status.value = ConnectionStatus.Disconnected
            return
        }
        _status.value = ConnectionStatus.Disconnecting
        try {
            withContext(Dispatchers.IO) {
                backend.setState(tunnel, Tunnel.State.DOWN, null)
            }
        } finally {
            activeTunnel = null
            _status.value = ConnectionStatus.Disconnected
        }
    }

    /** Reads live byte counters from the backend; returns zeros when no tunnel is up. */
    suspend fun currentTraffic(): TrafficStats {
        val tunnel = activeTunnel ?: return TrafficStats()
        return withContext(Dispatchers.IO) {
            runCatching {
                val stats = backend.getStatistics(tunnel)
                TrafficStats(rxBytes = stats.totalRx(), txBytes = stats.totalTx())
            }.getOrDefault(TrafficStats())
        }
    }
}
