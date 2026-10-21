package io.github.gonzsanz.cobalttv.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.gonzsanz.cobalttv.data.epg.EpgState
import io.github.gonzsanz.cobalttv.data.epg.EpgStatus
import kotlinx.coroutines.delay

/** Recomputes now/next once a minute while the screen is visible. */
@Composable
fun rememberCurrentTimeMs(intervalMs: Long = 60_000L): Long {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            delay(intervalMs)
            currentTimeMs = System.currentTimeMillis()
        }
    }
    return currentTimeMs
}

/** Full status for the TV header; "Sin guía EPG" covers lists without a guide. */
fun EpgState.guideStatusLabel(): String = when (status) {
    EpgStatus.Unavailable -> "Sin guía EPG"
    EpgStatus.Loading -> "Cargando guía EPG…"
    EpgStatus.Ready -> if (programmes.isEmpty()) {
        "Guía EPG sin programación"
    } else {
        "Guía EPG disponible"
    }
    EpgStatus.Error -> "Guía EPG no disponible"
}

/** Short status for the source card. */
fun EpgState.shortStatusLabel(): String = when (status) {
    EpgStatus.Unavailable -> "sin canales compatibles"
    EpgStatus.Loading -> "cargando…"
    EpgStatus.Ready -> if (programmes.isEmpty()) "sin programación" else "disponible"
    EpgStatus.Error -> "no disponible"
}

fun ChannelUi.epgNowTitle(epg: EpgState, atMs: Long): String {
    if (isDemo || epg.status != EpgStatus.Ready) return nowPlaying
    return epg.nowNext(epgId, atMs)?.now?.title ?: "Sin programación"
}

fun List<ChannelUi>.epgNowTitles(epg: EpgState, atMs: Long): Map<String, String> {
    if (epg.status != EpgStatus.Ready) return emptyMap()
    return associate { channel -> channel.id to channel.epgNowTitle(epg, atMs) }
}
