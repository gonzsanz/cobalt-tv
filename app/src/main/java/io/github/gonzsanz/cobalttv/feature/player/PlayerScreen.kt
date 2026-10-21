package io.github.gonzsanz.cobalttv.feature.player

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import io.github.gonzsanz.cobalttv.core.playback.LiveChannel
import io.github.gonzsanz.cobalttv.ui.LocalPlayer
import io.github.gonzsanz.cobalttv.ui.components.ChannelLogo
import io.github.gonzsanz.cobalttv.ui.model.ChannelUi
import io.github.gonzsanz.cobalttv.ui.model.accentFor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.KeyEvent as AndroidKeyEvent
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
import androidx.compose.runtime.collectAsState
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
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltSpinner
import io.github.gonzsanz.cobalttv.core.designsystem.ErrorRed
import io.github.gonzsanz.cobalttv.core.designsystem.Graphite
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.core.playback.PlaybackSession
import io.github.gonzsanz.cobalttv.data.epg.EpgStatus
import io.github.gonzsanz.cobalttv.ui.LocalEpg
import io.github.gonzsanz.cobalttv.ui.model.rememberCurrentTimeMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    val holder = LocalPlayer.current
    val exoPlayer = holder.player

    var currentRequest by remember(request) { mutableStateOf(request) }
    val isLive = currentRequest.isLive
    val isPlaying = holder.isPlaying
    val playbackState = holder.playbackState
    val error = holder.error
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var listVisible by remember { mutableStateOf(false) }
    var digits by remember { mutableStateOf("") }
    var previousChannelId by remember { mutableStateOf<String?>(null) }

    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    val togglePlayPause: () -> Unit = {
        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0L)
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        interactionTick++
    }

    val seekBy: (Long) -> Unit = { delta ->
        if (!currentRequest.isLive) {
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

    val canZap = isLive && currentRequest.liveChannels.size > 1
    val zapTo: (Int) -> Boolean = { index ->
        val channels = currentRequest.liveChannels
        val next = channels.getOrNull(index)
        if (next == null || next.id == currentRequest.liveChannelId) {
            false
        } else {
            previousChannelId = currentRequest.liveChannelId
            val nextRequest = currentRequest.copy(
                title = next.title,
                subtitle = next.subtitle,
                url = next.url,
                liveChannelId = next.id,
                epgId = next.epgId,
            )
            currentRequest = nextRequest
            PlaybackSession.current = nextRequest
            controlsVisible = true
            interactionTick++
            true
        }
    }
    val zapBy: (Int) -> Boolean = { direction ->
        if (!canZap) {
            false
        } else {
            val channels = currentRequest.liveChannels
            val currentIndex = channels.indexOfFirst { it.id == currentRequest.liveChannelId }
            val nextIndex = if (currentIndex == -1) {
                0
            } else {
                (currentIndex + direction + channels.size) % channels.size
            }
            zapTo(nextIndex)
        }
    }

    BackHandler {
        if (listVisible) listVisible = false else onBack()
    }

    // Starts the stream unless the preview already loaded this very channel.
    LaunchedEffect(currentRequest.url, currentRequest.liveChannelId) {
        controlsVisible = true
        holder.play(currentRequest.url, currentRequest.liveChannelId ?: currentRequest.url)
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    LaunchedEffect(error) {
        if (error != null) controlsVisible = true
    }
    LaunchedEffect(playbackState) {
        if (playbackState == Player.STATE_ENDED) controlsVisible = true
    }

    // Live keeps focus on the root (the banner has nothing to focus); VOD
    // moves it to the controls while they are visible.
    LaunchedEffect(controlsVisible, error, listVisible, isLive) {
        if (error != null || listVisible) return@LaunchedEffect
        if (isLive || !controlsVisible) {
            delay(30)
            runCatching { rootFocusRequester.requestFocus() }
        } else {
            repeat(5) {
                delay(80)
                if (runCatching { playPauseFocusRequester.requestFocus() }.isSuccess) {
                    return@LaunchedEffect
                }
            }
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            delay(120)
            runCatching { retryFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(playbackState) {
        if (playbackState == Player.STATE_ENDED && !isLive) {
            repeat(5) {
                delay(80)
                if (runCatching { playPauseFocusRequester.requestFocus() }.isSuccess) {
                    return@LaunchedEffect
                }
            }
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, interactionTick, error, listVisible) {
        if (!controlsVisible || error != null || listVisible) return@LaunchedEffect
        if (isLive) {
            delay(4_000)
            controlsVisible = false
        } else if (isPlaying) {
            delay(5_000)
            controlsVisible = false
        }
    }

    // Typing a channel number: jump after a short pause without more digits.
    LaunchedEffect(digits) {
        if (digits.isEmpty()) return@LaunchedEffect
        delay(1_500)
        val number = digits.toIntOrNull()
        digits = ""
        if (number != null) zapTo(number - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusProperties { canFocus = error == null && !listVisible && (isLive || !controlsVisible) }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (listVisible) return@onPreviewKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val nativeKeyCode = event.nativeKeyEvent.keyCode
                val isRepeat = event.nativeKeyEvent.repeatCount > 0
                if (nativeKeyCode == AndroidKeyEvent.KEYCODE_CHANNEL_UP) {
                    return@onPreviewKeyEvent if (isRepeat && canZap) true else zapBy(1)
                }
                if (nativeKeyCode == AndroidKeyEvent.KEYCODE_CHANNEL_DOWN) {
                    return@onPreviewKeyEvent if (isRepeat && canZap) true else zapBy(-1)
                }
                if (isLive) {
                    val digit = digitFor(nativeKeyCode)
                    if (digit != null) {
                        if (digits.length < 4) digits += digit
                        return@onPreviewKeyEvent true
                    }
                    when (nativeKeyCode) {
                        AndroidKeyEvent.KEYCODE_LAST_CHANNEL -> {
                            val index = currentRequest.liveChannels
                                .indexOfFirst { it.id == previousChannelId }
                            return@onPreviewKeyEvent if (index >= 0) zapTo(index) else true
                        }

                        AndroidKeyEvent.KEYCODE_INFO, AndroidKeyEvent.KEYCODE_MENU -> {
                            controlsVisible = true
                            interactionTick++
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                when (event.key) {
                    Key.DirectionUp -> {
                        if (isRepeat) {
                            canZap
                        } else if (zapBy(-1)) {
                            true
                        } else {
                            if (!controlsVisible) controlsVisible = true
                            interactionTick++
                            false
                        }
                    }

                    Key.DirectionDown -> {
                        if (isRepeat) {
                            canZap
                        } else if (zapBy(1)) {
                            true
                        } else {
                            if (!controlsVisible) controlsVisible = true
                            interactionTick++
                            false
                        }
                    }

                    Key.DirectionCenter, Key.Enter -> {
                        if (isLive) {
                            if (currentRequest.liveChannels.isNotEmpty()) listVisible = true
                            true
                        } else if (!controlsVisible) {
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

                    Key.DirectionLeft -> {
                        if (isLive && currentRequest.liveChannels.isNotEmpty()) {
                            listVisible = true
                            true
                        } else {
                            if (!controlsVisible) controlsVisible = true
                            interactionTick++
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (!controlsVisible) controlsVisible = true
                        interactionTick++
                        isLive
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
                message = friendlyMessage(error),
                onRetry = {
                    holder.reload()
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

                if (isLive) {
                    if (controlsVisible && !listVisible) {
                        LiveBanner(
                            request = currentRequest,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                    if (digits.isNotEmpty()) {
                        Text(
                            text = digits,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                        )
                    }
                    if (listVisible) {
                        ChannelListOverlay(
                            channels = currentRequest.liveChannels,
                            currentId = currentRequest.liveChannelId,
                            onSelect = { index ->
                                listVisible = false
                                zapTo(index)
                            },
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                } else if (controlsVisible) {
                    PlayerTopBar(
                        request = currentRequest,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    PlayerControls(
                        request = currentRequest,
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

private fun digitFor(keyCode: Int): Int? = when (keyCode) {
    in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 -> keyCode - AndroidKeyEvent.KEYCODE_0
    in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 ->
        keyCode - AndroidKeyEvent.KEYCODE_NUMPAD_0
    else -> null
}

private fun LiveChannel.toUi(): ChannelUi = ChannelUi(
    id = id,
    name = title,
    category = category,
    nowPlaying = "",
    accent = accentFor("$category|$title"),
    streamUrl = url,
    epgId = epgId,
    logoUrl = logoUrl,
)

@Composable
private fun LiveBanner(request: PlaybackRequest, modifier: Modifier = Modifier) {
    val epg by LocalEpg.current.state.collectAsState()
    val nowMs = rememberCurrentTimeMs(intervalMs = 15_000L)
    val index = request.liveChannels.indexOfFirst { it.id == request.liveChannelId }
    val channel = request.liveChannels.getOrNull(index)?.toUi() ?: ChannelUi(
        id = request.liveChannelId ?: request.url,
        name = request.title,
        category = "",
        nowPlaying = "",
        accent = accentFor(request.title),
        streamUrl = request.url,
        epgId = request.epgId,
    )
    val nowNext = if (epg.status == EpgStatus.Ready) epg.nowNext(request.epgId, nowMs) else null
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))),
            )
            .padding(start = 48.dp, end = 48.dp, top = 56.dp, bottom = 32.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        ChannelLogo(channel = channel, size = 64.dp)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index >= 0) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CobaltSoft,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            val now = nowNext?.now
            Text(
                text = when {
                    now != null -> now.title
                    epg.status == EpgStatus.Ready -> "Sin programación"
                    else -> "En directo"
                },
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (now != null) {
                Spacer(Modifier.height(6.dp))
                val total = (now.stopMs - now.startMs).coerceAtLeast(1L)
                val fraction = ((nowMs - now.startMs).toFloat() / total).coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormat.format(Date(now.startMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.22f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(CobaltSoft),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = timeFormat.format(Date(now.stopMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
            nowNext?.next?.let { next ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Después · ${timeFormat.format(Date(next.startMs))}  ${next.title}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeFormat.format(Date(nowMs)),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "↑↓ canal · OK lista · 0-9 número",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun ChannelListOverlay(
    channels: List<LiveChannel>,
    currentId: String?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val epg by LocalEpg.current.state.collectAsState()
    val nowMs = rememberCurrentTimeMs()
    val currentIndex = remember(channels, currentId) {
        channels.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 3).coerceAtLeast(0),
    )
    val currentFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(8) {
            delay(60)
            if (runCatching { currentFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Column(
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.78f)),
                ),
            )
            .padding(top = 28.dp, start = 16.dp, end = 16.dp),
    ) {
        Text(
            text = "Canales",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                var focused by remember { mutableStateOf(false) }
                val ui = remember(channel) { channel.toUi() }
                val programme = if (epg.status == EpgStatus.Ready) {
                    epg.nowNext(channel.epgId, nowMs)?.now?.title
                } else {
                    null
                }
                val shape = RoundedCornerShape(10.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(shape)
                        .background(
                            when {
                                focused -> Color.White.copy(alpha = 0.2f)
                                channel.id == currentId -> Color.White.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            },
                        )
                        .border(
                            if (focused) 2.dp else 0.dp,
                            if (focused) Color.White else Color.Transparent,
                            shape,
                        )
                        .then(
                            if (index == currentIndex) Modifier.focusRequester(currentFocus) else Modifier,
                        )
                        .onFocusChanged { focused = it.isFocused }
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.width(30.dp),
                    )
                    ChannelLogo(channel = ui, size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = channel.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (programme != null) {
                            Text(
                                text = programme,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(request: PlaybackRequest, modifier: Modifier = Modifier) {
    val channelIndex = request.liveChannels.indexOfFirst { it.id == request.liveChannelId }
    val epgRepo = LocalEpg.current
    val epg by epgRepo.state.collectAsState()
    val nowMs = rememberCurrentTimeMs()
    val guideReady = request.isLive && epg.status == EpgStatus.Ready
    val nowNext = if (guideReady) epg.nowNext(request.epgId, nowMs) else null
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
            if (guideReady) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append("Ahora: ")
                        append(nowNext?.now?.title ?: "sin programación")
                        nowNext?.next?.let { append("   ·   Después: ${it.title}") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (request.isLive && channelIndex >= 0 && request.liveChannels.size > 1) {
            Text(
                text = "${channelIndex + 1} / ${request.liveChannels.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.width(18.dp))
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
    val canZap = request.liveChannels.size > 1
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
                    text = when {
                        isEnded ->
                            "Fin de la reproducción · Pulsa reproducir para verla de nuevo"
                        canZap -> "Arriba/Abajo: cambiar de canal"
                        else -> "La señal en directo no se puede pausar ni adelantar"
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
        CobaltSpinner()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Conectando con la señal…",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
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
