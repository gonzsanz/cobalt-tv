package io.github.gonzsanz.cobalttv.core.playback

/**
 * Everything the player needs to start playback. It is intentionally small and
 * source agnostic: M3U and Xtream importers will produce the same object.
 */
data class PlaybackRequest(
    val title: String,
    val subtitle: String,
    val url: String,
    val isLive: Boolean = false,
)

/**
 * In-memory hand-off between a content screen and the player destination.
 * A player deep link with an encoded URL would be more fragile (long tokens,
 * credentials in the back stack), and this app is a single-process client.
 */
object PlaybackSession {
    var current: PlaybackRequest? = null
}
