package io.github.gonzsanz.cobalttv.data.epg

import android.util.Log
import io.github.gonzsanz.cobalttv.data.catalog.CatalogRepository
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Loads the XMLTV guide declared by the active playlist.
 *
 * The full XMLTV file is never cached or logged. Only the current and upcoming
 * programmes for channels in the active list are kept, in memory. The guide is
 * fetched again when the playlist is replaced or updated.
 */
class EpgRepository(
    private val catalog: CatalogRepository,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(EpgState())
    val state: StateFlow<EpgState> = _state.asStateFlow()

    init {
        scope.launch {
            catalog.state
                .map { catalogState ->
                    EpgRequest(
                        epgUrl = catalogState.source?.epgUrl,
                        channelIds = catalogState.channels.mapNotNull { it.tvgId }.toSet(),
                        refreshKey = catalogState.source?.importedAt ?: 0L,
                    )
                }
                .distinctUntilChanged()
                .collectLatest { request -> load(request) }
        }
    }

    private suspend fun load(request: EpgRequest) {
        val url = request.epgUrl?.toHttpUrlOrNull()?.toString()
        if (url == null || request.channelIds.isEmpty()) {
            _state.value = EpgState(status = EpgStatus.Unavailable)
            return
        }

        _state.value = EpgState(status = EpgStatus.Loading)
        try {
            val programmes = withContext(Dispatchers.IO) {
                download(url, request.channelIds)
            }
            _state.value = EpgState(
                status = EpgStatus.Ready,
                programmes = programmes,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Never log the guide URL: it may contain a private token.
            val reason = when {
                error is java.net.SocketTimeoutException -> "tiempo agotado"
                error.message?.contains("supera el límite") == true -> "demasiado grande"
                error.message?.startsWith("El servidor respondió") == true -> error.message
                else -> null
            }
            Log.w(TAG, "Guía EPG no disponible (${error.javaClass.simpleName}${reason?.let { ": $it" } ?: ""})")
            _state.value = EpgState(
                status = EpgStatus.Error,
                message = "No se pudo cargar la guía EPG.",
            )
        }
    }

    private fun download(
        url: String,
        channelIds: Set<String>,
    ): Map<String, List<EpgProgramme>> {
        val httpUrl = url.toHttpUrlOrNull() ?: throw IOException("URL de EPG no válida")
        val request = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", "CobaltTV/0.4")
            .header("Accept", "application/xml, text/xml, */*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("El servidor respondió HTTP ${response.code}")
            }
            val body = response.body
            if (body.contentLength() > MAX_EPG_BYTES) {
                throw IOException("La guía EPG supera el límite")
            }
            body.byteStream().use { input ->
                LimitedInputStream(input, MAX_EPG_BYTES).use { limited ->
                    return EpgParser.parse(limited, channelIds)
                }
            }
        }
    }

    private data class EpgRequest(
        val epgUrl: String?,
        val channelIds: Set<String>,
        val refreshKey: Long,
    )

    private class LimitedInputStream(
        input: InputStream,
        private val maxBytes: Long,
    ) : FilterInputStream(input) {
        private var bytesRead = 0L

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) {
                bytesRead++
                if (bytesRead > maxBytes) throw IOException("La guía EPG supera el límite")
            }
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val read = super.read(buffer, offset, length)
            if (read > 0) {
                bytesRead += read
                if (bytesRead > maxBytes) throw IOException("La guía EPG supera el límite")
            }
            return read
        }
    }

    private companion object {
        const val TAG = "CobaltEpg"
        const val MAX_EPG_BYTES = 256L * 1024 * 1024
    }
}
