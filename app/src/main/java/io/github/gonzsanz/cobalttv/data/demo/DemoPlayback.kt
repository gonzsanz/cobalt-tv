package io.github.gonzsanz.cobalttv.data.demo

import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest

/**
 * Legal public test media used until real M3U / Xtream sources are connected.
 * Both URLs are served over HTTPS so they also work on devices without
 * cleartext exemptions.
 */
object DemoStreams {
    /** HLS test stream (Big Buck Bunny, about 10 minutes). */
    const val Hls = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    /** Progressive MP4 test stream, used to exercise the non-HLS path. */
    const val Progressive = "https://media.w3.org/2010/05/sintel/trailer.mp4"
}

fun DemoChannel.toPlaybackRequest() = PlaybackRequest(
    title = name,
    subtitle = "$category · $nowPlaying",
    url = DemoStreams.Hls,
    isLive = true,
)

fun DemoMovie.toPlaybackRequest() = PlaybackRequest(
    title = title,
    subtitle = "$year · $genre",
    url = DemoStreams.Progressive,
    isLive = false,
)

fun DemoSeries.toPlaybackRequest() = PlaybackRequest(
    title = title,
    subtitle = "$seasons temporada(s) · $genre",
    url = DemoStreams.Progressive,
    isLive = false,
)

fun DemoContinueWatching.toPlaybackRequest() = PlaybackRequest(
    title = title,
    subtitle = subtitle,
    url = if (isLive) DemoStreams.Hls else DemoStreams.Progressive,
    isLive = isLive,
)

fun DemoSearchHit.toPlaybackRequest() = PlaybackRequest(
    title = title,
    subtitle = subtitle,
    url = streamUrl ?: if (kind == SearchKind.Channel) {
        DemoStreams.Hls
    } else {
        DemoStreams.Progressive
    },
    isLive = kind == SearchKind.Channel,
)
