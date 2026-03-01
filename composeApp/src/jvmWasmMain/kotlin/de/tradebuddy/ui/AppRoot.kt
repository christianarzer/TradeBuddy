package de.tradebuddy.ui

import de.tradebuddy.di.AppContainer
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.presentation.AppScreen
import de.tradebuddy.presentation.MarketEventsUiState
import de.tradebuddy.presentation.MarketEventsViewModel
import de.tradebuddy.presentation.PortfolioUiState
import de.tradebuddy.presentation.PortfolioViewModel
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.presentation.TasksUiState
import de.tradebuddy.presentation.TasksViewModel
import de.tradebuddy.ui.screens.LogsConsoleScreen
import de.tradebuddy.ui.screens.MarketEventsScreen
import de.tradebuddy.ui.screens.PortfolioScreen
import de.tradebuddy.ui.screens.SettingsScreen
import de.tradebuddy.ui.screens.SunMoonScreen
import de.tradebuddy.ui.screens.TasksScreen
import de.tradebuddy.ui.screens.TimeOptimizerScreen
import de.tradebuddy.ui.theme.AppIconSize
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.AppTheme
import de.tradebuddy.ui.theme.extended
import de.tradebuddy.ui.icons.SnowIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_toggle_theme
import trade_buddy.composeapp.generated.resources.app_name
import trade_buddy.composeapp.generated.resources.app_section_dashboards
import trade_buddy.composeapp.generated.resources.app_section_favorites
import trade_buddy.composeapp.generated.resources.app_section_tools
import trade_buddy.composeapp.generated.resources.app_shell_copyright
import trade_buddy.composeapp.generated.resources.app_shell_search
import trade_buddy.composeapp.generated.resources.app_shell_separator
import trade_buddy.composeapp.generated.resources.app_shell_shortcut_slash
import trade_buddy.composeapp.generated.resources.action_toggle_favorite
import trade_buddy.composeapp.generated.resources.logo_tradebuddy_dark
import trade_buddy.composeapp.generated.resources.logo_tradebuddy_light
import trade_buddy.composeapp.generated.resources.nav_market_events
import trade_buddy.composeapp.generated.resources.nav_portfolio
import trade_buddy.composeapp.generated.resources.nav_settings
import trade_buddy.composeapp.generated.resources.nav_sun_moon
import trade_buddy.composeapp.generated.resources.settings_logs_title
import trade_buddy.composeapp.generated.resources.time_optimizer_title
import trade_buddy.composeapp.generated.resources.tasks_title

@Composable
actual fun AppRoot() {
    val container = remember { AppContainer() }
    val sunMoonViewModel = remember(container) { container.createSunMoonViewModel() }
    val marketEventsViewModel = remember(container) { container.createMarketEventsViewModel() }
    val portfolioViewModel = remember(container) { container.createPortfolioViewModel() }
    val tasksViewModel = remember(container) { container.createTasksViewModel() }

    DisposableEffect(sunMoonViewModel, marketEventsViewModel, portfolioViewModel, tasksViewModel) {
        onDispose {
            sunMoonViewModel.close()
            marketEventsViewModel.close()
            portfolioViewModel.close()
            tasksViewModel.close()
        }
    }

    val sunMoonState by sunMoonViewModel.state.collectAsState()
    val marketEventsState by marketEventsViewModel.state.collectAsState()
    val portfolioState by portfolioViewModel.state.collectAsState()
    val tasksState by tasksViewModel.state.collectAsState()

    AppTheme(
        themeMode = sunMoonState.themeMode,
        accentColor = sunMoonState.accentColor
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 1024.dp) {
                DesktopDashboardShell(
                    sunMoonState = sunMoonState,
                    sunMoonViewModel = sunMoonViewModel,
                    marketEventsState = marketEventsState,
                    marketEventsViewModel = marketEventsViewModel,
                    portfolioState = portfolioState,
                    portfolioViewModel = portfolioViewModel,
                    tasksState = tasksState,
                    tasksViewModel = tasksViewModel
                )
            } else {
                MobileDashboardShell(
                    sunMoonState = sunMoonState,
                    sunMoonViewModel = sunMoonViewModel,
                    marketEventsState = marketEventsState,
                    marketEventsViewModel = marketEventsViewModel,
                    portfolioState = portfolioState,
                    portfolioViewModel = portfolioViewModel,
                    tasksState = tasksState,
                    tasksViewModel = tasksViewModel
                )
            }
        }
    }
}

@Composable
private fun DesktopDashboardShell(
    sunMoonState: SunMoonUiState,
    sunMoonViewModel: SunMoonViewModel,
    marketEventsState: MarketEventsUiState,
    marketEventsViewModel: MarketEventsViewModel,
    portfolioState: PortfolioUiState,
    portfolioViewModel: PortfolioViewModel,
    tasksState: TasksUiState,
    tasksViewModel: TasksViewModel
) {
    val ext = MaterialTheme.extended
    var toolbarSearchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(
        normalizedScreen(sunMoonState.screen),
        tasksState.searchQuery,
        portfolioState.searchQuery,
        marketEventsState.searchQuery,
        sunMoonState.logsSearchQuery
    ) {
        toolbarSearchQuery = currentScreenSearchQuery(
            screen = normalizedScreen(sunMoonState.screen),
            tasksQuery = tasksState.searchQuery,
            portfolioQuery = portfolioState.searchQuery,
            marketEventsQuery = marketEventsState.searchQuery,
            logsQuery = sunMoonState.logsSearchQuery
        )
    }

    fun applyToolbarSearch(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return

        resolveToolbarSearchScreen(normalizedQuery)?.let { targetScreen ->
            sunMoonViewModel.setScreen(targetScreen)
            toolbarSearchQuery = ""
            return
        }

        when (normalizedScreen(sunMoonState.screen)) {
            AppScreen.Tasks -> tasksViewModel.setSearchQuery(normalizedQuery)
            AppScreen.Portfolio -> portfolioViewModel.setSearchQuery(normalizedQuery)
            AppScreen.MarketEvents -> marketEventsViewModel.setSearchQuery(normalizedQuery)
            AppScreen.Logs -> sunMoonViewModel.setLogsSearchQuery(normalizedQuery)
            else -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ext.shellBackground
    ) {
        Row(Modifier.fillMaxSize()) {
            DashboardSidebar(
                selectedScreen = sunMoonState.screen,
                themeMode = sunMoonState.themeMode,
                favoriteScreens = sunMoonState.favoriteScreens,
                onScreenChange = sunMoonViewModel::setScreen
            )
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                DashboardHeader(
                    currentScreen = sunMoonState.screen,
                    themeMode = sunMoonState.themeMode,
                    isFavorite = normalizedScreen(sunMoonState.screen) in sunMoonState.favoriteScreens,
                    onToggleFavorite = { sunMoonViewModel.toggleFavorite(sunMoonState.screen) },
                    onToggleTheme = sunMoonViewModel::toggleTheme,
                    searchQuery = toolbarSearchQuery,
                    onSearchQueryChange = { query ->
                        toolbarSearchQuery = query
                        when (normalizedScreen(sunMoonState.screen)) {
                            AppScreen.Tasks -> tasksViewModel.setSearchQuery(query)
                            AppScreen.Portfolio -> portfolioViewModel.setSearchQuery(query)
                            AppScreen.MarketEvents -> marketEventsViewModel.setSearchQuery(query)
                            AppScreen.Logs -> sunMoonViewModel.setLogsSearchQuery(query)
                            else -> Unit
                        }
                    },
                    onSearchSubmit = { applyToolbarSearch(toolbarSearchQuery) }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.l)
                ) {
                    ScreenContent(
                        sunMoonState = sunMoonState,
                        sunMoonViewModel = sunMoonViewModel,
                        marketEventsState = marketEventsState,
                        marketEventsViewModel = marketEventsViewModel,
                        portfolioState = portfolioState,
                        portfolioViewModel = portfolioViewModel,
                        tasksState = tasksState,
                        tasksViewModel = tasksViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileDashboardShell(
    sunMoonState: SunMoonUiState,
    sunMoonViewModel: SunMoonViewModel,
    marketEventsState: MarketEventsUiState,
    marketEventsViewModel: MarketEventsViewModel,
    portfolioState: PortfolioUiState,
    portfolioViewModel: PortfolioViewModel,
    tasksState: TasksUiState,
    tasksViewModel: TasksViewModel
) {
    val ext = MaterialTheme.extended
    Scaffold(
        containerColor = ext.shellBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = sunMoonViewModel::toggleTheme) {
                        Icon(
                            imageVector = themeToggleIcon(sunMoonState.themeMode),
                            contentDescription = stringResource(Res.string.action_toggle_theme)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ext.shellBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = ext.toolbarSurface) {
                NavigationBarItem(
                    selected = sunMoonState.screen == AppScreen.Tasks,
                    onClick = { sunMoonViewModel.setScreen(AppScreen.Tasks) },
                    icon = { Icon(SnowIcons.CheckSquare, contentDescription = null) },
                    label = { Text(stringResource(Res.string.tasks_title)) }
                )
                NavigationBarItem(
                    selected = sunMoonState.screen == AppScreen.Portfolio,
                    onClick = { sunMoonViewModel.setScreen(AppScreen.Portfolio) },
                    icon = { Icon(SnowIcons.ChartPie, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_portfolio)) }
                )
                NavigationBarItem(
                    selected = sunMoonState.screen == AppScreen.SunMoon || sunMoonState.screen == AppScreen.AstroCalendar,
                    onClick = { sunMoonViewModel.setScreen(AppScreen.SunMoon) },
                    icon = {
                        SunMoonBrandIcon(
                            themeMode = sunMoonState.themeMode,
                            modifier = Modifier.size(AppIconSize.xs)
                        )
                    },
                    label = { Text(stringResource(Res.string.nav_sun_moon)) }
                )
                NavigationBarItem(
                    selected = sunMoonState.screen == AppScreen.MarketEvents,
                    onClick = { sunMoonViewModel.setScreen(AppScreen.MarketEvents) },
                    icon = { Icon(SnowIcons.CalendarEvent, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_market_events)) }
                )
                NavigationBarItem(
                    selected = sunMoonState.screen == AppScreen.Settings || sunMoonState.screen == AppScreen.Logs || sunMoonState.screen == AppScreen.TimeOptimizer,
                    onClick = { sunMoonViewModel.setScreen(AppScreen.Settings) },
                    icon = { Icon(SnowIcons.Gear, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_settings)) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
            .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ScreenContent(
                sunMoonState = sunMoonState,
                sunMoonViewModel = sunMoonViewModel,
                marketEventsState = marketEventsState,
                marketEventsViewModel = marketEventsViewModel,
                portfolioState = portfolioState,
                portfolioViewModel = portfolioViewModel,
                tasksState = tasksState,
                tasksViewModel = tasksViewModel
            )
        }
    }
}

@Composable
private fun ScreenContent(
    sunMoonState: SunMoonUiState,
    sunMoonViewModel: SunMoonViewModel,
    marketEventsState: MarketEventsUiState,
    marketEventsViewModel: MarketEventsViewModel,
    portfolioState: PortfolioUiState,
    portfolioViewModel: PortfolioViewModel,
    tasksState: TasksUiState,
    tasksViewModel: TasksViewModel
) {
    when (sunMoonState.screen) {
        AppScreen.SunMoon -> SunMoonScreen(state = sunMoonState, viewModel = sunMoonViewModel)
        AppScreen.AstroCalendar -> SunMoonScreen(state = sunMoonState, viewModel = sunMoonViewModel)
        AppScreen.MarketEvents -> MarketEventsScreen(state = marketEventsState, viewModel = marketEventsViewModel)
        AppScreen.Portfolio -> PortfolioScreen(state = portfolioState, viewModel = portfolioViewModel)
        AppScreen.Tasks -> TasksScreen(state = tasksState, viewModel = tasksViewModel)
        AppScreen.Settings -> SettingsScreen(state = sunMoonState, viewModel = sunMoonViewModel)
        AppScreen.Logs -> LogsConsoleScreen(
            viewModel = sunMoonViewModel,
            searchQuery = sunMoonState.logsSearchQuery,
            onSearchQueryChange = sunMoonViewModel::setLogsSearchQuery
        )
        AppScreen.TimeOptimizer -> TimeOptimizerScreen(
            state = sunMoonState,
            viewModel = sunMoonViewModel,
            showBackToSettings = false
        )
    }
}

@Composable
private fun DashboardSidebar(
    selectedScreen: AppScreen,
    themeMode: AppThemeMode,
    favoriteScreens: Set<AppScreen>,
    onScreenChange: (AppScreen) -> Unit
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .width(212.dp)
            .fillMaxHeight()
            .border(width = 1.dp, color = ext.shellDivider)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.l),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(
                    if (themeMode == AppThemeMode.Dark) {
                        Res.drawable.logo_tradebuddy_light
                    } else {
                        Res.drawable.logo_tradebuddy_dark
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(AppIconSize.s)
            )
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(4.dp))
        SidebarSectionTitle(stringResource(Res.string.app_section_dashboards))
        SidebarNavItem(
            label = stringResource(Res.string.tasks_title),
            icon = SnowIcons.CheckSquare,
            selected = selectedScreen == AppScreen.Tasks,
            onClick = { onScreenChange(AppScreen.Tasks) }
        )
        SidebarNavItem(
            label = stringResource(Res.string.nav_portfolio),
            icon = SnowIcons.ChartPie,
            selected = selectedScreen == AppScreen.Portfolio,
            onClick = { onScreenChange(AppScreen.Portfolio) }
        )
        SidebarBrandNavItem(
            label = stringResource(Res.string.nav_sun_moon),
            selected = selectedScreen == AppScreen.SunMoon || selectedScreen == AppScreen.AstroCalendar,
            themeMode = themeMode,
            onClick = { onScreenChange(AppScreen.SunMoon) }
        )
        SidebarNavItem(
            label = stringResource(Res.string.nav_market_events),
            icon = SnowIcons.CalendarEvent,
            selected = selectedScreen == AppScreen.MarketEvents,
            onClick = { onScreenChange(AppScreen.MarketEvents) }
        )

        val orderedFavorites = favoriteScreens
            .map(::normalizedScreen)
            .distinct()
            .filterNot { it == AppScreen.AstroCalendar || it == AppScreen.Logs || it == AppScreen.TimeOptimizer }
            .sortedBy(::screenSortOrder)

        if (orderedFavorites.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            SidebarSectionTitle(stringResource(Res.string.app_section_favorites))
            orderedFavorites.forEach { screen ->
                if (screen == AppScreen.SunMoon) {
                    SidebarBrandNavItem(
                        label = screenLabel(screen),
                        selected = normalizedScreen(selectedScreen) == screen,
                        themeMode = themeMode,
                        onClick = { onScreenChange(screen) }
                    )
                } else {
                    SidebarNavItem(
                        label = screenLabel(screen),
                        icon = screenIcon(screen),
                        selected = normalizedScreen(selectedScreen) == screen,
                        onClick = { onScreenChange(screen) }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        SidebarSectionTitle(stringResource(Res.string.app_section_tools))
        SidebarTextLink(
            label = stringResource(Res.string.nav_settings),
            selected = selectedScreen == AppScreen.Settings,
            onClick = { onScreenChange(AppScreen.Settings) }
        )
        SidebarTextLink(stringResource(Res.string.settings_logs_title), selected = selectedScreen == AppScreen.Logs, indented = true) { onScreenChange(AppScreen.Logs) }
        SidebarTextLink(stringResource(Res.string.time_optimizer_title), selected = selectedScreen == AppScreen.TimeOptimizer, indented = true) { onScreenChange(AppScreen.TimeOptimizer) }

        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.app_shell_copyright),
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted
        )
    }
}

@Composable
private fun SidebarSectionTitle(text: String) {
    val ext = MaterialTheme.extended
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ext.sidebarTextMuted
    )
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) ext.sidebarSelection else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(14.dp)
                .clip(MaterialTheme.shapes.small)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(AppIconSize.xs),
            tint = if (selected) MaterialTheme.colorScheme.onSurface else ext.sidebarTextMuted
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else ext.sidebarTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SidebarBrandNavItem(
    label: String,
    selected: Boolean,
    themeMode: AppThemeMode,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) ext.sidebarSelection else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(14.dp)
                .clip(MaterialTheme.shapes.small)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
        SunMoonBrandIcon(
            themeMode = themeMode,
            modifier = Modifier.size(AppIconSize.xs)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else ext.sidebarTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SunMoonBrandIcon(
    themeMode: AppThemeMode,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(
            if (themeMode == AppThemeMode.Dark) {
                Res.drawable.logo_tradebuddy_light
            } else {
                Res.drawable.logo_tradebuddy_dark
            }
        ),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
private fun SidebarTextLink(
    label: String,
    selected: Boolean,
    indented: Boolean = false,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) ext.sidebarSelection else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(
                start = if (indented) 26.dp else 12.dp,
                end = 8.dp,
                top = 5.dp,
                bottom = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else ext.sidebarTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DashboardHeader(
    currentScreen: AppScreen,
    themeMode: AppThemeMode,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleTheme: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit
) {
    val ext = MaterialTheme.extended
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                onClick = onToggleFavorite,
                icon = SnowIcons.Star,
                contentDescription = stringResource(Res.string.action_toggle_favorite),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else ext.sidebarTextMuted
            )
            Row(
                modifier = Modifier.heightIn(min = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = screenSectionLabel(currentScreen),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.sidebarTextMuted
                )
                Text(
                    text = stringResource(Res.string.app_shell_separator),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.sidebarTextMuted
                )
                Text(
                    text = screenLabel(currentScreen),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.weight(1f))

            ToolbarSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                onSubmit = onSearchSubmit,
                modifier = Modifier.width(232.dp)
            )

            Spacer(Modifier.width(8.dp))
            HeaderIconButton(
                onClick = onToggleTheme,
                icon = themeToggleIcon(themeMode),
                contentDescription = stringResource(Res.string.action_toggle_theme)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ext.shellDivider)
        )
    }
}

@Composable
private fun ToolbarSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(MaterialTheme.shapes.small)
            .background(ext.tableRowHover)
            .border(width = 1.dp, color = ext.shellDivider, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = SnowIcons.Search,
            contentDescription = null,
            modifier = Modifier.size(AppIconSize.xs),
            tint = ext.sidebarTextMuted
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { keyEvent ->
                    when {
                        keyEvent.type != KeyEventType.KeyUp -> false
                        keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter -> {
                            onSubmit()
                            true
                        }
                        else -> false
                    }
                },
            singleLine = true,
            textStyle = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = stringResource(Res.string.app_shell_search),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.sidebarTextMuted
                    )
                }
                innerTextField()
            }
        )
        Text(
            text = stringResource(Res.string.app_shell_shortcut_slash),
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted
        )
    }
}

@Composable
private fun screenLabel(screen: AppScreen): String = when (screen) {
    AppScreen.MarketEvents -> stringResource(Res.string.nav_market_events)
    AppScreen.Portfolio -> stringResource(Res.string.nav_portfolio)
    AppScreen.Tasks -> stringResource(Res.string.tasks_title)
    AppScreen.SunMoon -> stringResource(Res.string.nav_sun_moon)
    AppScreen.AstroCalendar -> stringResource(Res.string.nav_sun_moon)
    AppScreen.Settings -> stringResource(Res.string.nav_settings)
    AppScreen.Logs -> stringResource(Res.string.settings_logs_title)
    AppScreen.TimeOptimizer -> stringResource(Res.string.time_optimizer_title)
}

private fun screenIcon(screen: AppScreen): ImageVector = when (screen) {
    AppScreen.MarketEvents -> SnowIcons.CalendarEvent
    AppScreen.Portfolio -> SnowIcons.ChartPie
    AppScreen.Tasks -> SnowIcons.CheckSquare
    AppScreen.Settings -> SnowIcons.Gear
    AppScreen.Logs -> SnowIcons.Bell
    AppScreen.TimeOptimizer -> SnowIcons.Clock
    AppScreen.SunMoon, AppScreen.AstroCalendar -> SnowIcons.Sun
}

private fun currentScreenSearchQuery(
    screen: AppScreen,
    tasksQuery: String,
    portfolioQuery: String,
    marketEventsQuery: String,
    logsQuery: String
): String = when (screen) {
    AppScreen.Tasks -> tasksQuery
    AppScreen.Portfolio -> portfolioQuery
    AppScreen.MarketEvents -> marketEventsQuery
    AppScreen.Logs -> logsQuery
    else -> ""
}

private fun screenSortOrder(screen: AppScreen): Int = when (screen) {
    AppScreen.Tasks -> 0
    AppScreen.Portfolio -> 1
    AppScreen.SunMoon, AppScreen.AstroCalendar -> 2
    AppScreen.MarketEvents -> 3
    AppScreen.Settings -> 4
    AppScreen.TimeOptimizer -> 5
    AppScreen.Logs -> 6
}

private fun normalizedScreen(screen: AppScreen): AppScreen =
    if (screen == AppScreen.AstroCalendar) AppScreen.SunMoon else screen

private fun resolveToolbarSearchScreen(query: String): AppScreen? {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return null
    return when {
        normalized.contains("sonne") ||
            normalized.contains("mond") ||
            normalized.contains("astro") ||
            normalized.contains("sun") ||
            normalized.contains("moon") -> AppScreen.SunMoon
        normalized.contains("markt") ||
            normalized.contains("event") ||
            normalized.contains("wirtschaft") ||
            normalized.contains("termin") ||
            normalized.contains("calendar") -> AppScreen.MarketEvents
        normalized.contains("portfolio") ||
            normalized.contains("asset") ||
            normalized.contains("position") -> AppScreen.Portfolio
        normalized.contains("aufgabe") ||
            normalized.contains("task") ||
            normalized.contains("ziel") ||
            normalized.contains("board") -> AppScreen.Tasks
        normalized.contains("einst") ||
            normalized.contains("setting") -> AppScreen.Settings
        normalized.contains("log") ||
            normalized.contains("protokoll") -> AppScreen.Logs
        normalized.contains("export") ||
            normalized.contains("optimizer") -> AppScreen.TimeOptimizer
        else -> null
    }
}

@Composable
private fun screenSectionLabel(screen: AppScreen): String = when (screen) {
    AppScreen.MarketEvents, AppScreen.Portfolio, AppScreen.Tasks, AppScreen.SunMoon, AppScreen.AstroCalendar ->
        stringResource(Res.string.app_section_dashboards)
    AppScreen.Settings -> stringResource(Res.string.nav_settings)
    AppScreen.Logs, AppScreen.TimeOptimizer -> stringResource(Res.string.app_section_tools)
}

private fun themeToggleIcon(themeMode: AppThemeMode): ImageVector =
    if (themeMode == AppThemeMode.Dark) SnowIcons.Sun else SnowIcons.Moon

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(AppIconSize.xs),
            tint = tint
        )
    }
}


