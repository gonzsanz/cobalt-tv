package io.github.gonzsanz.cobalttv.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import io.github.gonzsanz.cobalttv.core.designsystem.CategoryChip
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButton
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSoft
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSpinner
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.catalog.SeriesDetail
import io.github.gonzsanz.cobalttv.data.catalog.SeriesEpisode
import io.github.gonzsanz.cobalttv.data.catalog.SeriesItem
import io.github.gonzsanz.cobalttv.data.xtream.XtreamException
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private sealed interface DetailState {
    data object Loading : DetailState
    data class Ready(val detail: SeriesDetail) : DetailState
    data class Failed(val message: String) : DetailState
}

@Composable
fun SeriesDetailScreen(onPlay: (PlaybackRequest) -> Unit) {
    val series = remember { SeriesSession.current }
    if (series == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay ninguna serie seleccionada", color = TextSecondary)
        }
        return
    }

    val catalog = LocalCatalog.current
    var state by remember { mutableStateOf<DetailState>(DetailState.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(series.id, retry) {
        state = DetailState.Loading
        state = try {
            DetailState.Ready(catalog.loadSeriesDetail(series))
        } catch (error: CancellationException) {
            throw error
        } catch (error: XtreamException) {
            DetailState.Failed(error.message ?: "No se pudieron cargar los episodios.")
        } catch (error: Exception) {
            DetailState.Failed("No se pudieron cargar los episodios. Revisa la conexión.")
        }
    }

    Row(Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 16.dp)) {
        Column(Modifier.width(210.dp)) {
            Box(
                Modifier
                    .width(210.dp)
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                if (!series.coverUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = series.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {},
                        error = {},
                    )
                }
            }
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(series.year, series.rating?.takeIf { it != "0" }, series.group)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = CobaltSoft,
            )
            val plot = (state as? DetailState.Ready)?.detail?.plot ?: series.plot
            if (!plot.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = plot,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))

            when (val current = state) {
                DetailState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CobaltSpinner(size = 40.dp) }

                is DetailState.Failed -> Column {
                    Text(current.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    CobaltButton(text = "Reintentar", onClick = { retry++ })
                }

                is DetailState.Ready -> Episodes(series, current.detail, onPlay)
            }
        }
    }
}

@Composable
private fun Episodes(
    series: SeriesItem,
    detail: SeriesDetail,
    onPlay: (PlaybackRequest) -> Unit,
) {
    if (detail.episodes.isEmpty()) {
        Text(
            text = "Esta serie no tiene episodios disponibles.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        return
    }
    val seasons = remember(detail) { detail.episodes.map { it.season }.distinct().sorted() }
    var season by remember(detail) { mutableIntStateOf(seasons.first()) }
    val episodes = remember(detail, season) { detail.episodes.filter { it.season == season } }
    val firstEpisode = remember { FocusRequester() }
    LaunchedEffect(detail) {
        repeat(8) {
            delay(80)
            if (runCatching { firstEpisode.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }

    if (seasons.size > 1) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(seasons, key = { it }) { number ->
                CategoryChip(
                    label = "Temporada $number",
                    selected = number == season,
                    onClick = { season = number },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(episodes, key = { it.id }) { episode ->
            EpisodeRow(
                episode = episode,
                modifier = if (episode == episodes.first()) Modifier.focusRequester(firstEpisode) else Modifier,
                onClick = {
                    onPlay(
                        PlaybackRequest(
                            title = series.name,
                            subtitle = "T${episode.season} · E${episode.number} · ${episode.title}",
                            url = episode.streamUrl,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun EpisodeRow(episode: SeriesEpisode, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(if (focused) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${episode.number}",
            style = MaterialTheme.typography.titleSmall,
            color = CobaltSoft,
            modifier = Modifier.width(36.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!episode.plot.isNullOrBlank()) {
                Text(
                    text = episode.plot,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!episode.duration.isNullOrBlank()) {
            Text(episode.duration, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
