package io.github.gonzsanz.cobalttv.data.xtream

import io.github.gonzsanz.cobalttv.data.catalog.Channel
import io.github.gonzsanz.cobalttv.data.catalog.SeriesDetail
import io.github.gonzsanz.cobalttv.data.catalog.SeriesEpisode
import io.github.gonzsanz.cobalttv.data.catalog.SeriesItem
import io.github.gonzsanz.cobalttv.data.catalog.VodDetail
import io.github.gonzsanz.cobalttv.data.catalog.VodItem
import java.security.MessageDigest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure Xtream response normalization. It has no network code so it can be
 * unit tested with JSON fixtures on the JVM.
 *
 * Xtream servers are not fully consistent: identifiers can be numbers or
 * strings, categories may be missing, and optional metadata is often null or
 * the literal string "null". The parser skips unusable entries instead of
 * failing the whole import.
 */
object XtreamJson {

    private val inactiveStatuses = setOf(
        "disabled",
        "banned",
        "expired",
        "inactive",
        "suspended",
    )
    private val extensionRegex = Regex("[A-Za-z0-9]{1,8}")
    private val knownEndpoints = setOf(
        "player_api.php",
        "panel_api.php",
        "get.php",
        "xmltv.php",
    )

    fun normalizeServer(raw: String): HttpUrl {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            throw XtreamException("Escribe la URL del servidor Xtream.")
        }
        val candidate = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val parsed = candidate.toHttpUrlOrNull()
            ?: throw XtreamException(
                "La URL del servidor no es válida. Debe empezar por http:// o https://.",
            )
        if (parsed.scheme != "http" && parsed.scheme != "https") {
            throw XtreamException("La URL del servidor debe usar http:// o https://.")
        }
        // Users often paste player_api.php or a URL with a token. Strip known
        // endpoints but keep a subdirectory, because some servers live under
        // /iptv or a similar path.
        val pathSegments = parsed.pathSegments
            .filter { it.isNotEmpty() }
            .dropLastWhile { it.lowercase() in knownEndpoints }
        val builder = parsed.newBuilder()
            .username("")
            .password("")
            .encodedPath("/")
            .query(null)
            .fragment(null)
        pathSegments.forEach { builder.addPathSegment(it) }
        return builder.build()
    }

    fun apiUrl(
        base: HttpUrl,
        credentials: XtreamCredentials,
        action: String?,
    ): HttpUrl {
        val builder = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", credentials.username)
            .addQueryParameter("password", credentials.password)
        if (action != null) builder.addQueryParameter("action", action)
        return builder.build()
    }

    fun streamUrl(
        base: HttpUrl,
        type: String,
        credentials: XtreamCredentials,
        id: String,
        extension: String,
    ): String = base.newBuilder()
        .addPathSegment(type)
        .addPathSegment(credentials.username)
        .addPathSegment(credentials.password)
        .addPathSegment("$id.$extension")
        .build()
        .toString()

    fun parseLogin(root: JSONObject): XtreamAccount {
        val user = root.optJSONObject("user_info")
            ?: throw XtreamException(
                "El servidor no devolvió información de la cuenta Xtream.",
            )
        return accountFrom(user)
    }

    /** Validates action responses that repeat user_info, when present. */
    fun validateAccount(root: JSONObject) {
        val user = root.optJSONObject("user_info") ?: return
        accountFrom(user)
    }

    fun parseCategories(value: Any?): Map<String, String> {
        val array = asArray(value) ?: return emptyMap()
        val categories = LinkedHashMap<String, String>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = text(item, "category_id") ?: continue
            categories[id] = text(item, "category_name") ?: "Sin categoría"
        }
        return categories
    }

    fun parseLiveStreams(
        value: Any?,
        categories: Map<String, String>,
        credentials: XtreamCredentials,
        base: HttpUrl,
    ): List<Channel> {
        val array = asArray(value) ?: return emptyList()
        val channels = ArrayList<Channel>()
        val seen = HashSet<String>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val streamId = text(item, "stream_id") ?: continue
            if (!seen.add(streamId)) continue

            val categoryId = text(item, "category_id").orEmpty()
            val directSource = httpUrl(item, "direct_source")
            channels += Channel(
                id = stableId(
                    prefix = "ch",
                    key = "${sourceKey(base)}|${credentials.username}|live|$streamId",
                ),
                name = text(item, "name") ?: "Canal $streamId",
                streamUrl = directSource
                    ?: streamUrl(base, "live", credentials, streamId, "m3u8"),
                group = categories[categoryId] ?: "Sin categoría",
                logoUrl = text(item, "stream_icon"),
                tvgId = text(item, "epg_channel_id"),
                number = intOrNull(item, "num"),
            )
        }
        return channels
    }

    fun parseMovies(
        value: Any?,
        categories: Map<String, String>,
        credentials: XtreamCredentials,
        base: HttpUrl,
    ): List<VodItem> {
        val array = asArray(value) ?: return emptyList()
        val movies = ArrayList<VodItem>()
        val seen = HashSet<String>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val streamId = text(item, "stream_id") ?: text(item, "id") ?: continue
            if (!seen.add(streamId)) continue

            val categoryId = text(item, "category_id").orEmpty()
            val extension = text(item, "container_extension")
                ?.takeIf { extensionRegex.matches(it) }
                ?: "mp4"
            val directSource = httpUrl(item, "direct_source")
            movies += VodItem(
                id = stableId(
                    prefix = "mv",
                    key = "${sourceKey(base)}|${credentials.username}|movie|$streamId",
                ),
                sourceId = streamId,
                name = text(item, "name") ?: "Película $streamId",
                streamUrl = directSource
                    ?: streamUrl(base, "movie", credentials, streamId, extension),
                group = categories[categoryId] ?: "Sin categoría",
                logoUrl = text(item, "stream_icon"),
                rating = text(item, "rating"),
                year = text(item, "year"),
                plot = text(item, "plot"),
                containerExtension = extension,
            )
        }
        return movies
    }

    fun parseSeries(
        value: Any?,
        categories: Map<String, String>,
        credentials: XtreamCredentials,
        base: HttpUrl,
    ): List<SeriesItem> {
        val array = asArray(value) ?: return emptyList()
        val series = ArrayList<SeriesItem>()
        val seen = HashSet<String>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val seriesId = text(item, "series_id") ?: text(item, "id") ?: continue
            if (!seen.add(seriesId)) continue

            val categoryId = text(item, "category_id").orEmpty()
            series += SeriesItem(
                id = stableId(
                    prefix = "sr",
                    key = "${sourceKey(base)}|${credentials.username}|series|$seriesId",
                ),
                sourceId = seriesId,
                name = text(item, "name") ?: "Serie $seriesId",
                group = categories[categoryId] ?: "Sin categoría",
                coverUrl = text(item, "cover") ?: text(item, "cover_big"),
                rating = text(item, "rating"),
                year = text(item, "year"),
                plot = text(item, "plot"),
            )
        }
        return series
    }

    fun parseVodInfo(root: JSONObject): VodDetail {
        val info = root.optJSONObject("info") ?: return VodDetail()
        fun first(vararg keys: String): String? = keys.firstNotNullOfOrNull { text(info, it) }
        return VodDetail(
            plot = first("plot", "description"),
            genre = first("genre"),
            cast = first("cast", "actors"),
            director = first("director"),
            duration = first("duration"),
            rating = first("rating", "rating_5based")?.takeIf { it != "0" },
            releaseDate = first("releasedate", "release_date", "releaseDate"),
        )
    }

    fun parseSeriesInfo(
        root: JSONObject,
        credentials: XtreamCredentials,
        base: HttpUrl,
    ): SeriesDetail {
        val plot = root.optJSONObject("info")?.let { text(it, "plot") }
        val episodes = ArrayList<SeriesEpisode>()

        fun addEpisode(item: JSONObject, fallbackSeason: Int) {
            val id = text(item, "id") ?: return
            val extension = text(item, "container_extension")
                ?.takeIf { extensionRegex.matches(it) }
                ?: "mp4"
            val season = intOrNull(item, "season") ?: fallbackSeason
            val number = intOrNull(item, "episode_num") ?: (episodes.size + 1)
            val info = item.optJSONObject("info")
            episodes += SeriesEpisode(
                id = id,
                season = season,
                number = number,
                title = text(item, "title") ?: "Episodio $number",
                streamUrl = streamUrl(base, "series", credentials, id, extension),
                plot = info?.let { text(it, "plot") },
                duration = info?.let { text(it, "duration") },
            )
        }

        fun addSeason(value: Any?, season: Int) {
            val array = value as? JSONArray ?: return
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { addEpisode(it, season) }
            }
        }

        when (val raw = root.opt("episodes")) {
            is JSONObject -> raw.keys().forEach { key ->
                addSeason(raw.opt(key), key.toIntOrNull() ?: 1)
            }
            is JSONArray -> for (index in 0 until raw.length()) {
                val entry = raw.opt(index)
                if (entry is JSONArray) {
                    addSeason(entry, index + 1)
                } else if (entry is JSONObject) {
                    addEpisode(entry, 1)
                }
            }
        }
        val seen = HashSet<String>()
        return SeriesDetail(
            plot = plot,
            episodes = episodes
                .filter { seen.add(it.id) }
                .sortedWith(compareBy({ it.season }, { it.number })),
        )
    }

    private fun accountFrom(user: JSONObject): XtreamAccount {
        val auth = when (val raw = user.opt("auth")) {
            is Boolean -> if (raw) 1 else 0
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull() ?: 1
            null -> 1
            else -> 1
        }
        if (auth == 0) {
            throw XtreamException("Usuario o contraseña incorrectos.")
        }

        val status = text(user, "status")
        if (status != null && status.lowercase() in inactiveStatuses) {
            throw XtreamException("La cuenta Xtream no está activa (estado: $status).")
        }
        return XtreamAccount(
            status = status,
            isTrial = status.equals("Trial", ignoreCase = true),
            maxConnections = intOrNull(user, "max_connections")?.takeIf { it > 0 },
        )
    }

    private fun asArray(value: Any?): JSONArray? = when (value) {
        is JSONArray -> value
        is JSONObject -> value.optJSONArray("data")
            ?: value.optJSONArray("categories")
            ?: value.optJSONArray("streams")
            ?: value.optJSONArray("series")
        else -> null
    }

    private fun text(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = when (val raw = obj.opt(key)) {
            is String -> raw
            is Number -> raw.toString()
            is Boolean -> raw.toString()
            null -> null
            else -> null
        }?.trim()
        return value?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun intOrNull(obj: JSONObject, key: String): Int? = when (val raw = obj.opt(key)) {
        is Number -> raw.toInt()
        is String -> raw.trim().toIntOrNull()
        null -> null
        else -> null
    }

    private fun httpUrl(obj: JSONObject, key: String): String? = text(obj, key)
        ?.takeIf {
            it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("https://", ignoreCase = true)
        }

    private fun sourceKey(base: HttpUrl): String =
        if (base.encodedPath == "/") {
            "${base.host}:${base.port}"
        } else {
            "${base.host}:${base.port}${base.encodedPath}"
        }

    private fun stableId(prefix: String, key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
        return buildString(prefix.length + 25) {
            append(prefix)
            append('-')
            digest.take(12).forEach { byte -> append("%02x".format(byte.toInt() and 0xFF)) }
        }
    }
}
