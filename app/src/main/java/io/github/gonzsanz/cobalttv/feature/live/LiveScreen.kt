package io.github.gonzsanz.cobalttv.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.gonzsanz.cobalttv.core.designsystem.CategoryChip
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.ui.components.ChannelGrid

@Composable
fun LiveScreen(onToast: (String) -> Unit) {
    var selectedCategory by rememberSaveable { mutableStateOf("Todos") }
    val channels = remember(selectedCategory) { DemoCatalog.channelsByCategory(selectedCategory) }
    val firstChipFocusRequester = remember { FocusRequester() }
    InitialFocusEffect(firstChipFocusRequester)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "TV en directo",
            subtitle = "Elige una categoría y un canal. El reproductor Media3 llega en la Fase 2.",
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(DemoCatalog.categories, key = { _, category -> category }) { index, category ->
                CategoryChip(
                    label = category,
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                    modifier = if (index == 0) Modifier.focusRequester(firstChipFocusRequester) else Modifier,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        ChannelGrid(
            channels = channels,
            onChannelClick = { channel ->
                onToast("«${channel.name}» se reproducirá en la Fase 2 (Media3).")
            },
            modifier = Modifier.weight(1f),
        )
    }
}
