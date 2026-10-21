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
import kotlinx.coroutines.launch
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

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        scope.launch { restore() }
    }

    fun importFromUrl(rawUrl: String, displayName: String? = null) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, importError = null) }
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
                failImport(error)
            }
        }
    }

    fun importFromUri(uri: Uri) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, importError = null) }
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
                failImport(error)
            }
        }
    }

    fun refresh() {
        val source = _state.value.source ?: return
        if (source.kind != SourceKind.Url) return
        importFromUrl(source.location, source.name)
    }

    fun removeSource() {
        if (_state.value.isLoading) return
        scope.launch {
            clearStoredSource()
            _state.value = CatalogState(isLoading = false)
        }
    }

    fun clearImportError() {
        if (_state.value.importError != null) {
            _state.update { it.copy(importError = null) }
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
            val text = readCache() ?: throw CatalogException("No se encontró la lista guardada.")
            val channels = M3uParser.parse(text)
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
                ),
                channels = channels,
                isLoading = false,
            )
        } catch (error: Exception) {
            Log.w(TAG, "No se pudo restaurar la lista (${error.javaClass.simpleName})")
            clearStoredSource()
            _state.value = CatalogState(
                isLoading = false,
                importError = "No se pudo recuperar la lista guardada. Vuelve a importarla.",
            )
        }
    }

    private fun commitImport(
        kind: SourceKind,
        requestedName: String?,
        location: String,
        text: String,
    ) {
        val channels = M3uParser.parse(text)
        if (channels.isEmpty()) {
            throw CatalogException("La lista no contiene canales compatibles.")
        }
        writeCache(text)

        val name = requestedName?.trim()?.takeIf { it.isNotEmpty() }
            ?: defaultSourceName(kind, location)
        val importedAt = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_KIND, kind.name)
            .putString(KEY_NAME, name)
            .putString(KEY_LOCATION, location)
            .putLong(KEY_IMPORTED_AT, importedAt)
            .commit()

        _state.value = CatalogState(
            source = PlaylistSource(
                kind = kind,
                name = name,
                location = location,
                channelCount = channels.size,
                importedAt = importedAt,
            ),
            channels = channels,
            isLoading = false,
        )
    }

    private fun failImport(error: Exception) {
        // Never log URLs or file contents: they can include credentials.
        Log.w(TAG, "Importación fallida (${error.javaClass.simpleName})")
        _state.update {
            it.copy(isLoading = false, importError = friendlyMessage(error))
        }
    }

    private fun download(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CobaltTV/0.3")
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

    private fun readCache(): String? {
        val file = File(appContext.filesDir, CACHE_NAME)
        if (!file.isFile) return null
        return file.readText(Charsets.UTF_8)
    }

    private fun writeCache(text: String) {
        val target = File(appContext.filesDir, CACHE_NAME)
        val temp = File(appContext.filesDir, "$CACHE_NAME.tmp")
        temp.writeText(text, Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) {
                temp.delete()
                throw CatalogException("No se pudo guardar la lista en el dispositivo.")
            }
        }
    }

    private fun clearStoredSource() {
        runCatching { File(appContext.filesDir, CACHE_NAME).delete() }
        prefs.edit().clear().commit()
    }

    private fun defaultSourceName(kind: SourceKind, location: String): String = when (kind) {
        SourceKind.Url -> location.toHttpUrlOrNull()?.host
            ?.takeIf { it.isNotBlank() }
            ?: "Lista M3U"

        SourceKind.File -> location.substringBeforeLast('.').trim()
            .ifEmpty { "Lista local" }
    }

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

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is CatalogException -> error.message ?: "No se pudo importar la lista."
        is UnknownHostException ->
            "No se pudo encontrar el servidor. Revisa la URL y la conexión a internet."
        is SocketTimeoutException ->
            "El servidor tardó demasiado en responder. Inténtalo de nuevo."
        is SSLException -> "La conexión segura con el servidor falló."
        is IOException ->
            "No se pudo descargar la lista. Revisa la conexión e inténtalo de nuevo."
        else -> "No se pudo importar la lista (${error.javaClass.simpleName})."
    }

    private companion object {
        const val TAG = "CobaltCatalog"
        const val PREFS = "cobalt_catalog"
        const val CACHE_NAME = "playlist_cache.m3u"
        const val KEY_KIND = "source_kind"
        const val KEY_NAME = "source_name"
        const val KEY_LOCATION = "source_location"
        const val KEY_IMPORTED_AT = "source_imported_at"
        const val MAX_BYTES = 16 * 1024 * 1024
    }
}
