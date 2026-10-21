package io.github.gonzsanz.cobalttv.data.catalog

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.github.gonzsanz.cobalttv.data.xtream.XtreamCatalog
import io.github.gonzsanz.cobalttv.data.xtream.XtreamCatalogCache
import io.github.gonzsanz.cobalttv.data.xtream.XtreamClient
import io.github.gonzsanz.cobalttv.data.xtream.XtreamCredentialStore
import io.github.gonzsanz.cobalttv.data.xtream.XtreamCredentials
import io.github.gonzsanz.cobalttv.data.xtream.XtreamException
import io.github.gonzsanz.cobalttv.data.xtream.XtreamJson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Single source of truth for the imported channel list.
 *
 * Only one playlist is active at a time for now. The raw M3U is cached in the
 * app's private files directory so Live TV keeps working after a restart and
 * can be restored without hitting the network. URLs and file contents are
 * never logged (they can contain private tokens).
 */
class CatalogRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val xtreamClient = XtreamClient()
    private val xtreamCredentials = XtreamCredentialStore(appContext)

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        scope.launch { restore() }
    }

    fun importFromUrl(rawUrl: String, displayName: String? = null) {
        if (_state.value.isLoading) return
        _state.update {
            it.copy(isLoading = true, importError = null, importErrorTarget = null)
        }
        scope.launch {
            try {
                val url = normalizeUrl(rawUrl)
                val text = decode(download(url))
                commitImport(
                    kind = SourceKind.Url,
                    requestedName = displayName,
                    location = url,
                    text = text,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failImport(error, SourceKind.Url)
            }
        }
    }

    fun importFromUri(uri: Uri) {
        if (_state.value.isLoading) return
        _state.update {
            it.copy(isLoading = true, importError = null, importErrorTarget = null)
        }
        scope.launch {
            try {
                val displayName = queryDisplayName(uri)
                val bytes = appContext.contentResolver.openInputStream(uri)
                    ?.use { it.readLimited(MAX_BYTES) }
                    ?: throw CatalogException("No se pudo abrir el archivo seleccionado.")
                val text = decode(bytes)
                val name = displayName
                    ?.substringBeforeLast('.')
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: "Lista local"
                commitImport(
                    kind = SourceKind.File,
                    requestedName = name,
                    location = displayName ?: name,
                    text = text,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failImport(error, SourceKind.File)
            }
        }
    }

    fun importXtream(rawServer: String, rawUsername: String, rawPassword: String) {
        if (_state.value.isLoading) return
        val server = rawServer.trim()
        val username = rawUsername.trim()
        val password = rawPassword
        val validation = when {
            server.isEmpty() -> "Escribe la URL del servidor Xtream."
            username.isEmpty() -> "Escribe el usuario de Xtream Codes."
            password.isBlank() -> "Escribe la contraseña de Xtream Codes."
            else -> null
        }
        if (validation != null) {
            failImport(XtreamException(validation), SourceKind.Xtream)
            return
        }

        _state.update {
            it.copy(isLoading = true, importError = null, importErrorTarget = null)
        }
        scope.launch {
            try {
                val credentials = XtreamCredentials(
                    server = XtreamJson.normalizeServer(server).toString(),
                    username = username,
                    password = password,
                )
                val catalog = xtreamClient.load(credentials)
                commitXtreamImport(credentials, catalog)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failImport(error, SourceKind.Xtream)
            }
        }
    }

    fun refresh() {
        val source = _state.value.source ?: return
        when (source.kind) {
            SourceKind.Url -> importFromUrl(source.location, source.name)
            SourceKind.Xtream -> {
                val stored = xtreamCredentials.load()
                if (stored == null) {
                    failImport(
                        XtreamException(
                            "No se pudieron recuperar las credenciales de Xtream. " +
                                "Vuelve a importar la cuenta.",
                        ),
                        SourceKind.Xtream,
                    )
                    return
                }
                importXtream(source.location, stored.username, stored.password)
            }

            SourceKind.File -> Unit
        }
    }

    fun removeSource() {
        if (_state.value.isLoading) return
        scope.launch {
            clearStoredSource()
            _state.value = CatalogState(isLoading = false)
        }
    }

    suspend fun loadVodDetail(movie: VodItem): VodDetail {
        val source = _state.value.source
        val stored = xtreamCredentials.load()
        if (source?.kind != SourceKind.Xtream || stored == null) return VodDetail()
        val credentials = XtreamCredentials(source.location, stored.username, stored.password)
        return withContext(Dispatchers.IO) {
            xtreamClient.loadVodDetail(credentials, movie.sourceId)
        }
    }

    /** Seasons and episodes come from the provider on demand, never from the cache. */
    suspend fun loadSeriesDetail(series: SeriesItem): SeriesDetail {
        val source = _state.value.source
        val stored = xtreamCredentials.load()
        if (source?.kind != SourceKind.Xtream || stored == null) {
            throw XtreamException("Esta fuente no ofrece temporadas ni episodios.")
        }
        val credentials = XtreamCredentials(source.location, stored.username, stored.password)
        return withContext(Dispatchers.IO) {
            xtreamClient.loadSeriesDetail(credentials, series.sourceId)
        }
    }

    fun clearImportError() {
        if (_state.value.importError != null) {
            _state.update { it.copy(importError = null, importErrorTarget = null) }
        }
    }

    private suspend fun restore() {
        val kind = prefs.getString(KEY_KIND, null)
            ?.let { stored -> SourceKind.entries.firstOrNull { it.name == stored } }
        val location = prefs.getString(KEY_LOCATION, null)
        if (kind == null || location.isNullOrBlank()) {
            _state.value = CatalogState(isLoading = false)
            return
        }

        try {
            when (kind) {
                SourceKind.Url, SourceKind.File -> restorePlaylist(kind, location)
                SourceKind.Xtream -> restoreXtream(location)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Never log URLs or credentials: they can include private tokens.
            Log.w(TAG, "No se pudo restaurar la fuente (${error.javaClass.simpleName})")
            clearStoredSource()
            _state.value = CatalogState(
                isLoading = false,
                importError = "No se pudo recuperar la fuente guardada. Vuelve a importarla.",
            )
        }
    }

    private fun restorePlaylist(kind: SourceKind, location: String) {
        val text = readCache(CACHE_NAME)
            ?: throw CatalogException("No se encontró la lista guardada.")
        val playlist = M3uParser.parsePlaylist(text)
        val channels = playlist.channels
        if (channels.isEmpty()) throw CatalogException("La lista guardada está vacía.")
        val name = prefs.getString(KEY_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: defaultSourceName(kind, location)
        _state.value = CatalogState(
            source = PlaylistSource(
                kind = kind,
                name = name,
                location = location,
                channelCount = channels.size,
                importedAt = prefs.getLong(KEY_IMPORTED_AT, 0L),
                epgUrl = playlist.epgUrl,
            ),
            channels = channels,
            isLoading = false,
        )
    }

    private fun restoreXtream(location: String) {
        val stored = xtreamCredentials.load()
            ?: throw CatalogException("No se pudieron recuperar las credenciales de Xtream.")
        val text = readCache(XTREAM_CACHE_NAME)
            ?: throw CatalogException("No se encontró el catálogo Xtream guardado.")
        val catalog = XtreamCatalogCache.decode(text)
        if (catalog.channels.isEmpty()) {
            throw CatalogException("La cuenta Xtream guardada no tiene canales en directo.")
        }
        val name = prefs.getString(KEY_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: defaultSourceName(SourceKind.Xtream, location)
        val credentials = XtreamCredentials(location, stored.username, stored.password)
        _state.value = CatalogState(
            source = PlaylistSource(
                kind = SourceKind.Xtream,
                name = name,
                location = location,
                channelCount = catalog.channels.size,
                importedAt = prefs.getLong(KEY_IMPORTED_AT, 0L),
                epgUrl = xtreamEpgUrl(credentials),
                movieCount = catalog.movies.size,
                seriesCount = catalog.series.size,
            ),
            channels = catalog.channels,
            movies = catalog.movies,
            series = catalog.series,
            isLoading = false,
        )
    }

    private fun commitImport(
        kind: SourceKind,
        requestedName: String?,
        location: String,
        text: String,
    ) {
        val playlist = M3uParser.parsePlaylist(text)
        val channels = playlist.channels
        if (channels.isEmpty()) {
            throw CatalogException("La lista no contiene canales compatibles.")
        }
        writeCache(CACHE_NAME, text)

        val name = requestedName?.trim()?.takeIf { it.isNotEmpty() }
            ?: defaultSourceName(kind, location)
        val importedAt = System.currentTimeMillis()
        val saved = prefs.edit()
            .putString(KEY_KIND, kind.name)
            .putString(KEY_NAME, name)
            .putString(KEY_LOCATION, location)
            .putLong(KEY_IMPORTED_AT, importedAt)
            .commit()
        if (!saved) {
            throw CatalogException("No se pudo guardar la fuente en el dispositivo.")
        }

        // An M3U import replaces the active source: do not keep old Xtream
        // credentials or its catalog on disk.
        runCatching { File(appContext.filesDir, XTREAM_CACHE_NAME).delete() }
        xtreamCredentials.clear()

        _state.value = CatalogState(
            source = PlaylistSource(
                kind = kind,
                name = name,
                location = location,
                channelCount = channels.size,
                importedAt = importedAt,
                epgUrl = playlist.epgUrl,
            ),
            channels = channels,
            isLoading = false,
        )
    }

    private fun commitXtreamImport(
        credentials: XtreamCredentials,
        catalog: XtreamCatalog,
    ) {
        if (catalog.channels.isEmpty()) {
            throw XtreamException("La cuenta no tiene canales en directo.")
        }

        val encoded = XtreamCatalogCache.encode(catalog)
        xtreamCredentials.save(credentials.username, credentials.password)
        writeCache(XTREAM_CACHE_NAME, encoded)

        val name = defaultSourceName(SourceKind.Xtream, credentials.server)
        val importedAt = System.currentTimeMillis()
        val saved = prefs.edit()
            .putString(KEY_KIND, SourceKind.Xtream.name)
            .putString(KEY_NAME, name)
            .putString(KEY_LOCATION, credentials.server)
            .putLong(KEY_IMPORTED_AT, importedAt)
            .commit()
        if (!saved) {
            throw CatalogException("No se pudo guardar la cuenta Xtream en el dispositivo.")
        }

        runCatching { File(appContext.filesDir, CACHE_NAME).delete() }

        _state.value = CatalogState(
            source = PlaylistSource(
                kind = SourceKind.Xtream,
                name = name,
                location = credentials.server,
                channelCount = catalog.channels.size,
                importedAt = importedAt,
                epgUrl = xtreamEpgUrl(credentials),
                movieCount = catalog.movies.size,
                seriesCount = catalog.series.size,
            ),
            channels = catalog.channels,
            movies = catalog.movies,
            series = catalog.series,
            isLoading = false,
        )
    }

    private fun failImport(error: Exception, target: SourceKind? = null) {
        // Never log URLs, file contents or credentials.
        Log.w(TAG, "Importación fallida (${error.javaClass.simpleName})")
        _state.update {
            it.copy(
                isLoading = false,
                importError = friendlyMessage(error, target),
                importErrorTarget = target,
            )
        }
    }

    private fun download(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CobaltTV/0.5")
            .header(
                "Accept",
                "audio/x-mpegurl, application/vnd.apple.mpegurl, " +
                    "application/x-mpegURL, text/plain, */*",
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    401, 403 -> "El servidor rechazó la descarga (HTTP ${response.code}). " +
                        "Comprueba que el enlace sea accesible."
                    404 -> "La lista no existe en esa URL (HTTP 404)."
                    in 500..599 -> "El servidor de la lista está fallando (HTTP ${response.code})."
                    else -> "El servidor respondió HTTP ${response.code}."
                }
                throw CatalogException(message)
            }
            val body = response.body
            if (body.contentLength() > MAX_BYTES) {
                throw CatalogException("La lista supera el límite de 16 MB.")
            }
            return body.byteStream().use { it.readLimited(MAX_BYTES) }
        }
    }

    private fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) throw CatalogException("Escribe la URL de la lista M3U.")
        val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return candidate.toHttpUrlOrNull()?.toString()
            ?: throw CatalogException(
                "La URL no es válida. Debe empezar por http:// o https://.",
            )
    }

    private fun decode(bytes: ByteArray): String {
        val utf8 = String(bytes, Charsets.UTF_8)
        // Many providers still publish Latin-1 lists. Invalid UTF-8 shows up as
        // replacement characters, so fall back only in that case.
        return if (utf8.contains('\uFFFD')) {
            String(bytes, Charsets.ISO_8859_1)
        } else {
            utf8
        }
    }

    private fun readCache(fileName: String): String? {
        val file = File(appContext.filesDir, fileName)
        if (!file.isFile) return null
        return file.readText(Charsets.UTF_8)
    }

    private fun writeCache(fileName: String, text: String) {
        val target = File(appContext.filesDir, fileName)
        val temp = File(appContext.filesDir, "$fileName.tmp")
        temp.writeText(text, Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) {
                temp.delete()
                throw CatalogException("No se pudo guardar la fuente en el dispositivo.")
            }
        }
    }

    private fun clearStoredSource() {
        runCatching { File(appContext.filesDir, CACHE_NAME).delete() }
        runCatching { File(appContext.filesDir, XTREAM_CACHE_NAME).delete() }
        xtreamCredentials.clear()
        prefs.edit().clear().commit()
    }

    private fun defaultSourceName(kind: SourceKind, location: String): String = when (kind) {
        SourceKind.Url -> location.toHttpUrlOrNull()?.host
            ?.takeIf { it.isNotBlank() }
            ?: "Lista M3U"

        SourceKind.File -> location.substringBeforeLast('.').trim()
            .ifEmpty { "Lista local" }

        SourceKind.Xtream -> location.toHttpUrlOrNull()?.host
            ?.takeIf { it.isNotBlank() }
            ?: "Xtream Codes"
    }

    private fun xtreamEpgUrl(credentials: XtreamCredentials): String? =
        credentials.server.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegment("xmltv.php")
            ?.addQueryParameter("username", credentials.username)
            ?.addQueryParameter("password", credentials.password)
            ?.build()
            ?.toString()

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > limit) {
                throw CatalogException(
                    "La lista supera el límite de ${limit / (1024 * 1024)} MB.",
                )
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun friendlyMessage(error: Throwable, target: SourceKind?): String = when (error) {
        is XtreamException -> error.message ?: "No se pudo importar la cuenta Xtream."
        is CatalogException -> error.message ?: "No se pudo importar la lista."
        is UnknownHostException -> if (target == SourceKind.Xtream) {
            "No se pudo encontrar el servidor Xtream. Revisa la URL y la conexión."
        } else {
            "No se pudo encontrar el servidor. Revisa la URL y la conexión a internet."
        }

        is SocketTimeoutException ->
            "El servidor tardó demasiado en responder. Inténtalo de nuevo."
        is SSLException -> "La conexión segura con el servidor falló."
        is IOException -> if (target == SourceKind.Xtream) {
            "No se pudo conectar con el servidor Xtream. Revisa la conexión."
        } else {
            "No se pudo descargar la lista. Revisa la conexión e inténtalo de nuevo."
        }

        else -> if (target == SourceKind.Xtream) {
            "No se pudo importar la cuenta Xtream (${error.javaClass.simpleName})."
        } else {
            "No se pudo importar la lista (${error.javaClass.simpleName})."
        }
    }

    private companion object {
        const val TAG = "CobaltCatalog"
        const val PREFS = "cobalt_catalog"
        const val CACHE_NAME = "playlist_cache.m3u"
        const val XTREAM_CACHE_NAME = "xtream_cache.json"
        const val KEY_KIND = "source_kind"
        const val KEY_NAME = "source_name"
        const val KEY_LOCATION = "source_location"
        const val KEY_IMPORTED_AT = "source_imported_at"
        const val MAX_BYTES = 16 * 1024 * 1024
    }
}
