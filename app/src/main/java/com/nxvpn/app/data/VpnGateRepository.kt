package com.nxvpn.app.data

import android.util.Base64
import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.data.model.VpnProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches ready-to-use public OpenVPN servers from the VPN Gate volunteer network
 * (a free academic project run by the University of Tsukuba, Japan).
 *
 * The endpoint returns a CSV where each data row carries an inline `.ovpn` config
 * (with embedded certificates) in its last column, which the OpenVPN for Android
 * engine can consume directly via [com.nxvpn.app.vpn.OpenVpnConnector].
 *
 * These servers are operated by volunteers: they come and go, may be slow, and should
 * be treated as untrusted. They are offered purely as a convenience next to the
 * import-your-own-config flow.
 */
class VpnGateRepository(
    private val apiUrl: String = "https://www.vpngate.net/api/iphone/",
) {

    /** Downloads the server list and returns the fastest [limit] servers, sorted by speed. */
    suspend fun fetch(limit: Int = 60): Result<List<ServerProfile>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 25_000
                setRequestProperty("User-Agent", "NX-VPN")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    error("VPN Gate returned HTTP ${connection.responseCode}")
                }
                val csv = connection.inputStream.bufferedReader().use { it.readText() }
                parse(csv, limit)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parse(csv: String, limit: Int): List<ServerProfile> {
        val servers = ArrayList<ServerProfile>()
        for (line in csv.lineSequence()) {
            // Skip the banner ("*vpn servers"), the "#HostName,..." header and the trailing "*".
            if (line.isBlank() || line.startsWith("*") || line.startsWith("#")) continue

            val cols = line.split(",")
            if (cols.size < 15) continue

            val hostName = cols[0].trim()
            val ip = cols[1].trim()
            val ping = cols[3].trim().toIntOrNull()
            val speed = cols[4].trim().toLongOrNull()
            val countryLong = cols[5].trim()
            val countryShort = cols[6].trim()
            // The base64 OpenVPN config is always the final column, even if the (free-text)
            // Message column happens to contain commas — so read it from the end.
            val configBase64 = cols.last().trim()
            if (hostName.isEmpty() || configBase64.isEmpty()) continue

            val config = runCatching {
                String(Base64.decode(configBase64, Base64.DEFAULT))
            }.getOrNull() ?: continue
            if (!config.contains("remote ", ignoreCase = true)) continue

            servers += ServerProfile(
                id = "vpngate:$hostName",
                name = countryLong.ifBlank { countryShort.ifBlank { hostName } },
                protocol = VpnProtocol.OPENVPN,
                config = config,
                countryCode = countryShort,
                flagEmoji = flagFor(countryShort),
                endpoint = ip,
                pingMs = ping,
                speedBps = speed,
                isPublic = true,
            )
        }
        return servers.sortedByDescending { it.speedBps ?: 0L }.take(limit)
    }

    /** Turns a 2-letter ISO country code into its flag emoji (regional indicator symbols). */
    private fun flagFor(countryShort: String): String {
        val cc = countryShort.uppercase()
        if (cc.length != 2 || cc.any { it !in 'A'..'Z' }) return "🌐"
        return buildString {
            cc.forEach { appendCodePoint(0x1F1E6 + (it - 'A')) }
        }
    }
}
