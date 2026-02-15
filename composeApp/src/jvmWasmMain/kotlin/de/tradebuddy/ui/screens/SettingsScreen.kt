package de.tradebuddy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle
import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.util.key
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.theme.previewColorsFor
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.nav_settings
import trade_buddy.composeapp.generated.resources.settings_active_count
import trade_buddy.composeapp.generated.resources.settings_cities_desc
import trade_buddy.composeapp.generated.resources.settings_cities_empty
import trade_buddy.composeapp.generated.resources.settings_cities_search
import trade_buddy.composeapp.generated.resources.settings_logs_desc
import trade_buddy.composeapp.generated.resources.settings_logs_open
import trade_buddy.composeapp.generated.resources.settings_logs_title
import trade_buddy.composeapp.generated.resources.settings_open_settings_dir
import trade_buddy.composeapp.generated.resources.settings_open_stats_dir
import trade_buddy.composeapp.generated.resources.settings_section_cities
import trade_buddy.composeapp.generated.resources.settings_section_compact
import trade_buddy.composeapp.generated.resources.settings_section_storage
import trade_buddy.composeapp.generated.resources.settings_section_themes
import trade_buddy.composeapp.generated.resources.settings_section_tools
import trade_buddy.composeapp.generated.resources.settings_select_all
import trade_buddy.composeapp.generated.resources.settings_select_none
import trade_buddy.composeapp.generated.resources.settings_show_azimuth
import trade_buddy.composeapp.generated.resources.settings_show_azimuth_desc
import trade_buddy.composeapp.generated.resources.settings_show_moon
import trade_buddy.composeapp.generated.resources.settings_show_moon_desc
import trade_buddy.composeapp.generated.resources.settings_show_rise
import trade_buddy.composeapp.generated.resources.settings_show_rise_desc
import trade_buddy.composeapp.generated.resources.settings_show_set
import trade_buddy.composeapp.generated.resources.settings_show_set_desc
import trade_buddy.composeapp.generated.resources.settings_show_sun
import trade_buddy.composeapp.generated.resources.settings_show_sun_desc
import trade_buddy.composeapp.generated.resources.settings_show_utc
import trade_buddy.composeapp.generated.resources.settings_show_utc_desc
import trade_buddy.composeapp.generated.resources.settings_storage_desc
import trade_buddy.composeapp.generated.resources.settings_storage_path
import trade_buddy.composeapp.generated.resources.settings_subtitle
import trade_buddy.composeapp.generated.resources.settings_theme_mode
import trade_buddy.composeapp.generated.resources.settings_theme_mode_desc
import trade_buddy.composeapp.generated.resources.settings_time_optimizer_desc
import trade_buddy.composeapp.generated.resources.settings_time_optimizer_open
import trade_buddy.composeapp.generated.resources.settings_time_optimizer_title
import trade_buddy.composeapp.generated.resources.settings_title
import trade_buddy.composeapp.generated.resources.settings_themes_desc
import trade_buddy.composeapp.generated.resources.tab_statistics
import trade_buddy.composeapp.generated.resources.theme_active
import trade_buddy.composeapp.generated.resources.theme_arctic
import trade_buddy.composeapp.generated.resources.theme_aurora
import trade_buddy.composeapp.generated.resources.theme_copper
import trade_buddy.composeapp.generated.resources.theme_day
import trade_buddy.composeapp.generated.resources.theme_horizon
import trade_buddy.composeapp.generated.resources.theme_midnight
import trade_buddy.composeapp.generated.resources.theme_neon
import trade_buddy.composeapp.generated.resources.theme_night
import trade_buddy.composeapp.generated.resources.theme_nimbus
import trade_buddy.composeapp.generated.resources.theme_ocean
import trade_buddy.composeapp.generated.resources.theme_pulse
import trade_buddy.composeapp.generated.resources.theme_ruby
import trade_buddy.composeapp.generated.resources.theme_slate
import trade_buddy.composeapp.generated.resources.theme_terminal

@Composable
fun SettingsScreen(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    val scroll = rememberScrollState()
    val cityScroll = rememberScrollState()
    var cityQuery by rememberSaveable { mutableStateOf("") }
    val storageUi = remember { settingsStorageSectionUi() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(Res.string.settings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionHeader(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(Res.string.settings_section_themes),
                    subtitle = stringResource(Res.string.settings_themes_desc)
                )

                val modeLabel = if (state.themeMode == AppThemeMode.Dark) {
                    stringResource(Res.string.theme_night)
                } else {
                    stringResource(Res.string.theme_day)
                }
                SettingsToggleRow(
                    title = stringResource(Res.string.settings_theme_mode),
                    subtitle = stringResource(Res.string.settings_theme_mode_desc, modeLabel),
                    checked = state.themeMode == AppThemeMode.Dark,
                    onToggle = { enabled ->
                        viewModel.setThemeMode(if (enabled) AppThemeMode.Dark else AppThemeMode.Light)
                    }
                )

                ThemeGrid(
                    selected = state.themeStyle,
                    onSelect = viewModel::setThemeStyle
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionHeader(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(Res.string.settings_section_compact)
                )

                SettingsToggleRow(
                    title = stringResource(Res.string.settings_show_utc),
                    subtitle = stringResource(Res.string.settings_show_utc_desc),
                    checked = state.showUtcTime,
                    onToggle = viewModel::setShowUtcTime
                )
                SettingsToggleRow(
                    title = stringResource(Res.string.settings_show_azimuth),
                    subtitle = stringResource(Res.string.settings_show_azimuth_desc),
                    checked = state.showAzimuth,
                    onToggle = viewModel::setShowAzimuth
                )
                SettingsToggleRow(
                    title = stringResource(Res.string.settings_show_sun),
                    subtitle = stringResource(Res.string.settings_show_sun_desc),
                    checked = state.showSun,
                    onToggle = viewModel::setShowSun
                )
                SettingsToggleRow(
                    title = stringResource(Res.string.settings_show_moon),
                    subtitle = stringResource(Res.string.settings_show_moon_desc),
                    checked = state.showMoon,
                    onToggle = viewModel::setShowMoon
                )
                SettingsToggleRow(
                    title = stringResource(Res.string.settings_show_rise),
                    subtitle = stringResource(Res.string.settings_show_rise_desc),
                    checked = state.showRise,
                    onToggle = viewModel::setShowRise
                )
                SettingsToggleRow(
                    title = stringResource(Res.string.settings_show_set),
                    subtitle = stringResource(Res.string.settings_show_set_desc),
                    checked = state.showSet,
                    onToggle = viewModel::setShowSet
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    icon = Icons.Outlined.LocationCity,
                    title = stringResource(Res.string.settings_section_cities),
                    subtitle = stringResource(Res.string.settings_cities_desc)
                )

                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    label = { Text(stringResource(Res.string.settings_cities_search)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.applyCityFilter(state.allCities.map { it.key() }.toSet()) }
                    ) {
                        Text(stringResource(Res.string.settings_select_all))
                    }
                    OutlinedButton(onClick = { viewModel.applyCityFilter(emptySet()) }) {
                        Text(stringResource(Res.string.settings_select_none))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(
                            Res.string.settings_active_count,
                            state.selectedCityKeys.size,
                            state.allCities.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val query = cityQuery.trim().lowercase()
                val filteredCities = remember(state.allCities, query) {
                    if (query.isBlank()) {
                        state.allCities
                    } else {
                        state.allCities.filter { city ->
                            city.label.lowercase().contains(query) ||
                                city.country.lowercase().contains(query) ||
                                city.countryCode.lowercase().contains(query) ||
                                city.zoneId.lowercase().contains(query)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(cityScroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredCities.isEmpty()) {
                        Text(
                            stringResource(Res.string.settings_cities_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredCities.forEach { city ->
                            CityToggleRow(
                                city = city,
                                checked = city.key() in state.selectedCityKeys,
                                onToggle = { checked ->
                                    val updated = state.selectedCityKeys.toMutableSet()
                                    if (checked) updated.add(city.key()) else updated.remove(city.key())
                                    viewModel.applyCityFilter(updated)
                                }
                            )
                        }
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    icon = Icons.Outlined.FolderOpen,
                    title = stringResource(Res.string.settings_section_tools)
                )
                ToolEntryCard(
                    icon = Icons.Outlined.Search,
                    title = stringResource(Res.string.settings_logs_title),
                    subtitle = stringResource(Res.string.settings_logs_desc),
                    buttonLabel = stringResource(Res.string.settings_logs_open),
                    onOpen = viewModel::openLogsConsole
                )
                ToolEntryCard(
                    icon = Icons.Outlined.DateRange,
                    title = stringResource(Res.string.settings_time_optimizer_title),
                    subtitle = stringResource(Res.string.settings_time_optimizer_desc),
                    buttonLabel = stringResource(Res.string.settings_time_optimizer_open),
                    onOpen = viewModel::openTimeOptimizer
                )
            }
        }

        if (storageUi.visible) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Outlined.FolderOpen,
                        title = stringResource(Res.string.settings_section_storage),
                        subtitle = stringResource(Res.string.settings_storage_desc)
                    )

                    StorageRow(
                        title = stringResource(Res.string.nav_settings),
                        path = storageUi.settingsPath,
                        buttonLabel = if (storageUi.canOpen) {
                            stringResource(Res.string.settings_open_settings_dir)
                        } else {
                            null
                        },
                        onOpen = ::openSettingsStoragePath
                    )
                    StorageRow(
                        title = stringResource(Res.string.tab_statistics),
                        path = storageUi.statsPath,
                        buttonLabel = if (storageUi.canOpen) {
                            stringResource(Res.string.settings_open_stats_dir)
                        } else {
                            null
                        },
                        onOpen = ::openStatsStoragePath
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onOpen: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onOpen) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun ThemeGrid(
    selected: AppThemeStyle,
    onSelect: (AppThemeStyle) -> Unit
) {
    val items = remember { AppThemeStyle.entries }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { style ->
                    ThemeOptionCard(
                        style = style,
                        selected = style == selected,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    style: AppThemeStyle,
    selected: Boolean,
    onSelect: (AppThemeStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayPreview = remember(style) { previewColorsFor(style, AppThemeMode.Light) }
    val nightPreview = remember(style) { previewColorsFor(style, AppThemeMode.Dark) }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    OutlinedCard(
        onClick = { onSelect(style) },
        modifier = modifier,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeSwatchRow(
                        label = stringResource(Res.string.theme_day),
                        colors = dayPreview
                    )
                    ThemeSwatchRow(
                        label = stringResource(Res.string.theme_night),
                        colors = nightPreview
                    )
                }
                Text(themeLabel(style), style = MaterialTheme.typography.titleSmall)
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(Res.string.theme_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchRow(label: String, colors: List<Color>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colors.forEach { color ->
                ThemeSwatch(color)
            }
        }
    }
}

@Composable
private fun ThemeSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun themeLabel(style: AppThemeStyle): String = when (style) {
    AppThemeStyle.Neon -> stringResource(Res.string.theme_neon)
    AppThemeStyle.Terminal -> stringResource(Res.string.theme_terminal)
    AppThemeStyle.Midnight -> stringResource(Res.string.theme_midnight)
    AppThemeStyle.Horizon -> stringResource(Res.string.theme_horizon)
    AppThemeStyle.Ocean -> stringResource(Res.string.theme_ocean)
    AppThemeStyle.Slate -> stringResource(Res.string.theme_slate)
    AppThemeStyle.Aurora -> stringResource(Res.string.theme_aurora)
    AppThemeStyle.Copper -> stringResource(Res.string.theme_copper)
    AppThemeStyle.Arctic -> stringResource(Res.string.theme_arctic)
    AppThemeStyle.Nimbus -> stringResource(Res.string.theme_nimbus)
    AppThemeStyle.Pulse -> stringResource(Res.string.theme_pulse)
    AppThemeStyle.Ruby -> stringResource(Res.string.theme_ruby)
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun CityToggleRow(
    city: City,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val borderColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (checked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    OutlinedCard(
        onClick = { onToggle(!checked) },
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CountryFlagBadge(city.countryCode)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(city.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    city.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        city.zoneId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun CountryFlagBadge(countryCode: String) {
    val flag = remember(countryCode) { flagEmoji(countryCode) }
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(flag, style = MaterialTheme.typography.titleMedium)
    }
}

private fun flagEmoji(countryCode: String): String {
    val code = countryCode.trim().uppercase()
    return if (code.length == 2) code else countryCode
}

@Composable
private fun StorageRow(
    title: String,
    path: String,
    buttonLabel: String?,
    onOpen: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(Res.string.settings_storage_path, path),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (buttonLabel != null) {
                OutlinedButton(onClick = onOpen) {
                    Text(buttonLabel)
                }
            }
        }
    }
}
