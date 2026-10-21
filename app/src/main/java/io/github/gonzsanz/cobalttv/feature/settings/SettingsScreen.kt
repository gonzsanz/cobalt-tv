package io.github.gonzsanz.cobalttv.feature.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.BuildConfig
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.FocusableSurface
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.ui.LocalCatalog

@Composable
fun SettingsScreen(
    onOpenSources: () -> Unit,
    onToast: (String) -> Unit,
) {
    val catalog by LocalCatalog.current.state.collectAsState()
    val source = catalog.source
    val firstRowFocusRequester = remember { FocusRequester() }
    InitialFocusEffect(firstRowFocusRequester)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Ajustes",
            subtitle = "Fuentes, reproducción y datos de la aplicación.",
            showDemoBadge = false,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 6.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SettingsSectionLabel("CONTENIDO") }
            item {
                SettingsRow(
                    title = "Fuentes IPTV",
                    subtitle = when {
                        catalog.isLoading && source == null -> "Cargando la lista guardada…"
                        source != null ->
                            "${source.name} · ${catalog.channels.size} canales"
                        else -> "Sin lista configurada · M3U por URL o archivo"
                    },
                    icon = CobaltIcons.Sources,
                    onClick = onOpenSources,
                    modifier = Modifier.focusRequester(firstRowFocusRequester),
                )
            }

            item { SettingsSectionLabel("REPRODUCCIÓN") }
            item {
                SettingsRow(
                    title = "Reproductor",
                    subtitle = "Media3 / ExoPlayer · HLS y MP4",
                    icon = CobaltIcons.Play,
                    onClick = { onToast("Reproducción con Media3 (HLS y MP4).") },
                )
            }

            item { SettingsSectionLabel("APLICACIÓN") }
            item {
                SettingsRow(
                    title = "Acerca de Cobalt TV",
                    subtitle = "Versión ${BuildConfig.VERSION_NAME} · Fase 3",
                    icon = Icons.Filled.Info,
                    onClick = { onToast("Cobalt TV ${BuildConfig.VERSION_NAME}, uso personal.") },
                )
            }
            item {
                Text(
                    text = "Cobalt TV no incluye listas de canales ni credenciales. " +
                        "Usa únicamente fuentes que tengas derecho a reproducir.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = TextMuted,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(16.dp),
        focusedScale = 1.01f,
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = if (focused) TextPrimary else TextMuted,
            )
        }
    }
}
