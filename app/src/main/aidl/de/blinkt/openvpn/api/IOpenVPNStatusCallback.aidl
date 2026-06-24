package de.blinkt.openvpn.api;

/**
 * Callback interface used by OpenVPN for Android to send status updates back to NX-VPN.
 * One-way so the server never blocks waiting for us.
 */
interface IOpenVPNStatusCallback {
    /**
     * Called when the service has a new status for you.
     */
    oneway void newStatus(in String uuid, in String state, in String message, in String level);
}
