package io.github.gonzsanz.cobalttv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.gonzsanz.cobalttv.data.epg.EpgRepository

val LocalEpg = staticCompositionLocalOf<EpgRepository> {
    error("EpgRepository no proporcionado")
}
