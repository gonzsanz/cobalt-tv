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

enum class SourceKind { Url, File, Xtream }

data class PlaylistSource(
    val kind: SourceKind,
    val name: String,
    /** URL for network playlists or Xtream servers, display name for local files. */
    val location: String,
    val channelCount: Int,
    val importedAt: Long,
    /** XMLTV guide URL declared by the playlist header, if any. */
    val epgUrl: String? = null,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
)

/** A normalized movie from Xtream Codes. Playback arrives in Phase 6. */
data class VodItem(
    val id: String,
    /** Provider identifier, kept for future detail requests. */
    val sourceId: String,
    val name: String,
    val streamUrl: String,
    val group: String,
    val logoUrl: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val plot: String? = null,
    val containerExtension: String? = null,
)

/** A normalized series entry. Seasons and episodes arrive in Phase 6. */
data class SeriesItem(
    val id: String,
    /** Provider series_id, needed to request seasons and episodes later. */
    val sourceId: String,
    val name: String,
    val group: String,
    val coverUrl: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val plot: String? = null,
)

/** One playable episode of a series, resolved on demand from the provider. */
data class SeriesEpisode(
    val id: String,
    val season: Int,
    val number: Int,
    val title: String,
    val streamUrl: String,
    val plot: String? = null,
    val duration: String? = null,
)

/** Extra movie metadata requested from the provider when a title is opened. */
data class VodDetail(
    val plot: String? = null,
    val genre: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val releaseDate: String? = null,
)

data class SeriesDetail(
    val plot: String?,
    val episodes: List<SeriesEpisode>,
)

data class CatalogState(
    val source: PlaylistSource? = null,
    val channels: List<Channel> = emptyList(),
    val movies: List<VodItem> = emptyList(),
    val series: List<SeriesItem> = emptyList(),
    val isLoading: Boolean = true,
    val importError: String? = null,
    /** Which form should show the error when both importers are on screen. */
    val importErrorTarget: SourceKind? = null,
) {
    val hasSource: Boolean get() = source != null
}

/** User-facing import failure. The message is already in Spanish. */
class CatalogException(message: String) : Exception(message)
