package io.github.gonzsanz.cobalttv.data.epg

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

private const val EPG_WINDOW_FUTURE_MS = 24L * 60 * 60 * 1000
private const val EPG_DEFAULT_DURATION_MS = 60L * 60 * 1000
private const val EPG_MAX_TITLE_CHARS = 120
private const val EPG_MAX_PROGRAMMES = 400_000
private val whitespaceRegex = Regex("\\s+")

/**
 * Streaming XMLTV parser for now/next.
 *
 * Full guides can be tens of megabytes, so programmes are filtered while they
 * are read: only wanted channel IDs and the next 24 hours are kept. SAX is
 * available both on Android and on the JVM, which keeps this testable without
 * Robolectric.
 */
object EpgParser {

    fun parse(
        input: InputStream,
        channelIds: Set<String>,
        nowMs: Long = System.currentTimeMillis(),
    ): Map<String, List<EpgProgramme>> {
        if (channelIds.isEmpty()) return emptyMap()
        val handler = EpgSaxHandler(channelIds = channelIds, nowMs = nowMs)
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
            hardenAgainstExternalEntities()
        }
        factory.newSAXParser().parse(InputSource(input), handler)
        return handler.result()
    }

    private fun SAXParserFactory.hardenAgainstExternalEntities() {
        listOf(
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
            "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false,
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
        ).forEach { (feature, value) ->
            runCatching { setFeature(feature, value) }
        }
    }
}

internal fun parseXmltvTime(raw: String?): Long? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val patterns = if (value.contains(' ')) {
        listOf(
            "yyyyMMddHHmmss Z",
            "yyyyMMddHHmm Z",
            "yyyyMMddHHmmss XXX",
            "yyyyMMddHHmm XXX",
        )
    } else {
        listOf("yyyyMMddHHmmss", "yyyyMMddHHmm")
    }
    for (pattern in patterns) {
        val format = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = runCatching { format.parse(value)?.time }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

private class EpgSaxHandler(
    private val channelIds: Set<String>,
    private val nowMs: Long,
) : DefaultHandler() {
    private val programmes = LinkedHashMap<String, MutableList<EpgProgramme>>()
    private var current: MutableProgramme? = null
    private var titleBuilder: StringBuilder? = null
    private var stored = 0

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
    ) {
        when (qName?.lowercase() ?: localName?.lowercase()) {
            "programme" -> {
                val channelId = attributes?.getValue("channel")?.trim().orEmpty()
                if (channelId.isEmpty() || channelId !in channelIds) {
                    current = null
                    return
                }
                val startMs = parseXmltvTime(attributes?.getValue("start")) ?: return
                current = MutableProgramme(
                    channelId = channelId,
                    startMs = startMs,
                    stopMs = parseXmltvTime(attributes?.getValue("stop")),
                )
            }

            "title" -> {
                if (current != null) titleBuilder = StringBuilder()
            }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        val builder = titleBuilder ?: return
        val remaining = EPG_MAX_TITLE_CHARS - builder.length
        if (remaining > 0) {
            builder.append(ch, start, minOf(length, remaining))
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (qName?.lowercase() ?: localName?.lowercase()) {
            "title" -> {
                val text = titleBuilder?.toString()
                    ?.trim()
                    ?.replace(whitespaceRegex, " ")
                val programme = current
                if (programme != null && programme.title.isNullOrEmpty() && !text.isNullOrEmpty()) {
                    programme.title = text
                }
                titleBuilder = null
            }

            "programme" -> {
                val programme = current
                current = null
                titleBuilder = null
                if (programme == null || stored >= EPG_MAX_PROGRAMMES) return
                val title = programme.title?.trim().orEmpty()
                if (title.isEmpty()) return
                val stopMs = programme.stopMs ?: (programme.startMs + EPG_DEFAULT_DURATION_MS)
                if (stopMs <= programme.startMs ||
                    stopMs <= nowMs ||
                    programme.startMs >= nowMs + EPG_WINDOW_FUTURE_MS
                ) {
                    return
                }
                programmes.getOrPut(programme.channelId) { ArrayList() } += EpgProgramme(
                    channelId = programme.channelId,
                    title = title,
                    startMs = programme.startMs,
                    stopMs = stopMs,
                )
                stored++
            }
        }
    }

    fun result(): Map<String, List<EpgProgramme>> = programmes.mapValues { (_, list) ->
        list.sortedWith(compareBy({ it.startMs }, { it.stopMs }))
            .distinctBy { it.startMs to it.title }
    }
}

private class MutableProgramme(
    val channelId: String,
    val startMs: Long,
    val stopMs: Long?,
) {
    var title: String? = null
}
