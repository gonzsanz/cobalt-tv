package io.github.gonzsanz.cobalttv.feature.series

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
import io.github.gonzsanz.cobalttv.data.catalog.SeriesItem
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import io.github.gonzsanz.cobalttv.ui.components.CategorySidebar
import io.github.gonzsanz.cobalttv.ui.components.VodPosterCard

private const val ALL = "Todas"

/** Hand-off of the selected series to the detail screen. */
object SeriesSession {
    var current: SeriesItem? = null
}

@Composable
fun SeriesScreen(onOpenSeries: (SeriesItem) -> Unit) {
    val catalog by LocalCatalog.current.state.collectAsState()
    val series = catalog.series

    if (series.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "Series",
                subtitle = if (catalog.isLoading) {
                    "Cargando tu fuente…"
                } else {
                    "Las series de tu cuenta aparecerán aquí."
                },
                showDemoBadge = false,
            )
            if (!catalog.isLoading) {
                EmptyState(
                    icon = CobaltIcons.Series,
                    title = "Sin series",
                    description = "La fuente activa no incluye series. " +
                        "Añade una cuenta Xtream Codes en Ajustes → Fuentes.",
                )
            }
        }
        return
    }

    val categories = remember(series) {
        listOf(ALL) + series.map { it.group }.filter { it.isNotBlank() }.distinct()
            .sortedBy { it.lowercase() }
    }
    val counts = remember(series) {
        buildMap {
            put(ALL, series.size)
            series.groupingBy { it.group }.eachCount().forEach { (k, v) -> put(k, v) }
        }
    }
    var selected by rememberSaveable { mutableStateOf(ALL) }
    LaunchedEffect(categories) { if (selected !in categories) selected = ALL }
    val visible = remember(series, selected) {
        if (selected == ALL) series else series.filter { it.group == selected }
    }
    val firstPoster = remember { FocusRequester() }
    InitialFocusEffect(firstPoster)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Series",
            subtitle = "${series.size} series de «${catalog.source?.name}»",
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
                itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                    VodPosterCard(
                        title = item.name,
                        subtitle = listOfNotNull(item.year, item.rating?.takeIf { it != "0" })
                            .joinToString(" · "),
                        imageUrl = item.coverUrl,
                        onClick = { onOpenSeries(item) },
                        modifier = if (index == 0) Modifier.focusRequester(firstPoster) else Modifier,
                    )
                }
            }
            }
    }
}
