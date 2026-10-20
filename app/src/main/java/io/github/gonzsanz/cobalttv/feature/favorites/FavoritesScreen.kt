package io.github.gonzsanz.cobalttv.feature.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.gonzsanz.cobalttv.core.designsystem.EmptyState
import io.github.gonzsanz.cobalttv.core.designsystem.InitialFocusEffect
import io.github.gonzsanz.cobalttv.core.designsystem.ScreenHeader

@Composable
fun FavoritesScreen(onExploreLive: () -> Unit) {
    val actionFocusRequester = remember { FocusRequester() }
    InitialFocusEffect(actionFocusRequester)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Favoritos",
            subtitle = "Canales y títulos que quieras tener siempre a mano.",
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Filled.Favorite,
                title = "Todavía no hay favoritos",
                description = "Cuando guardes un canal o un título aparecerá aquí. " +
                    "Los favoritos persistentes llegarán en la Fase 4.",
                actionText = "Explorar TV en directo",
                onAction = onExploreLive,
                actionModifier = Modifier.focusRequester(actionFocusRequester),
            )
        }
    }
}
