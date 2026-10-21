package io.github.gonzsanz.cobalttv.data.xtream

import io.github.gonzsanz.cobalttv.data.catalog.Channel
import io.github.gonzsanz.cobalttv.data.catalog.SeriesItem
import io.github.gonzsanz.cobalttv.data.catalog.VodItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Disk representation of the normalized Xtream catalog. It does not store a
 * separate credential record: playback URLs already contain the account path,
 * and the file lives in the app's private directory.
 */
object XtreamCatalogCache {

    private const val VERSION = 1

    fun encode(catalog: XtreamCatalog): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put(
            "channels",
            JSONArray().apply {
                catalog.channels.forEach { put(channelToJson(it)) }
            },
        )
        root.put(
            "movies",
            JSONArray().apply {
                catalog.movies.forEach { put(movieToJson(it)) }
            },
        )
        root.put(
            "series",
            JSONArray().apply {
                catalog.series.forEach { put(seriesToJson(it)) }
            },
        )
        return root.toString()
    }

    fun decode(text: String): XtreamCatalog {
        val root = try {
            JSONObject(text)
        } catch (error: JSONException) {
            throw XtreamException("La caché Xtream guardada no es válida.")
        }
        if (root.optInt("version", 0) != VERSION) {
            throw XtreamException("La caché Xtream guardada no es compatible.")
        }

        val channels = root.optJSONArray("channels").mapObjects(::channelFromJson)
        val movies = root.optJSONArray("movies").mapObjects(::movieFromJson)
        val series = root.optJSONArray("series").mapObjects(::seriesFromJson)
        if (channels.isEmpty() && movies.isEmpty() && series.isEmpty()) {
            throw XtreamException("La caché Xtream guardada está vacía.")
        }
        return XtreamCatalog(
            channels = channels,
            movies = movies,
            series = series,
        )
    }

    private fun channelToJson(channel: Channel): JSONObject = JSONObject().apply {
        put("id", channel.id)
        put("name", channel.name)
        put("streamUrl", channel.streamUrl)
        put("group", channel.group)
        putNullable("logoUrl", channel.logoUrl)
        putNullable("tvgId", channel.tvgId)
        putNullable("number", channel.number)
    }

    private fun channelFromJson(json: JSONObject): Channel? {
        val id = json.optStringOrNull("id") ?: return null
        val name = json.optStringOrNull("name") ?: return null
        val streamUrl = json.optStringOrNull("streamUrl") ?: return null
        val group = json.optStringOrNull("group") ?: return null
        return Channel(
            id = id,
            name = name,
            streamUrl = streamUrl,
            group = group,
            logoUrl = json.optStringOrNull("logoUrl"),
            tvgId = json.optStringOrNull("tvgId"),
            number = if (json.has("number") && !json.isNull("number")) {
                json.optInt("number")
            } else {
                null
            },
        )
    }

    private fun movieToJson(movie: VodItem): JSONObject = JSONObject().apply {
        put("id", movie.id)
        put("sourceId", movie.sourceId)
        put("name", movie.name)
        put("streamUrl", movie.streamUrl)
        put("group", movie.group)
        putNullable("logoUrl", movie.logoUrl)
        putNullable("rating", movie.rating)
        putNullable("year", movie.year)
        putNullable("plot", movie.plot)
        putNullable("containerExtension", movie.containerExtension)
    }

    private fun movieFromJson(json: JSONObject): VodItem? {
        val id = json.optStringOrNull("id") ?: return null
        val sourceId = json.optStringOrNull("sourceId") ?: return null
        val name = json.optStringOrNull("name") ?: return null
        val streamUrl = json.optStringOrNull("streamUrl") ?: return null
        val group = json.optStringOrNull("group") ?: return null
        return VodItem(
            id = id,
            sourceId = sourceId,
            name = name,
            streamUrl = streamUrl,
            group = group,
            logoUrl = json.optStringOrNull("logoUrl"),
            rating = json.optStringOrNull("rating"),
            year = json.optStringOrNull("year"),
            plot = json.optStringOrNull("plot"),
            containerExtension = json.optStringOrNull("containerExtension"),
        )
    }

    private fun seriesToJson(series: SeriesItem): JSONObject = JSONObject().apply {
        put("id", series.id)
        put("sourceId", series.sourceId)
        put("name", series.name)
        put("group", series.group)
        putNullable("coverUrl", series.coverUrl)
        putNullable("rating", series.rating)
        putNullable("year", series.year)
        putNullable("plot", series.plot)
    }

    private fun seriesFromJson(json: JSONObject): SeriesItem? {
        val id = json.optStringOrNull("id") ?: return null
        val sourceId = json.optStringOrNull("sourceId") ?: return null
        val name = json.optStringOrNull("name") ?: return null
        val group = json.optStringOrNull("group") ?: return null
        return SeriesItem(
            id = id,
            sourceId = sourceId,
            name = name,
            group = group,
            coverUrl = json.optStringOrNull("coverUrl"),
            rating = json.optStringOrNull("rating"),
            year = json.optStringOrNull("year"),
            plot = json.optStringOrNull("plot"),
        )
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() }
    }

    private inline fun <T> JSONArray?.mapObjects(
        transform: (JSONObject) -> T?,
    ): List<T> {
        if (this == null) return emptyList()
        val result = ArrayList<T>()
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            transform(item)?.let(result::add)
        }
        return result
    }
}
