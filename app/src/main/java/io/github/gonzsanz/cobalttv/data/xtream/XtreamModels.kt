package io.github.gonzsanz.cobalttv.data.xtream

import io.github.gonzsanz.cobalttv.data.catalog.Channel
import io.github.gonzsanz.cobalttv.data.catalog.SeriesItem
import io.github.gonzsanz.cobalttv.data.catalog.VodItem

/** Server, username and password entered by the user. Never log this object. */
data class XtreamCredentials(
    val server: String,
    val username: String,
    val password: String,
) {
    override fun toString(): String = "XtreamCredentials(redacted)"
}

data class XtreamAccount(
    val status: String? = null,
    val isTrial: Boolean = false,
    val maxConnections: Int? = null,
)

/** Normalized Xtream result. The UI consumes the same models as the M3U importer. */
data class XtreamCatalog(
    val channels: List<Channel>,
    val movies: List<VodItem> = emptyList(),
    val series: List<SeriesItem> = emptyList(),
    val account: XtreamAccount = XtreamAccount(),
)

/** User-facing Xtream failure. The message is already in Spanish. */
class XtreamException(message: String) : Exception(message)
