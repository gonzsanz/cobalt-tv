package io.github.gonzsanz.cobalttv.data.xtream

import io.github.gonzsanz.cobalttv.data.catalog.SeriesDetail
import io.github.gonzsanz.cobalttv.data.catalog.VodDetail
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Small Xtream Codes API client.
 *
 * It logs nothing: the request URL contains the username and password, and the
 * response may repeat them. All failures are translated to [XtreamException]
 * with a message that is safe to show in the UI.
 */
class XtreamClient(
    private val client: OkHttpClient = defaultClient(),
) {

    fun load(credentials: XtreamCredentials): XtreamCatalog {
        val base = XtreamJson.normalizeServer(credentials.server)
        val normalized = credentials.copy(server = base.toString())

        val login = fetchObject(XtreamJson.apiUrl(base, normalized, action = null))
        val account = XtreamJson.parseLogin(login)

        val liveCategories = try {
            XtreamJson.parseCategories(
                fetchValue(XtreamJson.apiUrl(base, normalized, "get_live_categories")),
            )
        } catch (error: XtreamException) {
            // Categories are optional: keep the live channels without groups.
            emptyMap()
        }
        val live = XtreamJson.parseLiveStreams(
            value = fetchValue(XtreamJson.apiUrl(base, normalized, "get_live_streams")),
            categories = liveCategories,
            credentials = normalized,
            base = base,
        )

        // VOD and series are optional sections. Some Xtream servers do not
        // implement them or return a partial response; that must not block
        // live TV. Phase 6 will consume these normalized lists.
        val movies = try {
            val movieCategories = XtreamJson.parseCategories(
                fetchValue(XtreamJson.apiUrl(base, normalized, "get_vod_categories")),
            )
            XtreamJson.parseMovies(
                value = fetchValue(XtreamJson.apiUrl(base, normalized, "get_vod_streams")),
                categories = movieCategories,
                credentials = normalized,
                base = base,
            )
        } catch (error: XtreamException) {
            emptyList()
        }

        val series = try {
            val seriesCategories = XtreamJson.parseCategories(
                fetchValue(XtreamJson.apiUrl(base, normalized, "get_series_categories")),
            )
            XtreamJson.parseSeries(
                value = fetchValue(XtreamJson.apiUrl(base, normalized, "get_series")),
                categories = seriesCategories,
                credentials = normalized,
                base = base,
            )
        } catch (error: XtreamException) {
            emptyList()
        }

        return XtreamCatalog(
            channels = live,
            movies = movies,
            series = series,
            account = account,
        )
    }

    fun loadVodDetail(credentials: XtreamCredentials, vodId: String): VodDetail {
        val base = XtreamJson.normalizeServer(credentials.server)
        val normalized = credentials.copy(server = base.toString())
        val url = XtreamJson.apiUrl(base, normalized, "get_vod_info")
            .newBuilder()
            .addQueryParameter("vod_id", vodId)
            .build()
        return XtreamJson.parseVodInfo(fetchObject(url))
    }

    fun loadSeriesDetail(credentials: XtreamCredentials, seriesId: String): SeriesDetail {
        val base = XtreamJson.normalizeServer(credentials.server)
        val normalized = credentials.copy(server = base.toString())
        val url = XtreamJson.apiUrl(base, normalized, "get_series_info")
            .newBuilder()
            .addQueryParameter("series_id", seriesId)
            .build()
        return XtreamJson.parseSeriesInfo(fetchObject(url), normalized, base)
    }

    private fun fetchObject(url: HttpUrl): JSONObject =
        fetchValue(url) as? JSONObject
            ?: throw XtreamException("El servidor no devolvió una respuesta Xtream válida.")

    private fun fetchValue(url: HttpUrl): Any {
        val text = requestText(url)
        val value = try {
            JSONTokener(text).nextValue()
        } catch (error: JSONException) {
            throw XtreamException("El servidor no devolvió una respuesta Xtream válida.")
        }
        if (value is JSONObject) {
            XtreamJson.validateAccount(value)
        }
        if (value !is JSONObject && value !is JSONArray) {
            throw XtreamException("El servidor no devolvió una respuesta Xtream válida.")
        }
        return value
    }

    private fun requestText(url: HttpUrl): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CobaltTV/0.5")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw httpError(response.code)
                }
                val body = response.body
                if (body.contentLength() > MAX_BYTES) {
                    throw XtreamException("La respuesta Xtream supera el límite de 32 MB.")
                }
                val bytes = body.byteStream().use { it.readLimited(MAX_BYTES) }
                if (bytes.isEmpty()) {
                    throw XtreamException("El servidor Xtream devolvió una respuesta vacía.")
                }
                return String(bytes, Charsets.UTF_8)
            }
        } catch (error: XtreamException) {
            throw error
        } catch (error: UnknownHostException) {
            throw XtreamException(
                "No se pudo encontrar el servidor Xtream. Revisa la URL y la conexión.",
            )
        } catch (error: SocketTimeoutException) {
            throw XtreamException(
                "El servidor Xtream tardó demasiado en responder. Inténtalo de nuevo.",
            )
        } catch (error: SSLException) {
            throw XtreamException("La conexión segura con el servidor Xtream falló.")
        } catch (error: IOException) {
            throw XtreamException(
                "No se pudo conectar con el servidor Xtream. Revisa la conexión.",
            )
        }
    }

    private fun httpError(code: Int): XtreamException = when (code) {
        401, 403 -> XtreamException(
            "El servidor rechazó las credenciales (HTTP $code).",
        )
        404 -> XtreamException(
            "No se encontró la API de Xtream en ese servidor (HTTP 404).",
        )
        in 500..599 -> XtreamException("El servidor Xtream está fallando (HTTP $code).")
        else -> XtreamException("El servidor Xtream respondió HTTP $code.")
    }

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > limit) {
                throw XtreamException("La respuesta Xtream supera el límite de 32 MB.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val MAX_BYTES = 32 * 1024 * 1024

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
