package com.nxvpn.app.data

import com.nxvpn.app.data.model.ServerProfile
import com.nxvpn.app.data.model.VpnProtocol
import java.util.UUID

/** Builds a [ServerProfile] from raw `.conf` / `.ovpn` text, pulling out display metadata. */
object ConfigImporter {

    fun fromText(raw: String, fallbackName: String? = null): Result<ServerProfile> {
        val config = raw.trim()
        if (config.isEmpty()) return Result.failure(IllegalArgumentException("Config is empty"))

        val protocol = VpnProtocol.detect(config)
        val endpoint = extractEndpoint(config, protocol)
        val (countryCode, flag) = guessCountry(config + " " + (fallbackName ?: ""))
        val name = (fallbackName?.takeIf { it.isNotBlank() }
            ?: endpoint.substringBefore(':').takeIf { it.isNotBlank() }
            ?: "${protocol.displayName} server")

        return Result.success(
            ServerProfile(
                id = UUID.randomUUID().toString(),
                name = name,
                protocol = protocol,
                config = config,
                countryCode = countryCode,
                flagEmoji = flag,
                endpoint = endpoint,
            )
        )
    }

    private fun extractEndpoint(config: String, protocol: VpnProtocol): String = when (protocol) {
        VpnProtocol.WIREGUARD ->
            Regex("(?im)^\\s*Endpoint\\s*=\\s*(.+)$").find(config)?.groupValues?.get(1)?.trim().orEmpty()
        VpnProtocol.OPENVPN ->
            Regex("(?im)^\\s*remote\\s+(\\S+)(?:\\s+(\\d+))?").find(config)?.let { m ->
                val host = m.groupValues[1]
                val port = m.groupValues[2]
                if (port.isNotBlank()) "$host:$port" else host
            }.orEmpty()
    }

    /** Very small flag lookup; covers the common cases people import. Everything else gets a globe. */
    private fun guessCountry(text: String): Pair<String, String> {
        val haystack = text.lowercase()
        val table = listOf(
            Triple(listOf("united states", "usa", "u.s.", "-us", "_us", "america"), "US", "🇺🇸"),
            Triple(listOf("germany", "deutschland", "-de", "frankfurt"), "DE", "🇩🇪"),
            Triple(listOf("netherlands", "amsterdam", "-nl"), "NL", "🇳🇱"),
            Triple(listOf("united kingdom", "london", "-uk", "-gb"), "GB", "🇬🇧"),
            Triple(listOf("turkey", "türkiye", "turkiye", "istanbul", "-tr"), "TR", "🇹🇷"),
            Triple(listOf("france", "paris", "-fr"), "FR", "🇫🇷"),
            Triple(listOf("japan", "tokyo", "-jp"), "JP", "🇯🇵"),
            Triple(listOf("singapore", "-sg"), "SG", "🇸🇬"),
            Triple(listOf("canada", "-ca"), "CA", "🇨🇦"),
        )
        for ((keys, code, flag) in table) {
            if (keys.any { it in haystack }) return code to flag
        }
        return "" to "🌐"
    }
}
