package io.github.gonzsanz.cobalttv.feature.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButton
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButtonStyle
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.EmptyState
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted

@Composable
fun SourcesScreen(onToast: (String) -> Unit) {
    val firstActionFocusRequester = remember { FocusRequester() }
    InitialFocusEffect(firstActionFocusRequester)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Fuentes",
            subtitle = "Configura de dónde viene tu contenido. Cada fuente es independiente.",
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = CobaltIcons.Sources,
                title = "Sin fuentes configuradas",
                description = "Añade una lista M3U por URL o una cuenta de Xtream Codes. " +
                    "También podrás importar un archivo M3U en una fase posterior.",
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            CobaltButton(
                text = "Añadir lista M3U",
                onClick = { onToast("La importación M3U llega en la Fase 3.") },
                icon = Icons.Filled.Add,
                modifier = Modifier.focusRequester(firstActionFocusRequester),
            )
            Spacer(Modifier.width(14.dp))
            CobaltButton(
                text = "Conectar Xtream Codes",
                onClick = { onToast("Xtream Codes llega en la Fase 5.") },
                style = CobaltButtonStyle.Secondary,
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Las credenciales se guardarán solo en este dispositivo. " +
                "No se envían a ningún servidor propio ni se incluyen en logs.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 8.dp),
        )
    }
}
