package com.iptv.app.core.network

import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel
import java.security.MessageDigest

/**
 * Stateless parser for the IPTV-org .m3u playlist format.
 *
 * Parses the raw string content of a .m3u file and returns a list of [Channel] domain objects.
 * Extraction rules:
 *  - #EXTINF line:  tvg-id, tvg-logo, group-title, display name (after last comma)
 *  - #EXTVLCOPT:   http-user-agent= and http-referrer=
 *  - Stream URL:    next non-comment, non-empty line after #EXTINF block
 */
object M3UParser {

    private val EXTINF_REGEX = Regex("""#EXTINF:-?\d+(.*)$""")
    private val ATTR_REGEX = Regex("""(\w[\w-]*)="([^"]*)"""")
    private val USER_AGENT_OPT = "#EXTVLCOPT:http-user-agent="
    private val REFERRER_OPT = "#EXTVLCOPT:http-referrer="

    /**
     * Parses [rawContent] and emits [Channel] objects.
     * Call from [kotlinx.coroutines.Dispatchers.IO].
     *
     * @param rawContent Full UTF-8 content of the .m3u file.
     * @return List of parsed [Channel] domain objects (entries with blank URLs are skipped).
     */
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
                    // Reset pending state for each new entry
                    pendingUserAgent = null
                    pendingReferrer = null

                    val match = EXTINF_REGEX.find(trimmed)
                    val attrPart = match?.groupValues?.get(1) ?: ""

                    // Parse all key="value" pairs
                    pendingAttrs = ATTR_REGEX.findAll(attrPart)
                        .associate { it.groupValues[1] to it.groupValues[2] }

                    // Display name is the last comma-separated segment
                    pendingName = trimmed.substringAfterLast(',').trim()
                }

                trimmed.startsWith(USER_AGENT_OPT) -> {
                    pendingUserAgent = trimmed.removePrefix(USER_AGENT_OPT).trim()
                }

                trimmed.startsWith(REFERRER_OPT) -> {
                    pendingReferrer = trimmed.removePrefix(REFERRER_OPT).trim()
                }

                trimmed.startsWith("#") || trimmed.isEmpty() -> {
                    // Skip other directives and blank lines (do NOT clear pending state)
                    Unit
                }

                else -> {
                    // Non-comment, non-empty line → stream URL
                    val streamUrl = trimmed
                    val attrs = pendingAttrs ?: emptyMap()
                    val name = pendingName ?: "Canal desconocido"

                    if (streamUrl.isNotBlank()) {
                        val tvgId = attrs["tvg-id"] ?: ""
                        val logoUrl = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                        val groupTitle = attrs["group-title"] ?: ""
                        val category = Category.fromGroupTitle(groupTitle)
                        val countryCode = extractCountryCode(tvgId)

                        channels += Channel(
                            id = stableId(tvgId, streamUrl),
                            tvgId = tvgId,
                            dialNumber = 0, // Assigned by DialNumberAssigner after parsing
                            name = name,
                            logoUrl = logoUrl,
                            category = category,
                            countryCode = countryCode,
                            streamUrl = streamUrl,
                            userAgent = pendingUserAgent,
                            referrer = pendingReferrer,
                        )
                    }

                    // Reset pending state
                    pendingAttrs = null
                    pendingName = null
                    pendingUserAgent = null
                    pendingReferrer = null
                }
            }
        }

        return channels
    }

    /**
     * Extracts a 2-letter uppercase country code from a tvg-id like "EnerGeek.cl" → "CL".
     * Falls back to "XX" for IDs without a recognized TLD suffix.
     */
    private fun extractCountryCode(tvgId: String): String {
        val suffix = tvgId.substringAfterLast('.', missingDelimiterValue = "")
        return if (suffix.length in 2..3) suffix.uppercase() else "XX"
    }

    /**
     * Produces a short stable ID by hex-encoding the first 8 bytes of SHA-256(tvgId + streamUrl).
     * Collision probability is negligible for typical playlist sizes (~5 000 entries).
     */
    private fun stableId(tvgId: String, streamUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("$tvgId|$streamUrl".toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }
}
