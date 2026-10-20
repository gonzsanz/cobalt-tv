package io.github.gonzsanz.cobalttv.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.Cobalt
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSoft
import io.github.gonzsanz.cobalttv.core.designsystem.EmptyState
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.data.demo.DemoCatalog
import io.github.gonzsanz.cobalttv.ui.components.SearchResultRow

@Composable
fun SearchScreen(onToast: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { DemoCatalog.search(query) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Buscar",
            subtitle = "Búsqueda sobre el catálogo de demostración.",
        )

        SearchField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.padding(horizontal = 48.dp),
        )

        Spacer(Modifier.height(12.dp))

        when {
            query.isBlank() -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "¿Qué quieres ver?",
                        description = "Escribe el nombre de un canal, película o serie.",
                    )
                }
            }

            results.isEmpty() -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "Sin resultados",
                        description = "No hay coincidencias para «$query». Prueba con otro nombre o categoría.",
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 4.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(results, key = { it.id }) { hit ->
                        SearchResultRow(
                            hit = hit,
                            onClick = { onToast("«${hit.title}» se abrirá en una fase posterior.") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    InitialFocusEffect(focusRequester)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) CobaltSoft else MaterialTheme.colorScheme.border,
                shape = shape,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = if (focused) CobaltSoft else TextMuted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Buscar canales, películas o series",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) {
                            keyboard?.show()
                        }
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(Cobalt),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
    }
}
