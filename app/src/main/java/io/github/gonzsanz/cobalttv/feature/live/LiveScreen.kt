package io.github.gonzsanz.cobalttv.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.CategoryChip
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSpinner
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import io.github.gonzsanz.cobalttv.ui.LocalFavorites
import io.github.gonzsanz.cobalttv.ui.components.ChannelGrid
import io.github.gonzsanz.cobalttv.ui.model.ChannelUi
import io.github.gonzsanz.cobalttv.ui.model.channelCountLabel
import io.github.gonzsanz.cobalttv.ui.model.toPlaybackRequest
import io.github.gonzsanz.cobalttv.ui.model.toUi
import kotlinx.coroutines.delay

@Composable
fun LiveScreen(
    onPlay: (PlaybackRequest) -> Unit,
    onToast: (String) -> Unit = {},
) {
    val catalog by LocalCatalog.current.state.collectAsState()
    val favoritesRepo = LocalFavorites.current
    val favorites by favoritesRepo.favorites.collectAsState()
    val hasRealChannels = catalog.channels.isNotEmpty()

    val categories = remember(hasRealChannels, catalog.channels) {
        if (!hasRealChannels) {
            DemoCatalog.categories
        } else {
            buildList {
                add("Todos")
                catalog.channels.asSequence()
                    .map { it.group }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sortedBy { it.lowercase() }
                    .forEach(::add)
            }
        }
    }

    var selectedCategory by rememberSaveable { mutableStateOf("Todos") }
    LaunchedEffect(categories) {
        if (selectedCategory !in categories) selectedCategory = "Todos"
    }

    val allChannels: List<ChannelUi> = remember(hasRealChannels, catalog.channels) {
        if (hasRealChannels) {
            catalog.channels.map { it.toUi() }
        } else {
            DemoCatalog.channels.map { it.toUi() }
        }
    }
    val channels = remember(allChannels, selectedCategory) {
        if (selectedCategory == "Todos") {
            allChannels
        } else {
            allChannels.filter { it.category == selectedCategory }
        }
    }

    val firstChipFocusRequester = remember { FocusRequester() }
    LaunchedEffect(categories.size, catalog.isLoading) {
        if (catalog.isLoading && !hasRealChannels) return@LaunchedEffect
        repeat(10) {
            delay(80)
            if (runCatching { firstChipFocusRequester.requestFocus() }.isSuccess) {
                return@LaunchedEffect
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "TV en directo",
            subtitle = when {
                hasRealChannels && catalog.source != null ->
                    "${channelCountLabel(catalog.channels.size)} de «${catalog.source?.name}»."
                hasRealChannels -> if (catalog.channels.size == 1) {
                    "1 canal importado."
                } else {
                    "${catalog.channels.size} canales importados."
                }
                else -> "Elige una categoría y un canal. " +
                    "Reproducción con un stream público de prueba."
            },
            showDemoBadge = !hasRealChannels,
        )

        if (catalog.isLoading && !hasRealChannels) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CobaltSpinner(size = 44.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Cargando canales…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            return@Column
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(categories, key = { _, category -> category }) { index, category ->
                CategoryChip(
                    label = if (category.length > 28) category.take(27) + "…" else category,
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstChipFocusRequester)
                    } else {
                        Modifier
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        ChannelGrid(
            channels = channels,
            onChannelClick = { channel ->
                onPlay(channel.toPlaybackRequest())
            },
            favoriteIds = favorites,
            onChannelLongClick = { channel ->
                val added = favoritesRepo.toggle(channel.id)
                onToast(if (added) "Añadido a favoritos" else "Quitado de favoritos")
            },
            modifier = Modifier.weight(1f),
        )

        Text(
            text = "OK: ver · Mantén OK para añadir o quitar de favoritos",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(start = 48.dp, bottom = 14.dp),
        )
    }
}
