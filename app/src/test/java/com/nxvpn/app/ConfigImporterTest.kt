package com.nxvpn.app

import com.nxvpn.app.data.ConfigImporter
import com.nxvpn.app.data.model.VpnProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigImporterTest {

    private val wireGuardConfig = """
        [Interface]
        PrivateKey = aGVsbG8gd29ybGQgdGhpcyBpcyBub3QgcmVhbCBrZXk=
        Address = 10.0.0.2/32
        DNS = 1.1.1.1

        [Peer]
        PublicKey = cHVibGljIGtleSBwbGFjZWhvbGRlciB2YWx1ZSBoZXJl
        Endpoint = us-east.example.com:51820
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()

    private val openVpnConfig = """
        client
        dev tun
        proto udp
        remote vpn.example.com 1194
        resolv-retry infinite
    """.trimIndent()

    @Test
    fun `detects wireguard config`() {
        assertEquals(VpnProtocol.WIREGUARD, VpnProtocol.detect(wireGuardConfig))
    }

    @Test
    fun `detects openvpn config`() {
        assertEquals(VpnProtocol.OPENVPN, VpnProtocol.detect(openVpnConfig))
    }

    @Test
    fun `imports wireguard endpoint and country`() {
        val profile = ConfigImporter.fromText(wireGuardConfig, fallbackName = "US East").getOrThrow()
        assertEquals(VpnProtocol.WIREGUARD, profile.protocol)
        assertEquals("us-east.example.com:51820", profile.endpoint)
        assertEquals("US", profile.countryCode)
    }

    @Test
    fun `imports openvpn endpoint`() {
        val profile = ConfigImporter.fromText(openVpnConfig, fallbackName = null).getOrThrow()
        assertEquals(VpnProtocol.OPENVPN, profile.protocol)
        assertEquals("vpn.example.com:1194", profile.endpoint)
    }

    @Test
    fun `empty config fails`() {
        assertTrue(ConfigImporter.fromText("   ", fallbackName = null).isFailure)
    }
}
