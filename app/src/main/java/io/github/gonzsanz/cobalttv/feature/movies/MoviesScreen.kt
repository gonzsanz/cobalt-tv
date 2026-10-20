package io.github.gonzsanz.cobalttv.feature.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.ui.components.PosterCard

@Composable
fun MoviesScreen(onToast: (String) -> Unit) {
    val firstPosterFocusRequester = remember { FocusRequester() }
    InitialFocusEffect(firstPosterFocusRequester)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Películas",
            subtitle = "${DemoCatalog.movies.size} títulos de demostración. " +
                "La ficha y la reproducción llegarán en la Fase 6.",
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 170.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 48.dp, top = 10.dp, end = 48.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(DemoCatalog.movies, key = { _, movie -> movie.id }) { index, movie ->
                PosterCard(
                    title = movie.title,
                    subtitle = "${movie.year} · ${movie.genre}",
                    accent = movie.accent,
                    onClick = { onToast("La ficha de «${movie.title}» llegará en la Fase 6.") },
                    modifier = if (index == 0) Modifier.focusRequester(firstPosterFocusRequester) else Modifier,
                )
            }
        }
    }
}
