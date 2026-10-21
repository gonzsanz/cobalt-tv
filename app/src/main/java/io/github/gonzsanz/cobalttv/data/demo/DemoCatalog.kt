package io.github.gonzsanz.cobalttv.data.demo

import androidx.compose.ui.graphics.Color

/**
 * Demo-only models used by the Phase 1 UI.
 *
 * None of these entries point to a real stream: they exist to design and test
 * layout, focus and navigation before the M3U / Xtream imports are implemented.
 * Every screen that renders them shows a "DATOS DE DEMO" badge.
 */
data class DemoChannel(
    val id: String,
    val name: String,
    val category: String,
    val nowPlaying: String,
    val accent: Color,
)

data class DemoMovie(
    val id: String,
    val title: String,
    val year: Int,
    val genre: String,
    val synopsis: String,
    val accent: Color,
)

data class DemoSeries(
    val id: String,
    val title: String,
    val seasons: Int,
    val genre: String,
    val synopsis: String,
    val accent: Color,
)

data class DemoContinueWatching(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val accent: Color,
    val isLive: Boolean = false,
)

data class DemoSearchHit(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: SearchKind,
    val accent: Color,
    val streamUrl: String? = null,
)

enum class SearchKind(val label: String) {
    Channel("Canal"),
    Movie("Película"),
    Series("Serie"),
}

object DemoCatalog {

    val categories = listOf(
        "Todos",
        "Deportes",
        "Noticias",
        "Cine",
        "Documentales",
        "Música",
        "Infantil",
    )

    val channels = listOf(
        DemoChannel(
            id = "ch-01",
            name = "Cobalt One",
            category = "Cine",
            nowPlaying = "Sesión de madrugada",
            accent = Color(0xFF3B6FF6),
        ),
        DemoChannel(
            id = "ch-02",
            name = "Aurora Noticias",
            category = "Noticias",
            nowPlaying = "Resumen internacional",
            accent = Color(0xFF2F8F8A),
        ),
        DemoChannel(
            id = "ch-03",
            name = "Deportes 24",
            category = "Deportes",
            nowPlaying = "Mesa de análisis",
            accent = Color(0xFFE0673F),
        ),
        DemoChannel(
            id = "ch-04",
            name = "Cine Nocturno",
            category = "Cine",
            nowPlaying = "Ciclo de cine negro",
            accent = Color(0xFF6C5CE7),
        ),
        DemoChannel(
            id = "ch-05",
            name = "Planeta Salvaje",
            category = "Documentales",
            nowPlaying = "Grandes migraciones",
            accent = Color(0xFF2E9E5B),
        ),
        DemoChannel(
            id = "ch-06",
            name = "Pulso Musical",
            category = "Música",
            nowPlaying = "Directo desde el estudio",
            accent = Color(0xFFD6457A),
        ),
        DemoChannel(
            id = "ch-07",
            name = "Pequeños Curiosos",
            category = "Infantil",
            nowPlaying = "Taller de inventos",
            accent = Color(0xFFE0A63F),
        ),
        DemoChannel(
            id = "ch-08",
            name = "Archivo Clásico",
            category = "Cine",
            nowPlaying = "Joyas restauradas",
            accent = Color(0xFF8A6A4B),
        ),
    )

    val movies = listOf(
        DemoMovie(
            id = "mv-01",
            title = "Órbita",
            year = 2024,
            genre = "Ciencia ficción",
            synopsis = "Una tripulación aislada detecta una señal que no debería existir.",
            accent = Color(0xFF3B6FF6),
        ),
        DemoMovie(
            id = "mv-02",
            title = "La Última Señal",
            year = 2022,
            genre = "Thriller",
            synopsis = "Un técnico de radio descubre un mensaje enterrado durante décadas.",
            accent = Color(0xFF6C5CE7),
        ),
        DemoMovie(
            id = "mv-03",
            title = "Horizonte Rojo",
            year = 2025,
            genre = "Aventura",
            synopsis = "Dos hermanos cruzan un desierto para cumplir una promesa.",
            accent = Color(0xFFE0673F),
        ),
        DemoMovie(
            id = "mv-04",
            title = "Ciudad de Cristal",
            year = 2021,
            genre = "Drama",
            synopsis = "Un arquitecto reconstruye su barrio mientras pierde la memoria.",
            accent = Color(0xFF2F8F8A),
        ),
        DemoMovie(
            id = "mv-05",
            title = "Bajo Cero",
            year = 2023,
            genre = "Suspense",
            synopsis = "Una estación polar queda incomunicada en plena tormenta.",
            accent = Color(0xFF4C7FB8),
        ),
        DemoMovie(
            id = "mv-06",
            title = "Eco",
            year = 2026,
            genre = "Misterio",
            synopsis = "Cada noche, la misma voz repite una frase distinta.",
            accent = Color(0xFFD6457A),
        ),
    )

    val series = listOf(
        DemoSeries(
            id = "sr-01",
            title = "Protocolo",
            seasons = 2,
            genre = "Thriller",
            synopsis = "Un equipo de analistas desmonta una red de mensajes cifrados.",
            accent = Color(0xFF3B6FF6),
        ),
        DemoSeries(
            id = "sr-02",
            title = "Mareas",
            seasons = 1,
            genre = "Drama",
            synopsis = "Tres generaciones de una familia que vive del mar.",
            accent = Color(0xFF2F8F8A),
        ),
        DemoSeries(
            id = "sr-03",
            title = "Estación 7",
            seasons = 3,
            genre = "Ciencia ficción",
            synopsis = "La última estación orbital recibe una visita inesperada.",
            accent = Color(0xFF6C5CE7),
        ),
        DemoSeries(
            id = "sr-04",
            title = "Los Archivos",
            seasons = 1,
            genre = "Documental",
            synopsis = "Historias reales recuperadas de archivos olvidados.",
            accent = Color(0xFFE0A63F),
        ),
    )

    val continueWatching = listOf(
        DemoContinueWatching(
            id = "cw-01",
            title = "Órbita",
            subtitle = "Película · 48 min restantes",
            progress = 0.62f,
            accent = Color(0xFF3B6FF6),
        ),
        DemoContinueWatching(
            id = "cw-02",
            title = "Protocolo",
            subtitle = "T1 · E3 · 12 min restantes",
            progress = 0.34f,
            accent = Color(0xFF6C5CE7),
        ),
        DemoContinueWatching(
            id = "cw-03",
            title = "Planeta Salvaje",
            subtitle = "Directo · Grandes migraciones",
            progress = 0.86f,
            accent = Color(0xFF2E9E5B),
            isLive = true,
        ),
    )

    fun channelsByCategory(category: String): List<DemoChannel> =
        if (category == "Todos") channels else channels.filter { it.category == category }

    fun search(query: String): List<DemoSearchHit> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()

        val channelHits = channels
            .filter { it.name.contains(term, ignoreCase = true) || it.category.contains(term, ignoreCase = true) }
            .map {
                DemoSearchHit(
                    id = it.id,
                    title = it.name,
                    subtitle = "${it.category} · ${it.nowPlaying}",
                    kind = SearchKind.Channel,
                    accent = it.accent,
                )
            }

        val movieHits = movies
            .filter { it.title.contains(term, ignoreCase = true) || it.genre.contains(term, ignoreCase = true) }
            .map {
                DemoSearchHit(
                    id = it.id,
                    title = it.title,
                    subtitle = "${it.year} · ${it.genre}",
                    kind = SearchKind.Movie,
                    accent = it.accent,
                )
            }

        val seriesHits = series
            .filter { it.title.contains(term, ignoreCase = true) || it.genre.contains(term, ignoreCase = true) }
            .map {
                DemoSearchHit(
                    id = it.id,
                    title = it.title,
                    subtitle = "${it.seasons} temporada(s) · ${it.genre}",
                    kind = SearchKind.Series,
                    accent = it.accent,
                )
            }

        return channelHits + movieHits + seriesHits
    }
}
