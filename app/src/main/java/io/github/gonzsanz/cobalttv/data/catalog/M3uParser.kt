package io.github.gonzsanz.cobalttv.data.catalog

import java.net.URLDecoder
import java.security.MessageDigest

/**
 * Tolerant M3U / M3U8 playlist parser.
 *
 * IPTV lists in the wild are messy: missing #EXTM3U headers, CRLF, BOMs,
 * comments between entries, quoted attribute values with commas, bare URLs,
 * duplicate entries and unknown directives. The parser never throws on a bad
 * line; it simply skips it and keeps the entries it can understand.
 *
 * It is deliberately free of Android classes so it can be unit tested on the
 * JVM without Robolectric.
 */
object M3uParser {

    private val attributeRegex = Regex(
        """([A-Za-z0-9_.-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s]+))""",
    )

    fun parse(rawText: String): List<Channel> {
        val text = rawText.removePrefix("\uFEFF")
        val channels = ArrayList<Channel>()
        val seen = HashSet<String>()
        var pending: PendingEntry? = null

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pending = parseExtInf(line.substringAfter(':'))
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    val group = line.substringAfter(':').trim()
                    if (group.isNotEmpty()) pending?.group = group
                }

                line.startsWith("#") -> {
                    // #EXTM3U, #EXTVLCOPT, #KODIPROP, #PLAYLIST... not needed yet.
                }

                else -> {
                    val entry = pending
                    pending = null
                    if (entry == null) {
                        // Bare URL lists exist too; give them a readable name.
                        if (line.contains("://")) {
                            addChannel(
                                channels = channels,
                                seen = seen,
                                name = nameFromUrl(line),
                                url = line,
                                group = null,
                                attributes = emptyMap(),
                            )
                        }
                    } else {
                        addChannel(
                            channels = channels,
                            seen = seen,
                            name = entry.name,
                            url = line,
                            group = entry.group,
                            attributes = entry.attributes,
                        )
                    }
                }
            }
        }

        return channels
    }

    private fun parseExtInf(header: String): PendingEntry {
        val (attributePart, rawName) = splitName(header)
        val attributes = parseAttributes(attributePart)
        val name = rawName.trim()
            .ifEmpty { attributes["tvg-name"]?.trim().orEmpty() }
        return PendingEntry(
            name = name,
            attributes = attributes,
            group = attributes["group-title"]?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: attributes["group"]?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    /**
     * Splits `-1 tvg-id="x" group-title="Cine, Acción",Name, with comma`
     * at the first comma outside quotes.
     */
    private fun splitName(header: String): Pair<String, String> {
        var inDouble = false
        var inSingle = false
        header.forEachIndexed { index, char ->
            when (char) {
                '"' -> if (!inSingle) inDouble = !inDouble
                '\'' -> if (!inDouble) inSingle = !inSingle
                ',' -> if (!inDouble && !inSingle) {
                    return header.substring(0, index) to header.substring(index + 1)
                }
            }
        }
        return header to ""
    }

    private fun parseAttributes(value: String): Map<String, String> {
        val attributes = LinkedHashMap<String, String>()
        attributeRegex.findAll(value).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val raw = match.groupValues[2]
                .ifEmpty { match.groupValues[3] }
                .ifEmpty { match.groupValues[4] }
            attributes[key] = raw
        }
        return attributes
    }

    private fun addChannel(
        channels: MutableList<Channel>,
        seen: MutableSet<String>,
        name: String,
        url: String,
        group: String?,
        attributes: Map<String, String>,
    ) {
        val finalName = name.trim().ifEmpty { nameFromUrl(url) }
        val finalGroup = group?.trim()?.takeIf { it.isNotEmpty() } ?: "Sin categoría"
        val dedupeKey = "$finalName\u0000$url"
        if (!seen.add(dedupeKey)) return

        channels += Channel(
            id = stableId(dedupeKey),
            name = finalName,
            streamUrl = url.trim(),
            group = finalGroup,
            logoUrl = attributes["tvg-logo"]?.trim()?.takeIf { it.isNotEmpty() },
            tvgId = attributes["tvg-id"]?.trim()?.takeIf { it.isNotEmpty() },
            number = attributes["tvg-chno"]?.trim()?.toIntOrNull(),
        )
    }

    private fun stableId(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        return buildString(25) {
            append("ch-")
            digest.take(12).forEach { byte -> append("%02x".format(byte.toInt() and 0xFF)) }
        }
    }

    private fun nameFromUrl(url: String): String {
        val withoutQuery = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val segment = withoutQuery.substringAfterLast('/')
        val base = segment.substringBeforeLast('.', segment)
        val decoded = runCatching { URLDecoder.decode(base, "UTF-8") }.getOrDefault(base)
        return decoded
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifEmpty { "Canal" }
    }

    private data class PendingEntry(
        val name: String,
        val attributes: Map<String, String>,
        var group: String?,
    )
}
