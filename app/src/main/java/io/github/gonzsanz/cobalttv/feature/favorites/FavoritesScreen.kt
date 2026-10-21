package io.github.gonzsanz.cobalttv.feature.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSpinner
import io.github.gonzsanz.cobalttv.core.designsystem.EmptyState
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import io.github.gonzsanz.cobalttv.ui.LocalFavorites
import io.github.gonzsanz.cobalttv.ui.components.ChannelGrid
import io.github.gonzsanz.cobalttv.ui.model.toPlaybackRequest
import io.github.gonzsanz.cobalttv.ui.model.toUi
import kotlinx.coroutines.delay

@Composable
fun FavoritesScreen(
    onPlay: (PlaybackRequest) -> Unit,
    onExploreLive: () -> Unit,
    onToast: (String) -> Unit = {},
) {
    val catalog by LocalCatalog.current.state.collectAsState()
    val favoritesRepo = LocalFavorites.current
    val favorites by favoritesRepo.favorites.collectAsState()

    val realChannels = remember(catalog.channels) {
        catalog.channels.map { it.toUi() }
    }
    val demoChannels = remember {
        DemoCatalog.channels.map { it.toUi() }
    }
    val availableChannels = remember(realChannels, demoChannels) {
        (realChannels + demoChannels).distinctBy { it.id }
    }
    val favoriteChannels = remember(availableChannels, favorites) {
        availableChannels.filter { it.id in favorites }
    }
    val unavailableCount = (favorites.size - favoriteChannels.size).coerceAtLeast(0)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Favoritos",
            subtitle = when {
                favoriteChannels.size == 1 -> "1 canal guardado."
                favoriteChannels.isNotEmpty() -> "${favoriteChannels.size} canales guardados."
                favorites.isNotEmpty() -> "Tus canales guardados no están en la lista activa."
                else -> "Guarda canales con el mando para tenerlos siempre a mano."
            },
            showDemoBadge = favoriteChannels.any { it.isDemo },
        )

        when {
            catalog.isLoading && favorites.isNotEmpty() -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CobaltSpinner(size = 44.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Cargando favoritos…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            favoriteChannels.isEmpty() -> {
                val actionFocusRequester = remember { FocusRequester() }
                InitialFocusEffect(actionFocusRequester)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = CobaltIcons.Heart,
                        title = if (favorites.isEmpty()) {
                            "Todavía no hay favoritos"
                        } else {
                            "Favoritos no disponibles"
                        },
                        description = if (favorites.isEmpty()) {
                            "En TV en directo, mantén OK sobre un canal para guardarlo aquí."
                        } else {
                            "Los canales guardados no están en la lista activa. " +
                                "Importa la lista de nuevo o guarda otros canales."
                        },
                        actionText = "Explorar TV en directo",
                        onAction = onExploreLive,
                        actionModifier = Modifier.focusRequester(actionFocusRequester),
                    )
                }
            }

            else -> {
                val firstCardFocusRequester = remember { FocusRequester() }
                LaunchedEffect(
                    favoriteChannels.size,
                    favoriteChannels.first().id,
                ) {
                    repeat(10) {
                        delay(80)
                        if (runCatching { firstCardFocusRequester.requestFocus() }.isSuccess) {
                            return@LaunchedEffect
                        }
                    }
                }
                ChannelGrid(
                    channels = favoriteChannels,
                    onChannelClick = { channel ->
                        onPlay(channel.toPlaybackRequest(favoriteChannels))
                    },
                    favoriteIds = favorites,
                    onChannelLongClick = { channel ->
                        favoritesRepo.toggle(channel.id)
                        onToast("Quitado de favoritos")
                    },
                    firstItemFocusRequester = firstCardFocusRequester,
                    modifier = Modifier.weight(1f),
                )
                if (unavailableCount > 0) {
                    Text(
                        text = if (unavailableCount == 1) {
                            "1 canal guardado no está en la lista actual."
                        } else {
                            "$unavailableCount canales guardados no están en la lista actual."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
                    )
                }
                Text(
                    text = "OK: ver · Mantén OK para quitar de favoritos",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 48.dp, bottom = 14.dp),
                )
            }
        }
    }
}
