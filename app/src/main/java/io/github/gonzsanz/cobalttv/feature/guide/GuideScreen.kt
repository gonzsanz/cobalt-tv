package io.github.gonzsanz.cobalttv.feature.guide

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.CategoryChip
import io.github.gonzsanz.cobalttv.core.designsystem.Cobalt
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSoft
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.data.epg.EpgProgramme
import io.github.gonzsanz.cobalttv.data.epg.EpgStatus
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import io.github.gonzsanz.cobalttv.ui.LocalEpg
import io.github.gonzsanz.cobalttv.ui.LocalFavorites
import io.github.gonzsanz.cobalttv.ui.components.ChannelLogo
import io.github.gonzsanz.cobalttv.ui.model.ChannelUi
import io.github.gonzsanz.cobalttv.ui.model.rememberCurrentTimeMs
import io.github.gonzsanz.cobalttv.ui.model.toPlaybackRequest
import io.github.gonzsanz.cobalttv.ui.model.toUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val DpPerMinute = 4.dp
private const val SPAN_MINUTES = 12 * 60
private const val SLOT_MINUTES = 30
private val ChannelColumnWidth = 176.dp
private val RowHeight = 52.dp
private const val ALL = "Todos"
private const val FAVORITES = "Favoritos"

private data class Cell(
    val programme: EpgProgramme?,
    val startMs: Long,
    val stopMs: Long,
)

private data class Selection(val channel: ChannelUi, val programme: EpgProgramme?)

@Composable
fun GuideScreen(onPlay: (PlaybackRequest) -> Unit, onToast: (String) -> Unit = {}) {
    val catalog by LocalCatalog.current.state.collectAsState()
    val favoritesRepo = LocalFavorites.current
    val favorites by favoritesRepo.favorites.collectAsState()
    val epg by LocalEpg.current.state.collectAsState()
    val nowMs = rememberCurrentTimeMs()
    val hasRealChannels = catalog.channels.isNotEmpty()

    val allChannels = remember(hasRealChannels, catalog.channels) {
        if (hasRealChannels) {
            catalog.channels.map { it.toUi() }
        } else {
            DemoCatalog.channels.map { it.toUi() }
        }
    }
    val groups = remember(allChannels) {
        allChannels.map { it.category }.filter { it.isNotBlank() }.distinct()
            .sortedBy { it.lowercase() }
    }
    val categories = remember(groups) { listOf(ALL, FAVORITES) + groups }
    var selectedCategory by rememberSaveable { mutableStateOf(ALL) }
    LaunchedEffect(categories) { if (selectedCategory !in categories) selectedCategory = ALL }

    val channels = remember(allChannels, selectedCategory, favorites) {
        when (selectedCategory) {
            ALL -> allChannels
            FAVORITES -> allChannels.filter { it.id in favorites }
            else -> allChannels.filter { it.category == selectedCategory }
        }
    }

    val slotMs = SLOT_MINUTES * 60_000L
    val startMs = (nowMs / slotMs) * slotMs
    val endMs = startMs + SPAN_MINUTES * 60_000L
    val hScroll = rememberScrollState()
    var selection by remember { mutableStateOf<Selection?>(null) }
    val firstCellFocus = remember { FocusRequester() }

    LaunchedEffect(channels.isNotEmpty(), epg.status) {
        if (channels.isEmpty() || epg.status != EpgStatus.Ready) return@LaunchedEffect
        repeat(10) {
            delay(80)
            if (runCatching { firstCellFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }

    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 18.dp)) {
        Text(
            text = "Guía de TV",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(categories, key = { _, name -> name }) { _, name ->
                CategoryChip(
                    label = if (name.length > 26) name.take(25) + "…" else name,
                    selected = name == selectedCategory,
                    onClick = { selectedCategory = name },
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (hasRealChannels && epg.status != EpgStatus.Ready) {
            NoGuide(loading = epg.status == EpgStatus.Loading)
            return@Column
        }

        SelectionInfo(selection = selection, nowMs = nowMs)
        Spacer(Modifier.height(8.dp))

        TimeHeader(startMs = startMs, hScroll = hScroll)

        if (channels.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedCategory == FAVORITES) {
                        "Aún no tienes favoritos en esta guía."
                    } else {
                        "No hay canales en esta categoría."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                val cells = remember(epg, channel.id, startMs) {
                    buildCells(
                        schedule = if (channel.isDemo) null else epg.programmes[channel.epgId],
                        startMs = startMs,
                        endMs = endMs,
                    )
                }
                GuideRow(
                    channel = channel,
                    number = index + 1,
                    cells = cells,
                    nowMs = nowMs,
                    hScroll = hScroll,
                    firstCellFocus = if (index == 0) firstCellFocus else null,
                    onFocused = { selection = Selection(channel, it) },
                    onClick = { onPlay(channel.toPlaybackRequest(channels)) },
                    onLongClick = {
                        val added = favoritesRepo.toggle(channel.id)
                        onToast(if (added) "Añadido a favoritos" else "Quitado de favoritos")
                    },
                )
            }
        }
    }
}

private fun buildCells(schedule: List<EpgProgramme>?, startMs: Long, endMs: Long): List<Cell> {
    if (schedule.isNullOrEmpty()) return listOf(Cell(null, startMs, endMs))
    val cells = ArrayList<Cell>()
    var cursor = startMs
    for (programme in schedule) {
        if (programme.stopMs <= cursor) continue
        if (programme.startMs >= endMs) break
        if (programme.startMs > cursor) cells += Cell(null, cursor, programme.startMs)
        val from = maxOf(programme.startMs, cursor)
        val to = minOf(programme.stopMs, endMs)
        cells += Cell(programme, from, to)
        cursor = to
    }
    if (cursor < endMs) cells += Cell(null, cursor, endMs)
    return cells
}

private fun widthFor(fromMs: Long, toMs: Long): Dp =
    DpPerMinute * ((toMs - fromMs) / 60_000f)

@Composable
private fun TimeHeader(startMs: Long, hScroll: androidx.compose.foundation.ScrollState) {
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(Modifier.fillMaxWidth().height(26.dp)) {
        Spacer(Modifier.width(ChannelColumnWidth + 8.dp))
        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(0.dp))) {
            Row(Modifier.horizontalScroll(hScroll)) {
                repeat(SPAN_MINUTES / SLOT_MINUTES) { slot ->
                    Box(
                        modifier = Modifier.width(DpPerMinute * SLOT_MINUTES.toFloat()),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = format.format(Date(startMs + slot * SLOT_MINUTES * 60_000L)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GuideRow(
    channel: ChannelUi,
    number: Int,
    cells: List<Cell>,
    nowMs: Long,
    hScroll: androidx.compose.foundation.ScrollState,
    firstCellFocus: FocusRequester?,
    onFocused: (EpgProgramme?) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .width(ChannelColumnWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.width(26.dp),
            )
            ChannelLogo(channel = channel, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(0.dp))) {
            Row(Modifier.horizontalScroll(hScroll).fillMaxHeight()) {
                var firstFocusable = true
                cells.forEach { cell ->
                    val requester = if (firstFocusable && firstCellFocus != null && cell.programme != null) {
                        firstFocusable = false
                        firstCellFocus
                    } else {
                        null
                    }
                    ProgrammeCell(
                        cell = cell,
                        nowMs = nowMs,
                        modifier = Modifier,
                        focusRequester = requester,
                        onFocused = { onFocused(cell.programme) },
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProgrammeCell(
    cell: Cell,
    nowMs: Long,
    modifier: Modifier,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val isNow = cell.programme != null && cell.startMs <= nowMs && cell.stopMs > nowMs
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Box(
        modifier = modifier
            .width(widthFor(cell.startMs, cell.stopMs))
            .fillMaxHeight()
            .padding(end = 3.dp)
            .clip(shape)
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.18f)
                    isNow -> Cobalt.copy(alpha = 0.22f)
                    cell.programme == null -> Color.White.copy(alpha = 0.03f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                },
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color.White else Color.White.copy(alpha = 0.06f),
                shape,
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(
                text = cell.programme?.title ?: "Sin programación",
                style = MaterialTheme.typography.labelMedium,
                color = if (cell.programme == null) TextMuted else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (cell.programme != null) {
                Text(
                    text = "${format.format(Date(cell.programme.startMs))} – " +
                        format.format(Date(cell.programme.stopMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SelectionInfo(selection: Selection?, nowMs: Long) {
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxWidth().height(52.dp)) {
        if (selection == null) {
            Text(
                text = "Elige un programa · OK para ver el canal · Mantén OK: favorito",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            return@Column
        }
        val programme = selection.programme
        Text(
            text = programme?.title ?: "Sin programación",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = buildString {
                append(selection.channel.name)
                if (programme != null) {
                    append(" · ${format.format(Date(programme.startMs))} – ")
                    append(format.format(Date(programme.stopMs)))
                    if (programme.startMs <= nowMs && programme.stopMs > nowMs) append(" · En emisión")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = CobaltSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoGuide(loading: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (loading) "Cargando la guía…" else "No hay guía EPG disponible",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (loading) {
                    "Puede tardar unos segundos con listas grandes."
                } else {
                    "La fuente activa no declara una guía XMLTV o no se pudo descargar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
