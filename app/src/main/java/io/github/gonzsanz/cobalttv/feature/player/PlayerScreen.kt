package io.github.gonzsanz.cobalttv.feature.player

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButton
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltButtonStyle
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSoft
import io.github.gonzsanz.cobalttv.core.designsystem.ErrorRed
import io.github.gonzsanz.cobalttv.core.designsystem.Graphite
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.core.playback.PlaybackSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "CobaltPlayer"
private const val SEEK_STEP_MS = 10_000L

@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val request = remember { PlaybackSession.current }
    if (request == null) {
        MissingPlayback(onBack = onBack)
    } else {
        PlayerContent(request = request, onBack = onBack)
    }
}

@Composable
private fun PlayerContent(request: PlaybackRequest, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(request.url) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            setHandleAudioBecomingNoisy(true)
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<PlaybackException?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var retryTick by remember { mutableIntStateOf(0) }

    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    val togglePlayPause: () -> Unit = {
        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0L)
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        interactionTick++
    }

    val seekBy: (Long) -> Unit = { delta ->
        if (!request.isLive) {
            val duration = exoPlayer.duration
            val target = (exoPlayer.currentPosition + delta).coerceAtLeast(0L)
            val bounded = if (duration > 0 && duration != C.TIME_UNSET) {
                target.coerceAtMost(duration)
            } else {
                target
            }
            exoPlayer.seekTo(bounded)
            interactionTick++
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_ENDED) controlsVisible = true
            }

            override fun onPlayerError(playerError: PlaybackException) {
                Log.e(TAG, "No se pudo reproducir ${request.url}", playerError)
                error = playerError
                controlsVisible = true
            }
        }
        exoPlayer.addListener(listener)

        var resumeOnStart = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeOnStart = exoPlayer.playWhenReady
                    exoPlayer.pause()
                }

                Lifecycle.Event.ON_START -> {
                    if (resumeOnStart) exoPlayer.play()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(request.url, retryTick) {
        error = null
        controlsVisible = true
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(request.url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // Keep focus on the controls while they are visible, and on the root while
    // they are hidden so BACK and D-pad presses are still delivered to the player.
    LaunchedEffect(controlsVisible, error) {
        if (error != null) return@LaunchedEffect
        if (controlsVisible) {
            repeat(5) {
                delay(80)
                if (runCatching { playPauseFocusRequester.requestFocus() }.isSuccess) {
                    return@LaunchedEffect
                }
            }
        } else {
            delay(30)
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            delay(120)
            runCatching { retryFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            repeat(5) {
                delay(80)
                if (runCatching { playPauseFocusRequester.requestFocus() }.isSuccess) {
                    return@LaunchedEffect
                }
            }
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, interactionTick, error) {
        if (controlsVisible && isPlaying && error == null) {
            delay(5_000)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusProperties { canFocus = !controlsVisible && error == null }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        if (!controlsVisible) {
                            controlsVisible = true
                            togglePlayPause()
                            true
                        } else {
                            interactionTick++
                            false
                        }
                    }

                    Key.MediaPlayPause -> {
                        controlsVisible = true
                        togglePlayPause()
                        true
                    }

                    Key.MediaPlay -> {
                        controlsVisible = true
                        exoPlayer.play()
                        interactionTick++
                        true
                    }

                    Key.MediaPause -> {
                        controlsVisible = true
                        exoPlayer.pause()
                        interactionTick++
                        true
                    }

                    Key.MediaFastForward -> {
                        controlsVisible = true
                        seekBy(SEEK_STEP_MS)
                        true
                    }

                    Key.MediaRewind -> {
                        controlsVisible = true
                        seekBy(-SEEK_STEP_MS)
                        true
                    }

                    Key.DirectionUp,
                    Key.DirectionDown,
                    Key.DirectionLeft,
                    Key.DirectionRight,
                    -> {
                        if (!controlsVisible) controlsVisible = true
                        interactionTick++
                        false
                    }

                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                    keepScreenOn = true
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            },
            update = { view -> view.player = exoPlayer },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        when {
            error != null -> ErrorPanel(
                message = friendlyMessage(error!!),
                onRetry = {
                    retryTick++
                    interactionTick++
                },
                onBack = onBack,
                retryFocusRequester = retryFocusRequester,
            )

            else -> {
                if (playbackState == Player.STATE_IDLE ||
                    playbackState == Player.STATE_BUFFERING
                ) {
                    BufferingOverlay()
                }

                if (controlsVisible) {
                    PlayerTopBar(
                        request = request,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    PlayerControls(
                        request = request,
                        isPlaying = isPlaying,
                        isEnded = playbackState == Player.STATE_ENDED,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onTogglePlayPause = togglePlayPause,
                        onSeek = seekBy,
                        playPauseFocusRequester = playPauseFocusRequester,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(request: PlaybackRequest, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent),
                ),
            )
            .padding(horizontal = 48.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = request.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = request.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (request.isLive) {
            LiveBadge()
            Spacer(Modifier.width(18.dp))
        }
        Text(
            text = "Atrás para salir",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ErrorRed.copy(alpha = 0.18f))
            .border(1.dp, ErrorRed.copy(alpha = 0.65f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(ErrorRed),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "EN DIRECTO",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun PlayerControls(
    request: PlaybackRequest,
    isPlaying: Boolean,
    isEnded: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    playPauseFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                ),
            )
            .padding(start = 48.dp, end = 48.dp, top = 48.dp, bottom = 30.dp),
    ) {
        if (request.isLive) {
            Text(
                text = "Señal en directo",
                style = MaterialTheme.typography.labelMedium,
                color = CobaltSoft,
            )
            Spacer(Modifier.height(16.dp))
        } else {
            val progress = if (durationMs > 0) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(CobaltSoft),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = formatTime(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!request.isLive) {
                SeekButton(
                    text = "− 10 s",
                    onClick = { onSeek(-SEEK_STEP_MS) },
                )
                Spacer(Modifier.width(16.dp))
            }

            PlayPauseButton(
                isPlaying = isPlaying,
                onClick = onTogglePlayPause,
                focusRequester = playPauseFocusRequester,
            )

            if (!request.isLive) {
                Spacer(Modifier.width(16.dp))
                SeekButton(
                    text = "+ 10 s",
                    onClick = { onSeek(SEEK_STEP_MS) },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (isEnded) {
                        "Fin de la reproducción · Pulsa reproducir para verla de nuevo"
                    } else {
                        "Usa el mando para controlar la reproducción"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (isEnded) {
                        "Fin de la reproducción · Pulsa reproducir para verla de nuevo"
                    } else {
                        "La señal en directo no se puede pausar ni adelantar"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(64.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.16f))
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.28f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) CobaltIcons.Pause else CobaltIcons.Play,
            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
            tint = if (focused) Graphite else Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun SeekButton(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.12f))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.24f),
                shape = RoundedCornerShape(50),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (focused) Graphite else Color.White,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun BufferingOverlay() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BufferingSpinner()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Conectando con la señal…",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun BufferingSpinner() {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing),
            )
        }
    }
    Canvas(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { rotationZ = rotation.value },
    ) {
        drawArc(
            color = CobaltSoft,
            startAngle = -90f,
            sweepAngle = 265f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ErrorPanel(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    retryFocusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.84f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ErrorRed.copy(alpha = 0.16f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.displaySmall,
                    color = ErrorRed,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "No se pudo reproducir",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CobaltButton(
                    text = "Reintentar",
                    onClick = onRetry,
                    modifier = Modifier.focusRequester(retryFocusRequester),
                )
                CobaltButton(
                    text = "Volver",
                    onClick = onBack,
                    style = CobaltButtonStyle.Secondary,
                )
            }
        }
    }
}

@Composable
private fun MissingPlayback(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No hay ningún contenido seleccionado",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(24.dp))
            CobaltButton(text = "Volver", onClick = onBack)
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0L || milliseconds == C.TIME_UNSET) return "00:00"
    val totalSeconds = milliseconds / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun friendlyMessage(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    -> "No se pudo conectar con el servidor del stream. Comprueba la conexión e inténtalo de nuevo."

    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "El servidor rechazó la solicitud. Puede que el enlace haya caducado o requiera autenticación."

    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "El enlace no existe o ya no está disponible."

    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    -> "Este dispositivo no puede decodificar el formato del stream."

    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    -> "El enlace no es una lista de reproducción válida."

    else -> error.localizedMessage ?: "Error de reproducción desconocido."
}
