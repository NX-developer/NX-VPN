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

    private data class CountryRule(
        val phrases: List<String>,
        val codes: List<String>,
        val code: String,
        val flag: String,
    )

    private val countryRules = listOf(
        CountryRule(listOf("united states", "america"), listOf("us", "usa"), "US", "🇺🇸"),
        CountryRule(listOf("germany", "deutschland", "frankfurt"), listOf("de", "ger"), "DE", "🇩🇪"),
        CountryRule(listOf("netherlands", "amsterdam"), listOf("nl"), "NL", "🇳🇱"),
        CountryRule(listOf("united kingdom", "london"), listOf("uk", "gb"), "GB", "🇬🇧"),
        CountryRule(listOf("turkey", "türkiye", "turkiye", "istanbul"), listOf("tr"), "TR", "🇹🇷"),
        CountryRule(listOf("france", "paris"), listOf("fr"), "FR", "🇫🇷"),
        CountryRule(listOf("japan", "tokyo"), listOf("jp"), "JP", "🇯🇵"),
        CountryRule(listOf("singapore"), listOf("sg"), "SG", "🇸🇬"),
        CountryRule(listOf("canada"), listOf("ca"), "CA", "🇨🇦"),
    )

    /** Very small flag lookup; covers the common cases people import. Everything else gets a globe. */
    private fun guessCountry(text: String): Pair<String, String> {
        val haystack = text.lowercase()
        // Whole-word tokens (split on anything that isn't a letter/digit) so a 2-letter
        // country code like "us" in "US East" or "us-east.example.com" is matched, while
        // random substrings inside base64 keys are not.
        val tokens = haystack.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()

        for (rule in countryRules) {
            val matched = rule.phrases.any { it in haystack } || rule.codes.any { it in tokens }
            if (matched) return rule.code to rule.flag
        }
        return "" to "🌐"
    }
}
