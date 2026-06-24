package com.nxvpn.app.vpn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import com.nxvpn.app.data.model.ConnectionStatus
import com.nxvpn.app.data.model.ServerProfile
import de.blinkt.openvpn.api.IOpenVPNAPIService
import de.blinkt.openvpn.api.IOpenVPNStatusCallback
import kotlinx.coroutines.CompletableDeferred

/**
 * Drives the user's installed "OpenVPN for Android" engine over its external AIDL API.
 *
 * NX-VPN never tunnels OpenVPN traffic itself; it hands an inline `.ovpn` config to the
 * engine app, which owns the actual VpnService. The connection requires two one-time
 * consent steps (external-API permission + the system VPN dialog), each returning an
 * [Intent] that must be launched from an Activity — hence the [prepareApiPermission] /
 * [prepareVpnService] split that the caller orchestrates.
 *
 * Status updates from the engine are mapped onto [ConnectionStatus] and pushed through
 * [setStatus], which is wired to the same StateFlow the WireGuard path uses.
 */
class OpenVpnConnector(
    context: Context,
    private val setStatus: (ConnectionStatus) -> Unit,
) {
    private val appContext = context.applicationContext

    private var service: IOpenVPNAPIService? = null
    private var bound = false
    private var pendingService: CompletableDeferred<IOpenVPNAPIService>? = null

    /** The profile we are currently bringing up; used to label status updates. */
    private var activeProfile: ServerProfile? = null

    private val statusCallback = object : IOpenVPNStatusCallback.Stub() {
        override fun newStatus(uuid: String?, state: String?, message: String?, level: String?) {
            val profile = activeProfile ?: return
            when (state) {
                "CONNECTED" ->
                    setStatus(ConnectionStatus.Connected(profile, SystemClock.elapsedRealtime()))
                "DISCONNECTED", "NOPROCESS" -> {
                    setStatus(ConnectionStatus.Disconnected)
                    activeProfile = null
                }
                "AUTH_FAILED" ->
                    setStatus(ConnectionStatus.Error(message?.takeIf { it.isNotBlank() } ?: "Authentication failed"))
                else ->
                    // CONNECTING, WAIT, AUTH, GET_CONFIG, ASSIGN_IP, RECONNECTING, RESOLVE, TCP_CONNECT, ...
                    setStatus(ConnectionStatus.Connecting(profile))
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val s = IOpenVPNAPIService.Stub.asInterface(binder)
            service = s
            pendingService?.complete(s)
            pendingService = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    /** True when the OpenVPN for Android engine is installed and bindable. */
    fun isEngineInstalled(): Boolean = runCatching {
        appContext.packageManager.getPackageInfo(ENGINE_PACKAGE, 0)
    }.isSuccess

    /** Binds to the engine service, suspending until the connection is established. */
    suspend fun awaitService(): IOpenVPNAPIService {
        service?.let { return it }
        val deferred = CompletableDeferred<IOpenVPNAPIService>()
        pendingService = deferred
        val intent = Intent(IOpenVPNAPIService::class.java.name).setPackage(ENGINE_PACKAGE)
        bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            pendingService = null
            error("Could not bind to OpenVPN for Android")
        }
        return deferred.await()
    }

    /** Marks the start of a connection attempt and surfaces a Connecting state immediately. */
    fun beginConnecting(profile: ServerProfile) {
        activeProfile = profile
        setStatus(ConnectionStatus.Connecting(profile))
    }

    /**
     * Returns an Intent the Activity must launch to grant NX-VPN external-API access,
     * or null if access was already granted.
     */
    fun prepareApiPermission(): Intent? = service?.prepare(appContext.packageName)

    /** Subscribes to engine status updates. Call after API permission is granted. */
    fun registerStatusCallback() {
        runCatching { service?.registerStatusCallback(statusCallback) }
    }

    /**
     * Returns an Intent the Activity must launch for the system VPN consent dialog,
     * or null if the engine already holds VPN permission.
     */
    fun prepareVpnService(): Intent? = service?.prepareVPNService()

    /** Starts the tunnel from the profile's inline config. */
    fun startVpn(profile: ServerProfile) {
        activeProfile = profile
        service?.startVPN(profile.config)
    }

    /** Aborts an in-flight attempt (e.g. the user declined a consent dialog). */
    fun cancel() {
        activeProfile = null
        setStatus(ConnectionStatus.Disconnected)
    }

    /** Tears the tunnel down. */
    fun disconnect() {
        setStatus(ConnectionStatus.Disconnecting)
        runCatching { service?.disconnect() }
        activeProfile = null
        setStatus(ConnectionStatus.Disconnected)
    }

    companion object {
        const val ENGINE_PACKAGE = "de.blinkt.openvpn"
    }
}
