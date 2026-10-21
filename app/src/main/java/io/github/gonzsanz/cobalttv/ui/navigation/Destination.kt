package io.github.gonzsanz.cobalttv.ui.navigation

/**
 * Top level sections of the app. `Sources` is intentionally not here: it is
 * reached from Home and Settings, not from the main rail.
 */
enum class Destination(
    val route: String,
    val label: String,
    val shortLabel: String,
) {
    Live("live", "TV en directo", "TV"),
    Guide("guide", "Guía de TV", "Guía"),
    Movies("movies", "Películas", "Películas"),
    Series("series", "Series", "Series"),
    Favorites("favorites", "Favoritos", "Favoritos"),
    Search("search", "Buscar", "Buscar"),
    Settings("settings", "Ajustes", "Ajustes"),
    ;

    companion object {
        const val SOURCES_ROUTE = "sources"
        const val PLAYER_ROUTE = "player"
        const val SERIES_DETAIL_ROUTE = "series_detail"
    }
}
