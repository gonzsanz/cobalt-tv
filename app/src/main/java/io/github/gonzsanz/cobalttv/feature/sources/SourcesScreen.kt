package io.github.gonzsanz.cobalttv.feature.sources

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButton
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButtonStyle
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSoft
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSpinner
import io.github.gonzsanz.cobalttv.core.designsystem.ErrorRed
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader
import io.github.gonzsanz.cobalttv.core.designsystem.SuccessGreen
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.designsystem.WarningAmber
import io.github.gonzsanz.cobalttv.data.catalog.PlaylistSource
import io.github.gonzsanz.cobalttv.data.catalog.SourceKind
import io.github.gonzsanz.cobalttv.ui.LocalCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun SourcesScreen(onOpenLive: () -> Unit) {
    val catalog = LocalCatalog.current
    val state by catalog.state.collectAsState()
    val source = state.source

    var url by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pickerError by remember { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    val urlFocusRequester = remember { FocusRequester() }
    val importFocusRequester = remember { FocusRequester() }
    val fileFocusRequester = remember { FocusRequester() }
    val watchFocusRequester = remember { FocusRequester() }
    val cancelDeleteFocusRequester = remember { FocusRequester() }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pickerError = null
            catalog.importFromUri(uri)
        }
    }

    val importUrl: () -> Unit = {
        keyboard?.hide()
        if (url.isNotBlank()) catalog.importFromUrl(url)
    }

    LaunchedEffect(state.isLoading, source != null) {
        if (state.isLoading) return@LaunchedEffect
        delay(120)
        val target = if (source == null) urlFocusRequester else watchFocusRequester
        repeat(6) {
            delay(80)
            if (runCatching { target.requestFocus() }.isSuccess) {
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(80)
            runCatching { cancelDeleteFocusRequester.requestFocus() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Fuentes",
            subtitle = if (source != null) {
                "Una lista activa. Puedes actualizarla o sustituirla cuando quieras."
            } else {
                "Configura de dónde viene tu contenido. Cada fuente es independiente."
            },
            showDemoBadge = false,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 48.dp,
                end = 48.dp,
                top = 8.dp,
                bottom = 26.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (source != null) {
                item {
                    ActiveSourceCard(
                        source = source,
                        isLoading = state.isLoading,
                        confirmDelete = confirmDelete,
                        watchFocusRequester = watchFocusRequester,
                        cancelDeleteFocusRequester = cancelDeleteFocusRequester,
                        onWatch = onOpenLive,
                        onRefresh = catalog::refresh,
                        onAskDelete = { confirmDelete = true },
                        onCancelDelete = { confirmDelete = false },
                        onDelete = {
                            confirmDelete = false
                            catalog.removeSource()
                        },
                    )
                }
            }

            item {
                ImportCard(
                    url = url,
                    onUrlChange = {
                        url = it
                        catalog.clearImportError()
                    },
                    onImport = importUrl,
                    onPickFile = {
                        pickerError = null
                        try {
                            filePicker.launch(arrayOf("*/*"))
                        } catch (error: ActivityNotFoundException) {
                            pickerError = "Este dispositivo no tiene un selector de archivos. " +
                                "Importa la lista por URL."
                        }
                    },
                    isLoading = state.isLoading,
                    hasSource = source != null,
                    importError = state.importError,
                    pickerError = pickerError,
                    urlFocusRequester = urlFocusRequester,
                    importFocusRequester = importFocusRequester,
                    fileFocusRequester = fileFocusRequester,
                )
            }

            item {
                InfoRow(
                    title = "Xtream Codes",
                    description = "Servidor, usuario y contraseña. Llega en la Fase 5.",
                    badge = "FASE 5",
                    badgeColor = WarningAmber,
                )
            }

            item {
                Text(
                    text = "La lista se guarda solo en este dispositivo y no se envía a " +
                        "ningún servidor propio. Usa únicamente fuentes que tengas derecho " +
                        "a reproducir.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveSourceCard(
    source: PlaylistSource,
    isLoading: Boolean,
    confirmDelete: Boolean,
    watchFocusRequester: FocusRequester,
    cancelDeleteFocusRequester: FocusRequester,
    onWatch: () -> Unit,
    onRefresh: () -> Unit,
    onAskDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CobaltSoft.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(1.dp, CobaltSoft.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(CobaltSoft.copy(alpha = 0.16f))
                    .border(1.dp, CobaltSoft.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CobaltIcons.Sources,
                    contentDescription = null,
                    tint = CobaltSoft,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${source.kind.label()} · ${source.channelCount} canales · " +
                        "Importada el ${formatDate(source.importedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (source.kind == SourceKind.File) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = source.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isLoading) {
                CobaltSpinner(size = 20.dp, strokeWidth = 3.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Actualizando…",
                    style = MaterialTheme.typography.labelSmall,
                    color = CobaltSoft,
                )
            } else if (!confirmDelete) {
                StatusBadge(text = "ACTIVA", color = SuccessGreen)
            }
        }

        Spacer(Modifier.height(18.dp))

        if (confirmDelete) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "¿Eliminar «${source.name}»? Se borrará la lista guardada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f),
                )
                CobaltButton(
                    text = "Sí, eliminar",
                    onClick = onDelete,
                    style = CobaltButtonStyle.Danger,
                )
                Spacer(Modifier.width(12.dp))
                CobaltButton(
                    text = "Cancelar",
                    onClick = onCancelDelete,
                    style = CobaltButtonStyle.Secondary,
                    modifier = Modifier.focusRequester(cancelDeleteFocusRequester),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CobaltButton(
                    text = "Ver TV en directo",
                    onClick = onWatch,
                    icon = CobaltIcons.Play,
                    modifier = Modifier.focusRequester(watchFocusRequester),
                )
                Spacer(Modifier.width(12.dp))
                if (source.kind == SourceKind.Url) {
                    CobaltButton(
                        text = "Actualizar",
                        onClick = onRefresh,
                        style = CobaltButtonStyle.Secondary,
                        enabled = !isLoading,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                CobaltButton(
                    text = "Eliminar",
                    onClick = onAskDelete,
                    style = CobaltButtonStyle.Secondary,
                    enabled = !isLoading,
                )
            }
        }
    }
}

@Composable
private fun ImportCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onImport: () -> Unit,
    onPickFile: () -> Unit,
    isLoading: Boolean,
    hasSource: Boolean,
    importError: String?,
    pickerError: String?,
    urlFocusRequester: FocusRequester,
    importFocusRequester: FocusRequester,
    fileFocusRequester: FocusRequester,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.border, RoundedCornerShape(20.dp))
            .padding(22.dp),
    ) {
        Text(
            text = if (hasSource) "Sustituir la lista" else "Añadir lista M3U",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pega la URL de una lista M3U o M3U8. También puedes elegir un " +
                "archivo guardado en el dispositivo.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "URL DE LA LISTA",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(8.dp))
        UrlField(
            value = url,
            onValueChange = onUrlChange,
            focusRequester = urlFocusRequester,
            enabled = !isLoading,
            onDone = onImport,
            onDpadDown = {
                keyboard?.hide()
                val target = if (!isLoading && url.isNotBlank()) {
                    importFocusRequester
                } else {
                    fileFocusRequester
                }
                runCatching { target.requestFocus() }
            },
        )
        if (importError != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = importError,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
            )
            if (hasSource) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "La lista actual sigue activa.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CobaltButton(
                text = if (isLoading) "Importando…" else "Importar lista",
                onClick = onImport,
                icon = Icons.Filled.Add,
                enabled = !isLoading && url.isNotBlank(),
                modifier = Modifier.focusRequester(importFocusRequester),
            )
            Spacer(Modifier.width(12.dp))
            CobaltButton(
                text = "Elegir archivo…",
                onClick = onPickFile,
                style = CobaltButtonStyle.Secondary,
                enabled = !isLoading,
                modifier = Modifier.focusRequester(fileFocusRequester),
            )
            if (isLoading) {
                Spacer(Modifier.width(16.dp))
                CobaltSpinner(size = 22.dp, strokeWidth = 3.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Leyendo la lista…",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = pickerError
                ?: "Si el selector de archivos no está disponible en tu televisor, usa la URL.",
            style = MaterialTheme.typography.labelSmall,
            color = if (pickerError != null) ErrorRed else TextMuted,
        )
    }
}

@Composable
private fun UrlField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean,
    onDone: () -> Unit,
    onDpadDown: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f), shape)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) CobaltSoft else MaterialTheme.colorScheme.border,
                shape = shape,
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "https://ejemplo.com/lista.m3u",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionDown
                        ) {
                            onDpadDown()
                            true
                        } else {
                            false
                        }
                    }
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) keyboard?.show()
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(CobaltSoft),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Uri,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboard?.hide()
                        if (value.isNotBlank()) onDone()
                    },
                ),
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun InfoRow(
    title: String,
    description: String,
    badge: String,
    badgeColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
        )
    }
}

private fun SourceKind.label(): String = when (this) {
    SourceKind.Url -> "M3U por URL"
    SourceKind.File -> "Archivo local"
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "fecha desconocida"
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
