package io.github.gonzsanz.cobalttv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.gonzsanz.cobalttv.core.playback.PlayerHolder

val LocalPlayer = staticCompositionLocalOf<PlayerHolder> {
    error("PlayerHolder no proporcionado")
}
