package io.github.gonzsanz.cobalttv.feature.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.EmptyState
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.catalog.VodItem
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import io.github.gonzsanz.cobalttv.ui.components.CategorySidebar
import io.github.gonzsanz.cobalttv.ui.components.VodPosterCard

private const val ALL = "Todas"

@Composable
fun MoviesScreen(onPlay: (PlaybackRequest) -> Unit) {
    val catalog by LocalCatalog.current.state.collectAsState()
    val movies = catalog.movies

    if (movies.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "Películas",
                subtitle = if (catalog.isLoading) {
                    "Cargando tu fuente…"
                } else {
                    "Las películas de tu cuenta aparecerán aquí."
                },
                showDemoBadge = false,
            )
            if (!catalog.isLoading) {
                EmptyState(
                    icon = CobaltIcons.Movies,
                    title = "Sin películas",
                    description = "La fuente activa no incluye películas. " +
                        "Añade una cuenta Xtream Codes en Ajustes → Fuentes.",
                )
            }
        }
        return
    }

    val categories = remember(movies) {
        listOf(ALL) + movies.map { it.group }.filter { it.isNotBlank() }.distinct()
            .sortedBy { it.lowercase() }
    }
    val counts = remember(movies) {
        buildMap {
            put(ALL, movies.size)
            movies.groupingBy { it.group }.eachCount().forEach { (k, v) -> put(k, v) }
        }
    }
    var selected by rememberSaveable { mutableStateOf(ALL) }
    LaunchedEffect(categories) { if (selected !in categories) selected = ALL }
    val visible = remember(movies, selected) {
        if (selected == ALL) movies else movies.filter { it.group == selected }
    }
    val firstPoster = remember { FocusRequester() }
    InitialFocusEffect(firstPoster)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Películas",
            subtitle = "${movies.size} títulos de «${catalog.source?.name}»",
            showDemoBadge = false,
        )
        Row(Modifier.fillMaxSize().padding(start = 24.dp, bottom = 12.dp)) {
            CategorySidebar(
                categories = categories,
                counts = counts,
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.width(190.dp).fillMaxHeight(),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                itemsIndexed(visible, key = { _, movie -> movie.id }) { index, movie ->
                    VodPosterCard(
                        title = movie.name,
                        subtitle = listOfNotNull(movie.year, movie.rating?.takeIf { it != "0" })
                            .joinToString(" · "),
                        imageUrl = movie.logoUrl,
                        onClick = { onPlay(movie.toPlaybackRequest()) },
                        modifier = if (index == 0) Modifier.focusRequester(firstPoster) else Modifier,
                    )
                }
            }
            }
    }
}

private fun VodItem.toPlaybackRequest() = PlaybackRequest(
    title = name,
    subtitle = listOfNotNull(year, group.takeIf { it.isNotBlank() }).joinToString(" · "),
    url = streamUrl,
)
