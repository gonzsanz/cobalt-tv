package io.github.gonzsanz.cobalttv.ui.model

import androidx.compose.ui.graphics.Color
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.catalog.Channel
import io.github.gonzsanz.cobalttv.data.demo.DemoChannel
import io.github.gonzsanz.cobalttv.data.demo.DemoStreams

/**
 * UI-friendly channel shared by demo data and imported M3U channels. Keeps the
 * cards independent from the source model and makes the demo/real switch a
 * one-line mapping.
 */
data class ChannelUi(
    val id: String,
    val name: String,
    val category: String,
    val nowPlaying: String,
    val accent: Color,
    val streamUrl: String,
    val isDemo: Boolean = false,
)

private val accentPalette = listOf(
    Color(0xFF3B6FF6),
    Color(0xFF2F8F8A),
    Color(0xFFE0673F),
    Color(0xFF6C5CE7),
    Color(0xFF2E9E5B),
    Color(0xFFD6457A),
    Color(0xFFE0A63F),
    Color(0xFF4C7FB8),
    Color(0xFF8A6A4B),
)

fun accentFor(seed: String): Color =
    accentPalette[(seed.hashCode() and Int.MAX_VALUE) % accentPalette.size]

/** "1 canal" / "8 canales", used in headers and source summaries. */
fun channelCountLabel(count: Int): String =
    if (count == 1) "1 canal" else "$count canales"

fun Channel.toUi(): ChannelUi = ChannelUi(
    id = id,
    name = name,
    category = group,
    nowPlaying = "En directo",
    accent = accentFor("$group|$name"),
    streamUrl = streamUrl,
)

fun DemoChannel.toUi(): ChannelUi = ChannelUi(
    id = id,
    name = name,
    category = category,
    nowPlaying = nowPlaying,
    accent = accent,
    streamUrl = DemoStreams.Hls,
    isDemo = true,
)

fun ChannelUi.toPlaybackRequest(): PlaybackRequest = PlaybackRequest(
    title = name,
    subtitle = "$category · $nowPlaying",
    url = streamUrl,
    isLive = true,
)
