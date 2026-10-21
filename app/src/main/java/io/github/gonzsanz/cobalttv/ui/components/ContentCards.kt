package io.github.gonzsanz.cobalttv.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.FocusableSurface
import io.github.gonzsanz.cobalttv.core.designsystem.InitialsAvatar
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.TextPrimary
import io.github.gonzsanz.cobalttv.core.designsystem.TextSecondary
import io.github.gonzsanz.cobalttv.data.demo.DemoContinueWatching
import io.github.gonzsanz.cobalttv.data.demo.DemoSearchHit
import io.github.gonzsanz.cobalttv.ui.model.ChannelUi

@Composable
fun PosterCard(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 176.dp,
    height: Dp = 264.dp,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(18.dp),
        borderColor = Color.White.copy(alpha = 0.06f),
        focusedBorderColor = Color.White,
        focusedScale = 1.06f,
        brush = Brush.linearGradient(
            listOf(
                accent.copy(alpha = 0.95f),
                accent.copy(alpha = 0.42f),
                Color(0xFF0B1120),
            ),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(width * 0.9f)
                    .padding(top = 18.dp, end = 10.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                            startY = height.value * 0.35f,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: ChannelUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .width(248.dp)
            .height(140.dp),
        shape = RoundedCornerShape(18.dp),
        focusedScale = 1.05f,
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = channel.name, accent = channel.accent, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (focused) Color.White else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = channel.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = channel.nowPlaying,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: DemoContinueWatching,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .width(320.dp)
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        borderColor = Color.White.copy(alpha = 0.06f),
        focusedBorderColor = Color.White,
        focusedScale = 1.04f,
        brush = Brush.linearGradient(
            listOf(
                item.accent.copy(alpha = 0.85f),
                item.accent.copy(alpha = 0.35f),
                Color(0xFF0A1020),
            ),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                            startY = 40f,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
            ) {
                Text(
                    text = "CONTINUAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    hit: DemoSearchHit,
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
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(name = hit.title, accent = hit.accent, size = 44.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = hit.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (focused) Color.White else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = hit.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = hit.kind.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = hit.accent,
            )
        }
    }
}

@Composable
fun ChannelGrid(
    channels: List<ChannelUi>,
    onChannelClick: (ChannelUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 240.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 48.dp,
            end = 48.dp,
            top = 12.dp,
            bottom = 40.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(
            items = channels,
            key = { it.id },
        ) { channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel) },
            )
        }
    }
}
