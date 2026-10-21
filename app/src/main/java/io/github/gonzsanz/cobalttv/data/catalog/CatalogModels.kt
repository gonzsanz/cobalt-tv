package io.github.gonzsanz.cobalttv.data.catalog

/** A playable live channel, independent of the source that produced it. */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val group: String,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val number: Int? = null,
)

enum class SourceKind { Url, File }

data class PlaylistSource(
    val kind: SourceKind,
    val name: String,
    /** URL for network playlists, original display name for local files. */
    val location: String,
    val channelCount: Int,
    val importedAt: Long,
    /** XMLTV guide URL declared by the playlist header, if any. */
    val epgUrl: String? = null,
)

data class CatalogState(
    val source: PlaylistSource? = null,
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = true,
    val importError: String? = null,
) {
    val hasSource: Boolean get() = source != null
}

/** User-facing import failure. The message is already in Spanish. */
class CatalogException(message: String) : Exception(message)
