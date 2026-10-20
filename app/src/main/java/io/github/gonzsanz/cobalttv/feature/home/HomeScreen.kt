package io.github.gonzsanz.cobalttv.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.Cobalt
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButton
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButtonStyle
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.SectionHeader
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.ui.components.ChannelCard
import io.github.gonzsanz.cobalttv.ui.components.ContinueWatchingCard
import io.github.gonzsanz.cobalttv.ui.components.PosterCard
import io.github.gonzsanz.cobalttv.ui.navigation.Destination

@Composable
fun HomeScreen(
    onNavigate: (Destination) -> Unit,
    onOpenSources: () -> Unit,
    onToast: (String) -> Unit,
) {
    val heroFocusRequester = remember { FocusRequester() }
    InitialFocusEffect(heroFocusRequester)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            ScreenHeader(
                title = "Inicio",
                subtitle = "Tu televisión, tus fuentes, en un solo mando.",
            )
        }

        item {
            HeroBanner(
                primaryFocusRequester = heroFocusRequester,
                onLiveClick = { onNavigate(Destination.Live) },
                onSourceClick = onOpenSources,
            )
        }

        item {
            SectionHeader(title = "Continuar viendo", trailing = "Solo demo")
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(DemoCatalog.continueWatching, key = { it.id }) { item ->
                    ContinueWatchingCard(
                        item = item,
                        onClick = { onToast("«${item.title}» se reproducirá en la Fase 2.") },
                    )
                }
            }
        }

        item {
            SectionHeader(title = "TV en directo", trailing = "${DemoCatalog.channels.size} canales demo")
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(DemoCatalog.channels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { onToast("«${channel.name}» se reproducirá en la Fase 2.") },
                    )
                }
            }
        }

        item {
            SectionHeader(title = "Películas", trailing = "${DemoCatalog.movies.size} títulos demo")
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(DemoCatalog.movies, key = { it.id }) { movie ->
                    PosterCard(
                        title = movie.title,
                        subtitle = "${movie.year} · ${movie.genre}",
                        accent = movie.accent,
                        onClick = { onToast("La ficha de «${movie.title}» llegará en la Fase 6.") },
                    )
                }
            }
        }

        item {
            SectionHeader(title = "Series", trailing = "${DemoCatalog.series.size} títulos demo")
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(DemoCatalog.series, key = { it.id }) { series ->
                    PosterCard(
                        title = series.title,
                        subtitle = "${series.seasons} temp. · ${series.genre}",
                        accent = series.accent,
                        onClick = { onToast("La ficha de «${series.title}» llegará en la Fase 6.") },
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Cobalt TV 0.1.0 · Fase 1 · Contenido de demostración, sin emisiones reales",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    primaryFocusRequester: FocusRequester,
    onLiveClick: () -> Unit,
    onSourceClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp)
            .height(340.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0D1730),
                        Color(0xFF162C58),
                        Color(0xFF0A1224),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Cobalt.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.15f),
                    radius = size.width * 0.42f,
                ),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.78f, size.height * 0.15f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.height * 0.62f,
                center = Offset(size.width * 0.92f, size.height * 0.9f),
            )
            drawCircle(
                color = Cobalt.copy(alpha = 0.12f),
                radius = size.height * 0.38f,
                center = Offset(size.width * 0.7f, size.height * 0.75f),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.68f)
                .padding(40.dp),
        ) {
            Text(
                text = "BIENVENIDO A",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                color = Color(0xFF8FAEFF),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Cobalt TV",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "TV en directo, películas y series desde tus propias fuentes. " +
                    "Sin cuentas de terceros ni servicios intermedios.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CobaltButton(
                    text = "Ver TV en directo",
                    onClick = onLiveClick,
                    icon = CobaltIcons.Play,
                    modifier = Modifier.focusRequester(primaryFocusRequester),
                )
                CobaltButton(
                    text = "Añadir fuente",
                    onClick = onSourceClick,
                    style = CobaltButtonStyle.Secondary,
                )
            }
        }
    }
}
