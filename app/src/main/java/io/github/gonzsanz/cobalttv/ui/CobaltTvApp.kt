package io.github.gonzsanz.cobalttv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gonzsanz.cobalttv.core.designsystem.Cobalt
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltDeep
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltIcons
import io.github.gonzsanz.cobalttv.core.designsystem.TextMuted
import io.github.gonzsanz.cobalttv.core.designsystem.ToastHost
import io.github.gonzsanz.cobalttv.core.designsystem.ToastState
import io.github.gonzsanz.cobalttv.core.playback.PlaybackRequest
import io.github.gonzsanz.cobalttv.core.playback.PlaybackSession
import io.github.gonzsanz.cobalttv.data.catalog.CatalogRepository
import io.github.gonzsanz.cobalttv.data.epg.EpgRepository
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.gonzsanz.cobalttv.core.playback.PlayerHolder
import io.github.gonzsanz.cobalttv.data.favorites.FavoritesRepository
import io.github.gonzsanz.cobalttv.feature.favorites.FavoritesScreen
import io.github.gonzsanz.cobalttv.feature.guide.GuideScreen
import io.github.gonzsanz.cobalttv.feature.live.LiveScreen
import io.github.gonzsanz.cobalttv.feature.movies.MoviesScreen
import io.github.gonzsanz.cobalttv.feature.player.PlayerScreen
import io.github.gonzsanz.cobalttv.feature.search.SearchScreen
import io.github.gonzsanz.cobalttv.feature.series.SeriesDetailScreen
import io.github.gonzsanz.cobalttv.feature.series.SeriesScreen
import io.github.gonzsanz.cobalttv.feature.series.SeriesSession
import io.github.gonzsanz.cobalttv.feature.settings.SettingsScreen
import io.github.gonzsanz.cobalttv.feature.sources.SourcesScreen
import io.github.gonzsanz.cobalttv.ui.navigation.Destination

@Composable
fun CobaltTvApp() {
    val navController = rememberNavController()
    val toastState = remember { ToastState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isPlayer = currentRoute == Destination.PLAYER_ROUTE

    val openPlayer: (PlaybackRequest) -> Unit = { request ->
        PlaybackSession.current = request
        navController.navigate(Destination.PLAYER_ROUTE) {
            launchSingleTop = true
        }
    }

    val context = LocalContext.current
    val catalog = remember(context.applicationContext) {
        CatalogRepository(context.applicationContext)
    }
    val favorites = remember(context.applicationContext) {
        FavoritesRepository(context.applicationContext)
    }
    val epg = remember(catalog) {
        EpgRepository(catalog)
    }
    val playerHolder = remember(context.applicationContext) {
        PlayerHolder(context.applicationContext)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(playerHolder, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> playerHolder.onAppStop()
                Lifecycle.Event.ON_START -> playerHolder.onAppStart()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerHolder.release()
        }
    }
    // The preview and the player share one stream; leaving TV, Guide and the
    // player must stop it so nothing keeps playing in the background.
    LaunchedEffect(currentRoute) {
        val keep = currentRoute == Destination.Live.route ||
            currentRoute == Destination.Guide.route ||
            currentRoute == Destination.PLAYER_ROUTE
        if (!keep && currentRoute != null) playerHolder.stop()
    }

    CompositionLocalProvider(
        LocalCatalog provides catalog,
        LocalFavorites provides favorites,
        LocalEpg provides epg,
        LocalPlayer provides playerHolder,
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(Modifier.fillMaxSize()) {
            if (!isPlayer) {
                SideNav(
                    currentRoute = currentRoute,
                    onNavigate = { navController.navigateTopLevel(it) },
                )
            }

            NavHost(
                navController = navController,
                startDestination = Destination.Live.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
                modifier = if (isPlayer) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                },
            ) {
                composable(Destination.Live.route) {
                    LiveScreen(
                        onPlay = openPlayer,
                        onToast = toastState::show,
                    )
                }
                composable(Destination.Guide.route) {
                    GuideScreen(onPlay = openPlayer)
                }
                composable(Destination.Movies.route) {
                    MoviesScreen(onPlay = openPlayer)
                }
                composable(Destination.Series.route) {
                    SeriesScreen(onOpenSeries = { item ->
                        SeriesSession.current = item
                        navController.navigate(Destination.SERIES_DETAIL_ROUTE)
                    })
                }
                composable(Destination.Favorites.route) {
                    FavoritesScreen(
                        onPlay = openPlayer,
                        onExploreLive = { navController.navigateTopLevel(Destination.Live) },
                        onToast = toastState::show,
                    )
                }
                composable(Destination.Search.route) {
                    SearchScreen(onPlay = openPlayer)
                }
                composable(Destination.Settings.route) {
                    SettingsScreen(
                        onOpenSources = { navController.navigate(Destination.SOURCES_ROUTE) },
                        onToast = toastState::show,
                    )
                }
                composable(Destination.SOURCES_ROUTE) {
                    SourcesScreen(
                        onOpenLive = {
                            navController.navigateTopLevel(Destination.Live)
                        },
                    )
                }
                composable(Destination.SERIES_DETAIL_ROUTE) {
                    SeriesDetailScreen(onPlay = openPlayer)
                }
                composable(Destination.PLAYER_ROUTE) {
                    PlayerScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        if (!isPlayer) {
            ToastHost(
                state = toastState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp),
            )
        }
    }
    }
}

private fun NavHostController.navigateTopLevel(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun SideNav(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(104.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark()
        Spacer(Modifier.height(14.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Destination.entries
                .filter { it != Destination.Settings }
                .forEach { destination ->
                    SideNavItem(
                        destination = destination,
                        icon = iconFor(destination),
                        selected = currentRoute == destination.route,
                        onClick = { onNavigate(destination) },
                    )
                }
        }

        Spacer(Modifier.weight(1f))

        SideNavItem(
            destination = Destination.Settings,
            icon = Icons.Filled.Settings,
            selected = currentRoute == Destination.Settings.route ||
                currentRoute == Destination.SOURCES_ROUTE,
            onClick = { onNavigate(Destination.Settings) },
        )
    }
}

@Composable
private fun BrandMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Cobalt, CobaltDeep)))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "C",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "COBALT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
            ),
            color = TextMuted,
        )
    }
}

@Composable
private fun SideNavItem(
    destination: Destination,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    val background by animateColorAsState(
        targetValue = when {
            focused -> Color.White.copy(alpha = 0.10f)
            selected -> Cobalt.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        animationSpec = tween(140),
        label = "navBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (focused || selected) Color.White else TextMuted,
        animationSpec = tween(140),
        label = "navContent",
    )
    val borderColor = when {
        focused -> Color.White
        selected -> Cobalt.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .width(84.dp)
            .height(47.dp)
            .clip(shape)
            .background(background)
            .border(if (focused) 2.dp else 1.dp, borderColor, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.shortLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = contentColor,
            maxLines = 1,
        )
    }
}

private fun iconFor(destination: Destination): ImageVector = when (destination) {
    Destination.Live -> CobaltIcons.LiveTv
    Destination.Guide -> Icons.Filled.DateRange
    Destination.Movies -> CobaltIcons.Movies
    Destination.Series -> CobaltIcons.Series
    Destination.Favorites -> Icons.Filled.Favorite
    Destination.Search -> Icons.Filled.Search
    Destination.Settings -> Icons.Filled.Settings
}
