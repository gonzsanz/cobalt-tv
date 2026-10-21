package io.github.gonzsanz.cobalttv.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

/**
 * A focus-aware surface for TV: the only interaction feedback comes from focus,
 * never from hover. Content receives whether the surface currently owns focus so
 * labels can change colour when needed.
 */
@Composable
fun FocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    focusedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    brush: Brush? = null,
    borderColor: Color = MaterialTheme.colorScheme.border,
    focusedBorderColor: Color = Color.White,
    focusedScale: Float = 1.04f,
    enabled: Boolean = true,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val isFocused = focused && enabled

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = tween(160),
        label = "surfaceScale",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) focusedContainerColor else containerColor,
        animationSpec = tween(160),
        label = "surfaceColor",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(160),
        label = "surfaceBorder",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(shape)
            .background(brush = brush ?: SolidColor(backgroundColor), shape = shape)
            .border(
                width = borderWidth,
                color = if (isFocused) focusedBorderColor else borderColor,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick),
        content = { content(isFocused) },
    )
}

enum class CobaltButtonStyle { Primary, Secondary, Ghost }

@Composable
fun CobaltButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: CobaltButtonStyle = CobaltButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val containerColor = when (style) {
        CobaltButtonStyle.Primary -> MaterialTheme.colorScheme.primary
        CobaltButtonStyle.Secondary -> MaterialTheme.colorScheme.surfaceVariant
        CobaltButtonStyle.Ghost -> Color.Transparent
    }
    val focusedContainerColor = when (style) {
        CobaltButtonStyle.Primary -> CobaltSoft
        CobaltButtonStyle.Secondary -> Color(0xFF22314A)
        CobaltButtonStyle.Ghost -> Color.White.copy(alpha = 0.08f)
    }
    val borderColor = when (style) {
        CobaltButtonStyle.Primary -> Color.Transparent
        CobaltButtonStyle.Secondary -> MaterialTheme.colorScheme.border
        CobaltButtonStyle.Ghost -> Color.Transparent
    }

    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 50.dp)
            .widthIn(min = 150.dp),
        shape = RoundedCornerShape(14.dp),
        containerColor = containerColor,
        focusedContainerColor = focusedContainerColor,
        borderColor = borderColor,
        focusedScale = 1.03f,
        enabled = enabled,
    ) { focused ->
        val contentColor = when {
            style == CobaltButtonStyle.Primary && focused -> Graphite
            style == CobaltButtonStyle.Primary -> Color.White
            style == CobaltButtonStyle.Secondary && focused -> Color.White
            else -> TextSecondary
        }
        Row(
            modifier = Modifier
                .heightIn(min = 50.dp)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(WarningAmber.copy(alpha = 0.12f))
            .border(1.dp, WarningAmber.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(WarningAmber),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "DATOS DE DEMO",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = WarningAmber,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDemoBadge: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        if (showDemoBadge) {
            DemoBadge()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(28.dp))
            CobaltButton(text = actionText, onClick = onAction, modifier = actionModifier)
        }
    }
}

@Stable
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set

    internal var token by mutableIntStateOf(0)
        private set

    fun show(message: String) {
        this.message = message
        token++
    }

    fun dismiss() {
        message = null
    }
}

/**
 * Subtle bottom toast with immediate feedback (e.g. a feature that belongs to a
 * later phase). It never steals focus and disappears on its own.
 */
@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val message = state.message
    var lastMessage by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            lastMessage = message
            delay(3_200)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(120)) + slideInVertically(tween(180)) { it / 2 },
        exit = fadeOut(tween(150)),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GraphiteSurfaceHigh.copy(alpha = 0.97f))
                .border(1.dp, Cobalt.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Cobalt),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(50),
        containerColor = if (selected) Cobalt.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        focusedContainerColor = if (selected) Cobalt.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        borderColor = if (selected) Cobalt.copy(alpha = 0.55f) else MaterialTheme.colorScheme.border,
        focusedScale = 1.05f,
    ) { focused ->
        Box(
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    selected || focused -> CobaltSoft
                    else -> TextSecondary
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
fun InitialFocusEffect(focusRequester: FocusRequester) {
    LaunchedEffect(focusRequester) {
        // The target may not be attached on the first frame (lazy lists, NavHost
        // transitions), so retry briefly instead of assuming immediate layout.
        repeat(10) {
            delay(100)
            if (runCatching { focusRequester.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
}

@Composable
fun InitialsAvatar(
    name: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val initials = remember(name) {
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.22f))
            .border(1.dp, accent.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
    }
}
