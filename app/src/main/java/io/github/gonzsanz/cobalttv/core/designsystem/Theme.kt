package io.github.gonzsanz.cobalttv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val CobaltColorScheme = darkColorScheme(
    primary = Cobalt,
    onPrimary = Color.White,
    primaryContainer = CobaltDeep,
    onPrimaryContainer = Color.White,
    secondary = CobaltSoft,
    onSecondary = Graphite,
    background = Graphite,
    onBackground = TextPrimary,
    surface = GraphiteSurface,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    border = GraphiteOutline,
    borderVariant = Color(0xFF1B2639),
    error = ErrorRed,
    onError = Color.White,
)

private val CobaltShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CobaltTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CobaltColorScheme,
        typography = CobaltTypography,
        shapes = CobaltShapes,
        content = content,
    )
}
