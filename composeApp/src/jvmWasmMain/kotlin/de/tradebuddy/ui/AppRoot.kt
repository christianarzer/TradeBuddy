package de.tradebuddy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.tradebuddy.di.AppContainer
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.presentation.AppScreen
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.screens.LogsConsoleScreen
import de.tradebuddy.ui.screens.SettingsScreen
import de.tradebuddy.ui.screens.SunMoonScreen
import de.tradebuddy.ui.screens.TimeOptimizerScreen
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_toggle_theme
import trade_buddy.composeapp.generated.resources.app_name
import trade_buddy.composeapp.generated.resources.app_subtitle
import trade_buddy.composeapp.generated.resources.nav_settings
import trade_buddy.composeapp.generated.resources.nav_sun_moon

@Composable
actual fun AppRoot() {
    val container = remember { AppContainer() }
    val viewModel = remember(container) { container.createSunMoonViewModel() }
    DisposableEffect(viewModel) {
        onDispose { viewModel.close() }
    }

    val state by viewModel.state.collectAsState()

    AppRootContent(state = state, viewModel = viewModel)
}

@Composable
internal fun AppRootContent(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    AppTheme(
        themeStyle = state.themeStyle,
        themeMode = state.themeMode
    ) {
        AppScaffold(
            state = state,
            viewModel = viewModel
        )
    }
}

@Composable
private fun AppScaffold(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 980.dp
        val backgroundBrush = if (state.themeMode == AppThemeMode.Light) {
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    MaterialTheme.colorScheme.background
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.background
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    AppTopBar(
                        state = state,
                        onToggleTheme = viewModel::toggleTheme,
                        showThemeToggle = !isWide
                    )
                },
                bottomBar = {
                    if (!isWide) {
                        AppBottomBar(
                            screen = state.screen,
                            onScreenChange = viewModel::setScreen,
                            themeMode = state.themeMode
                        )
                    }
                }
            ) { padding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (isWide) {
                        AppNavigationRail(
                            screen = state.screen,
                            onScreenChange = viewModel::setScreen,
                            onToggleTheme = viewModel::toggleTheme,
                            themeMode = state.themeMode
                        )
                        Spacer(Modifier.width(AppSpacing.s))
                    }

                    var visible by remember(state.screen) { mutableStateOf(false) }
                    LaunchedEffect(state.screen) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isWide) AppSpacing.screenHPaddingWide else AppSpacing.screenHPadding, vertical = AppSpacing.m)
                        ) {
                            when (state.screen) {
                                AppScreen.SunMoon -> SunMoonScreen(state = state, viewModel = viewModel)
                                AppScreen.AstroCalendar -> SunMoonScreen(state = state, viewModel = viewModel)
                                AppScreen.Settings -> SettingsScreen(state = state, viewModel = viewModel)
                                AppScreen.Logs -> LogsConsoleScreen(viewModel = viewModel)
                                AppScreen.TimeOptimizer -> TimeOptimizerScreen(state = state, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    state: SunMoonUiState,
    onToggleTheme: () -> Unit,
    showThemeToggle: Boolean
) {
    val themeIcon = if (state.themeMode == AppThemeMode.Dark) {
        Icons.Outlined.WbSunny
    } else {
        Icons.Outlined.DarkMode
    }
    val themeToggleTint = MaterialTheme.colorScheme.onSurface
    val topBarContainer = if (state.themeMode == AppThemeMode.Light) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = appLogoPainter(),
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier.size(28.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            if (showThemeToggle) {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        themeIcon,
                        contentDescription = stringResource(Res.string.action_toggle_theme),
                        tint = themeToggleTint
                    )
                }
            }
        }
    )
}

@Composable
private fun AppNavigationRail(
    screen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onToggleTheme: () -> Unit,
    themeMode: AppThemeMode
) {
    val themeToggleTint = MaterialTheme.colorScheme.onSurface
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (themeMode == AppThemeMode.Light) 0.95f else 0.88f
        )
    ) {
        NavigationRailItem(
            selected = screen == AppScreen.SunMoon,
            onClick = { onScreenChange(AppScreen.SunMoon) },
            icon = { Icon(Icons.Outlined.WbSunny, contentDescription = null) },
            label = {
                Text(
                    stringResource(Res.string.nav_sun_moon),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
        NavigationRailItem(
            selected = screen == AppScreen.Settings ||
                screen == AppScreen.Logs ||
                screen == AppScreen.TimeOptimizer,
            onClick = { onScreenChange(AppScreen.Settings) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_settings)) }
        )
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (themeMode == AppThemeMode.Dark) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode,
                    contentDescription = stringResource(Res.string.action_toggle_theme),
                    tint = themeToggleTint
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    screen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    themeMode: AppThemeMode
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (themeMode == AppThemeMode.Light) 0.95f else 0.88f
        )
    ) {
        NavigationBarItem(
            selected = screen == AppScreen.SunMoon,
            onClick = { onScreenChange(AppScreen.SunMoon) },
            icon = { Icon(Icons.Outlined.WbSunny, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_sun_moon)) }
        )
        NavigationBarItem(
            selected = screen == AppScreen.Settings ||
                screen == AppScreen.Logs ||
                screen == AppScreen.TimeOptimizer,
            onClick = { onScreenChange(AppScreen.Settings) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_settings)) }
        )
    }
}
