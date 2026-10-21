package io.github.gonzsanz.cobalttv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.gonzsanz.cobalttv.data.favorites.FavoritesRepository

/**
 * Favourites repository available to every screen. Screens observe its flow;
 * the provider in [CobaltTvApp] owns its lifetime.
 */
val LocalFavorites = staticCompositionLocalOf<FavoritesRepository> {
    error("LocalFavorites no está inicializado")
}
