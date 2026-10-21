package io.github.gonzsanz.cobalttv.feature.series

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
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.data.demo.toPlaybackRequest
import io.github.gonzsanz.cobalttv.ui.components.PosterCard

@Composable
fun SeriesScreen(onPlay: (PlaybackRequest) -> Unit) {
    val firstPosterFocusRequester = remember { FocusRequester() }
    InitialFocusEffect(firstPosterFocusRequester)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Series",
            subtitle = "${DemoCatalog.series.size} series de demostración. " +
                "Pulsa una serie para reproducir un vídeo de prueba.",
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 170.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 48.dp, top = 10.dp, end = 48.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(DemoCatalog.series, key = { _, series -> series.id }) { index, series ->
                PosterCard(
                    title = series.title,
                    subtitle = "${series.seasons} temp. · ${series.genre}",
                    accent = series.accent,
                    onClick = { onPlay(series.toPlaybackRequest()) },
                    modifier = if (index == 0) Modifier.focusRequester(firstPosterFocusRequester) else Modifier,
                )
            }
        }
    }
}
