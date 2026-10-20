package com.iptv.app.core.network

import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import java.security.MessageDigest

/**
 * Stateless parser for the IPTV-org .m3u playlist format.
 * Filters and normalizes channels for Spanish-speaking Latin America & Spain.
 */
object M3UParser {

    private val EXTINF_REGEX = Regex("""#EXTINF:-?\d+(.*)$""")
    private val ATTR_REGEX = Regex("""(\w[\w-]*)="([^"]*)"""")
    private val USER_AGENT_OPT = "#EXTVLCOPT:http-user-agent="
    private val REFERRER_OPT = "#EXTVLCOPT:http-referrer="

    /** Official Spanish-speaking country ISO codes + US Hispanic / International feeds. */
    val HISPANIC_COUNTRIES = setOf(
        "MX", "AR", "CL", "CO", "VE", "PE", "ES", "EC", "DO",
        "UY", "PY", "BO", "CR", "PA", "GT", "HN", "SV", "NI", "PR", "CU", "GQ"
    )

    private val BLOCKED_FOREIGN_COUNTRIES = setOf(
        "CH", "RU", "DE", "FR", "IT", "PT", "BR", "CN", "JP", "KR", "IN", "TR", "PL", "NL", "RO", "UA", "GR", "SE"
    )

    private val ENGLISH_ONLY_MARKERS = listOf(
        "bbc ", "nbc ", "cbs ", "abc news", "fox weather", "sky news", "al jazeera english",
        "nhk world", "france 24 english", "cgtn", "rt english", "bloomberg"
    )

    fun parse(rawContent: String): List<Channel> {
        val lines = rawContent.lines()
        val channels = mutableListOf<Channel>()

        var pendingAttrs: Map<String, String>? = null
        var pendingName: String? = null
        var pendingUserAgent: String? = null
        var pendingReferrer: String? = null

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.startsWith("#EXTINF:") -> {
                    pendingUserAgent = null
                    pendingReferrer = null

                    val match = EXTINF_REGEX.find(trimmed)
                    val attrPart = match?.groupValues?.get(1) ?: ""

                    pendingAttrs = ATTR_REGEX.findAll(attrPart)
                        .associate { it.groupValues[1] to it.groupValues[2] }

                    pendingName = trimmed.substringAfterLast(',').trim()
                }

                trimmed.startsWith(USER_AGENT_OPT) -> {
                    pendingUserAgent = trimmed.removePrefix(USER_AGENT_OPT).trim()
                }

                trimmed.startsWith(REFERRER_OPT) -> {
                    pendingReferrer = trimmed.removePrefix(REFERRER_OPT).trim()
                }

                trimmed.startsWith("#") || trimmed.isEmpty() -> Unit

                else -> {
                    val streamUrl = trimmed
                    val attrs = pendingAttrs ?: emptyMap()
                    val rawName = pendingName ?: "Canal desconocido"

                    if (streamUrl.isNotBlank()) {
                        val tvgId = attrs["tvg-id"] ?: ""
                        val logoUrl = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                        val groupTitle = attrs["group-title"] ?: ""
                        val countryCode = extractCountryCode(tvgId, rawName)

                        if (shouldIncludeSpanishChannel(rawName, countryCode)) {
                            val category = Category.fromGroupTitle(groupTitle, rawName)
                            channels += Channel(
                                id = stableId(tvgId, streamUrl),
                                tvgId = tvgId,
                                dialNumber = 0,
                                name = cleanChannelName(rawName),
                                logoUrl = logoUrl,
                                category = category,
                                countryCode = countryCode,
                                streamUrl = streamUrl,
                                userAgent = pendingUserAgent,
                                referrer = pendingReferrer,
                            )
                        }
                    }

                    pendingAttrs = null
                    pendingName = null
                    pendingUserAgent = null
                    pendingReferrer = null
                }
            }
        }

        // Prioritize Latin America & Spain channels first
        return channels.sortedWith(
            compareBy<Channel> { it.countryCode !in HISPANIC_COUNTRIES }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Extracts a clean 2-letter ISO country code from a tvg-id like "EnerGeek.cl@SD" → "CL".
     */
    private fun extractCountryCode(tvgId: String, channelName: String): String {
        val withoutFeed = tvgId.substringBefore('@').trim()
        val suffix = withoutFeed.substringAfterLast('.', missingDelimiterValue = "").uppercase()
        if (suffix.length == 2 && suffix.all { it.isLetter() }) {
            return if (suffix == "UK") "GB" else suffix
        }

        val nameLower = channelName.lowercase()
        return when {
            nameLower.contains("méxico") || nameLower.contains("mexico") || nameLower.contains("azteca") || nameLower.contains("televisa") -> "MX"
            nameLower.contains("argentina") || nameLower.contains("telefe") -> "AR"
            nameLower.contains("chile") || nameLower.contains("energeek") -> "CL"
            nameLower.contains("colombia") || nameLower.contains("caracol") || nameLower.contains("rcn") -> "CO"
            nameLower.contains("venezuela") || nameLower.contains("venevision") || nameLower.contains("televen") || nameLower.contains("globovision") -> "VE"
            nameLower.contains("perú") || nameLower.contains("peru") -> "PE"
            nameLower.contains("españa") || nameLower.contains("rtve") || nameLower.contains("atres") -> "ES"
            nameLower.contains("ecuador") -> "EC"
            nameLower.contains("dominicana") -> "DO"
            nameLower.contains("uruguay") -> "UY"
            nameLower.contains("paraguay") -> "PY"
            nameLower.contains("bolivia") -> "BO"
            nameLower.contains("costa rica") -> "CR"
            nameLower.contains("panamá") || nameLower.contains("panama") -> "PA"
            nameLower.contains("guatemala") -> "GT"
            nameLower.contains("honduras") -> "HN"
            nameLower.contains("salvador") -> "SV"
            nameLower.contains("nicaragua") -> "NI"
            nameLower.contains("puerto rico") -> "PR"
            nameLower.contains("cuba") -> "CU"
            else -> "LATAM"
        }
    }

    private fun shouldIncludeSpanishChannel(name: String, countryCode: String): Boolean {
        if (countryCode in BLOCKED_FOREIGN_COUNTRIES) return false
        val lower = name.lowercase()
        if (ENGLISH_ONLY_MARKERS.any { lower.contains(it) }) return false
        return true
    }

    private fun cleanChannelName(rawName: String): String {
        return rawName
            .replace(Regex("""\s*\[.*?]"""), "")
            .trim()
            .ifBlank { rawName }
    }

    private fun stableId(tvgId: String, streamUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("$tvgId|$streamUrl".toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }
}
