package io.github.gonzsanz.cobalttv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import io.github.gonzsanz.cobalttv.core.designsystem.FocusableSurface
import io.github.gonzsanz.cobalttv.ui.model.accentFor

/** Poster for provider content (movies and series) with a gradient fallback. */
@Composable
fun VodPosterCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    height: Dp = 225.dp,
) {
    val accent = accentFor(title)
    val fallback = Brush.linearGradient(
        listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.4f), Color(0xFF0B1120)),
    )
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.width(width).height(height),
        shape = RoundedCornerShape(14.dp),
        borderColor = Color.White.copy(alpha = 0.06f),
        focusedScale = 1.06f,
        brush = fallback,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!imageUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {},
                    error = {},
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = height.value * 0.45f,
                        ),
                    ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
