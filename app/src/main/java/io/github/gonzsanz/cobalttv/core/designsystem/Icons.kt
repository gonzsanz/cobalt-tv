package io.github.gonzsanz.cobalttv.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A few outline glyphs that are not part of `material-icons-core`.
 * Drawing them here keeps the APK small and the icon set consistent.
 */
object CobaltIcons {

    val LiveTv: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiveTv",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 7f)
                lineTo(20.5f, 7f)
                lineTo(20.5f, 18.5f)
                lineTo(3.5f, 18.5f)
                close()
                moveTo(9f, 21.5f)
                lineTo(15f, 21.5f)
                moveTo(10.5f, 10.5f)
                lineTo(15f, 12.75f)
                lineTo(10.5f, 15f)
                close()
            }
        }.build()
    }

    val Movies: ImageVector by lazy {
        ImageVector.Builder(
            name = "Movies",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 5.5f)
                lineTo(20.5f, 5.5f)
                lineTo(20.5f, 18.5f)
                lineTo(3.5f, 18.5f)
                close()
                moveTo(7.5f, 5.5f)
                lineTo(7.5f, 18.5f)
                moveTo(16.5f, 5.5f)
                lineTo(16.5f, 18.5f)
                moveTo(3.5f, 12f)
                lineTo(7.5f, 12f)
                moveTo(16.5f, 12f)
                lineTo(20.5f, 12f)
            }
        }.build()
    }

    val Series: ImageVector by lazy {
        ImageVector.Builder(
            name = "Series",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.5f, 3.5f)
                lineTo(16.5f, 3.5f)
                lineTo(16.5f, 20.5f)
                lineTo(7.5f, 20.5f)
                close()
                moveTo(4f, 6.5f)
                lineTo(4f, 17.5f)
                moveTo(20f, 6.5f)
                lineTo(20f, 17.5f)
                moveTo(10.5f, 8.5f)
                lineTo(13.5f, 8.5f)
            }
        }.build()
    }

    val Sources: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sources",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                lineTo(20f, 10f)
                lineTo(4f, 10f)
                close()
                moveTo(4f, 14f)
                lineTo(20f, 14f)
                lineTo(20f, 18f)
                lineTo(4f, 18f)
                close()
                moveTo(7f, 8f)
                lineTo(7.01f, 8f)
                moveTo(7f, 16f)
                lineTo(7.01f, 16f)
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 5.5f)
                lineTo(18.5f, 12f)
                lineTo(8f, 18.5f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(9f, 6f)
                lineTo(9f, 18f)
                moveTo(15f, 6f)
                lineTo(15f, 18f)
            }
        }.build()
    }

    val Heart: ImageVector by lazy {
        heartBuilder("Heart", filled = false)
    }

    val HeartFilled: ImageVector by lazy {
        heartBuilder("HeartFilled", filled = true)
    }

    private fun heartBuilder(name: String, filled: Boolean): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = if (filled) null else SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 20.4f)
                curveTo(10.9f, 19.4f, 3.8f, 14.6f, 3.8f, 9.4f)
                curveTo(3.8f, 6.7f, 5.9f, 4.6f, 8.4f, 4.6f)
                curveTo(10.1f, 4.6f, 11.3f, 5.6f, 12f, 6.8f)
                curveTo(12.7f, 5.6f, 13.9f, 4.6f, 15.6f, 4.6f)
                curveTo(18.1f, 4.6f, 20.2f, 6.7f, 20.2f, 9.4f)
                curveTo(20.2f, 14.6f, 13.1f, 19.4f, 12f, 20.4f)
                close()
            }
        }.build()
}
