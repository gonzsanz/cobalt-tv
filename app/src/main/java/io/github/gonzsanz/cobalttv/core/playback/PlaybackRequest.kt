package io.github.gonzsanz.cobalttv.core.playback

/**
 * One channel available for zapping while a live stream is playing. It only
 * carries what the player needs, so switching channels never logs or exposes
 * the source URL.
 */
data class LiveChannel(
    val id: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val epgId: String? = null,
    val category: String = "",
    val logoUrl: String? = null,
)

/**
 * Everything the player needs to start playback. It is intentionally small and
 * source agnostic: M3U and Xtream importers will produce the same object.
 *
 * [liveChannels] is only populated for a live channel played from a channel
 * list. It lets the player change channel with the D-pad without navigating
 * back to the list.
 */
data class PlaybackRequest(
    val title: String,
    val subtitle: String,
    val url: String,
    val isLive: Boolean = false,
    val liveChannelId: String? = null,
    val epgId: String? = null,
    val liveChannels: List<LiveChannel> = emptyList(),
)

/**
 * In-memory hand-off between a content screen and the player destination.
 * A player deep link with an encoded URL would be more fragile (long tokens,
 * credentials in the back stack), and this app is a single-process client.
 */
object PlaybackSession {
    var current: PlaybackRequest? = null
}
