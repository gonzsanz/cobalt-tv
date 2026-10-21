package io.github.gonzsanz.cobalttv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import io.github.gonzsanz.cobalttv.core.designsystem.InitialsAvatar
import io.github.gonzsanz.cobalttv.ui.model.ChannelUi

/** Channel logo from the playlist, with initials while loading or when missing. */
@Composable
fun ChannelLogo(
    channel: ChannelUi,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val url = channel.logoUrl
    if (url.isNullOrBlank()) {
        InitialsAvatar(name = channel.name, accent = channel.accent, modifier = modifier, size = size)
        return
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(3.dp),
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size),
            loading = {},
            error = {
                InitialsAvatar(name = channel.name, accent = channel.accent, size = size - 6.dp)
            },
        )
    }
}
