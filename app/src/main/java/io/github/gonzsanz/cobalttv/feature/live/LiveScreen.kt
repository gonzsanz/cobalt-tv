package io.github.gonzsanz.cobalttv.feature.live

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import io.github.gonzsanz.cobalttv.core.playback.PlayerHolder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.Cobalt
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSoft
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSpinner
import io.github.gonzsanz.cobalttv.core.designsystem.EmptyState
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.data.epg.EpgProgramme
import io.github.gonzsanz.cobalttv.data.epg.EpgState
import io.github.gonzsanz.cobalttv.data.epg.EpgStatus
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import io.github.gonzsanz.cobalttv.ui.LocalEpg
import io.github.gonzsanz.cobalttv.ui.LocalFavorites
import io.github.gonzsanz.cobalttv.ui.LocalPlayer
import io.github.gonzsanz.cobalttv.ui.components.ChannelLogo
import io.github.gonzsanz.cobalttv.ui.model.ChannelUi
import io.github.gonzsanz.cobalttv.ui.model.channelCountLabel
import io.github.gonzsanz.cobalttv.ui.model.guideStatusLabel
import io.github.gonzsanz.cobalttv.ui.model.rememberCurrentTimeMs
import io.github.gonzsanz.cobalttv.ui.model.toPlaybackRequest
import io.github.gonzsanz.cobalttv.ui.model.toUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val ALL = "Todos"
private const val FAVORITES = "Favoritos"

@Composable
fun LiveScreen(
    onPlay: (PlaybackRequest) -> Unit,
    onToast: (String) -> Unit = {},
) {
    val catalog by LocalCatalog.current.state.collectAsState()
    val favoritesRepo = LocalFavorites.current
    val favorites by favoritesRepo.favorites.collectAsState()
    val epg by LocalEpg.current.state.collectAsState()
    val nowMs = rememberCurrentTimeMs()
    val hasRealChannels = catalog.channels.isNotEmpty()

    val allChannels: List<ChannelUi> = remember(hasRealChannels, catalog.channels) {
        if (hasRealChannels) {
            catalog.channels.map { it.toUi() }
        } else {
            DemoCatalog.channels.map { it.toUi() }
        }
    }

    val groups = remember(allChannels) {
        allChannels.asSequence()
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()
    }
    val categories = remember(groups) { listOf(ALL, FAVORITES) + groups }
    val counts = remember(allChannels, favorites) {
        buildMap {
            put(ALL, allChannels.size)
            put(FAVORITES, allChannels.count { it.id in favorites })
            allChannels.groupingBy { it.category }.eachCount().forEach { (k, v) -> put(k, v) }
        }
    }

    var selectedCategory by rememberSaveable { mutableStateOf(ALL) }
    LaunchedEffect(categories) {
        if (selectedCategory !in categories) selectedCategory = ALL
    }

    val channels = remember(allChannels, selectedCategory, favorites) {
        when (selectedCategory) {
            ALL -> allChannels
            FAVORITES -> allChannels.filter { it.id in favorites }
            else -> allChannels.filter { it.category == selectedCategory }
        }
    }

    val player = LocalPlayer.current
    var focusedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedCategory) { focusedId = null }
    val focusedChannel = channels.firstOrNull { it.id == focusedId } ?: channels.firstOrNull()
    val startIndex = remember(channels) {
        channels.indexOfFirst { it.id == player.currentKey }.coerceAtLeast(0)
    }

    val firstRowFocus = remember { FocusRequester() }
    val firstCategoryFocus = remember { FocusRequester() }
    LaunchedEffect(catalog.isLoading, hasRealChannels) {
        if (catalog.isLoading && !hasRealChannels) return@LaunchedEffect
        repeat(10) {
            delay(80)
            val ok = runCatching { firstRowFocus.requestFocus() }.isSuccess ||
                runCatching { firstCategoryFocus.requestFocus() }.isSuccess
            if (ok) return@LaunchedEffect
        }
    }

    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 18.dp)) {
        LiveHeader(
            count = allChannels.size,
            sourceName = catalog.source?.name,
            guide = if (hasRealChannels) epg.guideStatusLabel() else null,
            isDemo = !hasRealChannels,
        )
        Spacer(Modifier.height(12.dp))

        if (catalog.isLoading && !hasRealChannels) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CobaltSpinner(size = 44.dp)
            }
            return@Column
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryColumn(
                categories = categories,
                counts = counts,
                selected = selectedCategory,
                onSelect = { selectedCategory = it },
                firstFocus = firstCategoryFocus,
                modifier = Modifier.width(176.dp).fillMaxHeight(),
            )

            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (channels.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Favorite,
                        title = if (selectedCategory == FAVORITES) {
                            "Aún no tienes favoritos"
                        } else {
                            "No hay canales en esta categoría"
                        },
                        description = if (selectedCategory == FAVORITES) {
                            "Mantén pulsado OK sobre un canal para guardarlo aquí."
                        } else {
                            "Prueba con otra categoría."
                        },
                    )
                } else {
                    ChannelColumn(
                        key = selectedCategory,
                        startIndex = startIndex,
                        channels = channels,
                        favorites = favorites,
                        epg = epg,
                        nowMs = nowMs,
                        firstRowFocus = firstRowFocus,
                        onFocused = { focusedId = it },
                        playingId = player.currentKey,
                        onClick = { channel ->
                            // First OK plays in the preview; OK again on the same channel goes full screen.
                            if (channel.id == player.currentKey && player.error == null) {
                                onPlay(channel.toPlaybackRequest(channels))
                            } else {
                                player.play(channel.streamUrl, channel.id)
                            }
                        },
                        onLongClick = { channel ->
                            val added = favoritesRepo.toggle(channel.id)
                            onToast(if (added) "Añadido a favoritos" else "Quitado de favoritos")
                        },
                    )
                }
            }

            Column(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewBox(
                    holder = player,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                InfoPanel(
                    channel = focusedChannel,
                    number = focusedChannel?.let { channels.indexOf(it) + 1 } ?: 0,
                    favorite = focusedChannel?.id in favorites,
                    epg = epg,
                    nowMs = nowMs,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }

        Text(
            text = "OK: reproducir · OK de nuevo: pantalla completa · Mantén OK: favorito",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun LiveHeader(count: Int, sourceName: String?, guide: String?, isDemo: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "TV en directo",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = buildString {
                append(channelCountLabel(count))
                if (sourceName != null) append(" · $sourceName")
                if (guide != null) append(" · $guide")
                if (isDemo) append(" · Datos de demostración")
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryColumn(
    categories: List<String>,
    counts: Map<String, Int>,
    selected: String,
    onSelect: (String) -> Unit,
    firstFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        itemsIndexed(categories, key = { _, name -> name }) { index, name ->
            var focused by remember { mutableStateOf(false) }
            val isSelected = name == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            focused -> Color.White.copy(alpha = 0.14f)
                            isSelected -> Cobalt.copy(alpha = 0.22f)
                            else -> Color.Transparent
                        },
                    )
                    .border(
                        if (focused) 2.dp else 0.dp,
                        if (focused) Color.White else Color.Transparent,
                        RoundedCornerShape(10.dp),
                    )
                    .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .combinedClickable(onClick = { onSelect(name) })
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected || focused) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 500) else Modifier),
                )
                Text(
                    text = (counts[name] ?: 0).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelColumn(
    key: String,
    startIndex: Int,
    channels: List<ChannelUi>,
    favorites: Set<String>,
    playingId: String?,
    epg: EpgState,
    nowMs: Long,
    firstRowFocus: FocusRequester,
    onFocused: (String) -> Unit,
    onClick: (ChannelUi) -> Unit,
    onLongClick: (ChannelUi) -> Unit,
) {
    androidx.compose.runtime.key(key) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (startIndex - 2).coerceAtLeast(0),
    )
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            var focused by remember { mutableStateOf(false) }
            val programme = remember(epg, channel.id, nowMs) {
                if (!channel.isDemo && epg.status == EpgStatus.Ready) {
                    epg.nowNext(channel.epgId, nowMs)?.now
                } else {
                    null
                }
            }
            val shape = RoundedCornerShape(10.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(shape)
                    .background(if (focused) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .border(
                        if (focused) 2.dp else 0.dp,
                        if (focused) Color.White else Color.Transparent,
                        shape,
                    )
                    .then(if (index == startIndex) Modifier.focusRequester(firstRowFocus) else Modifier)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onFocused(channel.id)
                    }
                    .combinedClickable(
                        onClick = { onClick(channel) },
                        onLongClick = { onLongClick(channel) },
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.width(30.dp),
                )
                ChannelLogo(channel = channel, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = if (focused) {
                            Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 500)
                        } else {
                            Modifier
                        },
                    )
                    Text(
                        text = when {
                            programme != null -> programme.title
                            channel.isDemo -> channel.nowPlaying
                            epg.status == EpgStatus.Ready -> "Sin programación"
                            else -> "En directo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (programme != null) {
                        Spacer(Modifier.height(3.dp))
                        ProgressBar(programme, nowMs, Modifier.fillMaxWidth(0.7f))
                    }
                }
                if (channel.id == playingId) {
                    Icon(
                        imageVector = io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons.Play,
                        contentDescription = "Reproduciendo",
                        tint = Cobalt,
                        modifier = Modifier.size(18.dp).padding(end = 2.dp),
                    )
                }
                if (channel.id in favorites) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorito",
                        tint = CobaltSoft,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun InfoPanel(
    channel: ChannelUi?,
    number: Int,
    favorite: Boolean,
    epg: EpgState,
    nowMs: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(16.dp),
    ) {
        if (channel == null) return@Column
        val nowNext = if (!channel.isDemo && epg.status == EpgStatus.Ready) {
            epg.nowNext(channel.epgId, nowMs)
        } else {
            null
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel = channel, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Canal $number · ${channel.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        val now = nowNext?.now
        Text("AHORA", style = MaterialTheme.typography.labelSmall, color = CobaltSoft)
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                now != null -> now.title
                channel.isDemo -> channel.nowPlaying
                epg.status == EpgStatus.Ready -> "Sin programación"
                else -> "En directo"
            },
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (now != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = timeRange(now),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            ProgressBar(now, nowMs, Modifier.fillMaxWidth())
        }

        val next = nowNext?.next
        if (next != null) {
            Spacer(Modifier.height(18.dp))
            Text("DESPUÉS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(
                text = next.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeRange(next),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }

        Spacer(Modifier.weight(1f))
        if (favorite) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = CobaltSoft,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("En favoritos", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun ProgressBar(programme: EpgProgramme, nowMs: Long, modifier: Modifier = Modifier) {
    val total = (programme.stopMs - programme.startMs).coerceAtLeast(1L)
    val fraction = ((nowMs - programme.startMs).toFloat() / total).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Cobalt),
        )
    }
}

private fun timeRange(programme: EpgProgramme): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${format.format(Date(programme.startMs))} – ${format.format(Date(programme.stopMs))}"
}

@Composable
private fun PreviewBox(holder: PlayerHolder, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isFocusable = false
                    isFocusableInTouchMode = false
                    player = holder.player
                }
            },
            update = { view -> view.player = holder.player },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )
        when {
            holder.currentKey == null -> Text(
                text = "Vista previa",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
            holder.error != null -> Text(
                text = "Sin señal",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            holder.playbackState == Player.STATE_BUFFERING ||
                holder.playbackState == Player.STATE_IDLE -> CobaltSpinner(size = 28.dp)
        }
    }
}
