package io.github.gonzsanz.cobalttv.feature.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButton
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSoft
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSpinner
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.catalog.VodDetail
import io.github.gonzsanz.cobalttv.data.catalog.VodItem
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Hand-off of the selected movie to the detail screen. */
object MovieSession {
    var current: VodItem? = null
}

@Composable
fun MovieDetailScreen(onPlay: (PlaybackRequest) -> Unit) {
    val movie = remember { MovieSession.current }
    if (movie == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay ninguna película seleccionada", color = TextSecondary)
        }
        return
    }

    val catalog = LocalCatalog.current
    var detail by remember { mutableStateOf<VodDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(movie.id) {
        detail = try {
            catalog.loadVodDetail(movie)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
        loading = false
    }

    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(8) {
            delay(80)
            if (runCatching { playFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }

    val plot = detail?.plot ?: movie.plot
    val rating = detail?.rating ?: movie.rating?.takeIf { it != "0" }
    val facts = listOfNotNull(
        movie.year ?: detail?.releaseDate?.take(4),
        detail?.duration,
        rating?.let { "★ $it" },
        detail?.genre ?: movie.group.takeIf { it.isNotBlank() },
    )

    Row(Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 16.dp)) {
        Box(
            Modifier
                .width(210.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            if (!movie.logoUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = movie.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {},
                    error = {},
                )
            }
        }
        Spacer(Modifier.width(26.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = movie.name,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = facts.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = CobaltSoft,
            )
            Spacer(Modifier.height(16.dp))
            CobaltButton(
                text = "Reproducir",
                onClick = {
                    onPlay(
                        PlaybackRequest(
                            title = movie.name,
                            subtitle = facts.joinToString(" · "),
                            url = movie.streamUrl,
                        ),
                    )
                },
                modifier = Modifier.focusRequester(playFocus),
            )
            Spacer(Modifier.height(18.dp))

            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (loading && plot.isNullOrBlank()) {
                    CobaltSpinner(size = 28.dp)
                } else {
                    Text(
                        text = plot?.takeIf { it.isNotBlank() } ?: "Sin descripción disponible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                detail?.director?.let { Meta("Dirección", it) }
                detail?.cast?.let { Meta("Reparto", it) }
            }
        }
    }
}

@Composable
private fun Meta(label: String, value: String) {
    Spacer(Modifier.height(12.dp))
    Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
    Spacer(Modifier.height(2.dp))
    Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
}
