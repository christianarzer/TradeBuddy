package de.tradebuddy.ui.screens

import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppAccentColor
import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.util.key
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.appBlockCardColors
import de.tradebuddy.ui.theme.appFlatCardElevation
import de.tradebuddy.ui.theme.accentColorFor
import de.tradebuddy.ui.theme.extended
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.nav_settings
import trade_buddy.composeapp.generated.resources.settings_active_count
import trade_buddy.composeapp.generated.resources.settings_cities_desc
import trade_buddy.composeapp.generated.resources.settings_cities_empty
import trade_buddy.composeapp.generated.resources.settings_cities_search
import trade_buddy.composeapp.generated.resources.settings_open_settings_dir
import trade_buddy.composeapp.generated.resources.settings_open_stats_dir
import trade_buddy.composeapp.generated.resources.settings_section_cities
import trade_buddy.composeapp.generated.resources.settings_section_compact
import trade_buddy.composeapp.generated.resources.settings_section_storage
import trade_buddy.composeapp.generated.resources.settings_section_themes
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
import trade_buddy.composeapp.generated.resources.settings_theme_mode
import trade_buddy.composeapp.generated.resources.settings_theme_mode_desc
import trade_buddy.composeapp.generated.resources.settings_theme_accent
import trade_buddy.composeapp.generated.resources.settings_theme_accent_desc
import trade_buddy.composeapp.generated.resources.settings_themes_desc
import trade_buddy.composeapp.generated.resources.tab_statistics
import trade_buddy.composeapp.generated.resources.theme_day
import trade_buddy.composeapp.generated.resources.theme_night
import trade_buddy.composeapp.generated.resources.accent_purple
import trade_buddy.composeapp.generated.resources.accent_indigo
import trade_buddy.composeapp.generated.resources.accent_blue
import trade_buddy.composeapp.generated.resources.accent_green
import trade_buddy.composeapp.generated.resources.accent_yellow
import trade_buddy.composeapp.generated.resources.accent_orange
import trade_buddy.composeapp.generated.resources.accent_cyan

@Composable
fun SettingsScreen(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    val ext = MaterialTheme.extended
    val scroll = rememberScrollState()
    val cityScroll = rememberScrollState()
    var cityQuery by rememberSaveable { mutableStateOf("") }
    val storageUi = remember { settingsStorageSectionUi() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = appBlockCardColors(),
            elevation = appFlatCardElevation(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.itemGap)
            ) {
                SectionHeader(
                    icon = SnowIcons.Moon,
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
                SettingsAccentRow(
                    selected = state.accentColor,
                    onSelect = viewModel::setAccentColor
                )
            }
        }

        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = appBlockCardColors(),
            elevation = appFlatCardElevation(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.itemGap)
            ) {
                SectionHeader(
                    icon = SnowIcons.Sliders,
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

        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = appBlockCardColors(),
            elevation = appFlatCardElevation(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
            ) {
                SectionHeader(
                    icon = SnowIcons.Buildings,
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
                    leadingIcon = { Icon(SnowIcons.Search, contentDescription = null) },
                    label = { Text(stringResource(Res.string.settings_cities_search)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
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
                        .background(ext.toolbarSurface, MaterialTheme.shapes.medium)
                        .padding(horizontal = AppSpacing.s)
                        .verticalScroll(cityScroll),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
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

        if (storageUi.visible) {
            ElevatedCard(
                Modifier.fillMaxWidth(),
                colors = appBlockCardColors(),
                elevation = appFlatCardElevation(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
                ) {
                    SectionHeader(
                        icon = SnowIcons.FolderOpen,
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
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsAccentRow(
    selected: AppAccentColor,
    onSelect: (AppAccentColor) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Text(
            text = stringResource(Res.string.settings_theme_accent),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(Res.string.settings_theme_accent_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            AppAccentColor.entries.forEach { accent ->
                FilterChip(
                    selected = selected == accent,
                    onClick = { onSelect(accent) },
                    label = { Text(text = stringResource(accent.labelRes())) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(accentColorFor(accent), CircleShape)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CityToggleRow(
    city: City,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (checked) ext.tableRowSelected else Color.Transparent, MaterialTheme.shapes.small)
                .clickable { onToggle(!checked) }
                .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
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
                        .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 1.dp)
                .background(ext.shellDivider)
        )
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

private fun AppAccentColor.labelRes() = when (this) {
    AppAccentColor.Purple -> Res.string.accent_purple
    AppAccentColor.Indigo -> Res.string.accent_indigo
    AppAccentColor.Blue -> Res.string.accent_blue
    AppAccentColor.Green -> Res.string.accent_green
    AppAccentColor.Yellow -> Res.string.accent_yellow
    AppAccentColor.Orange -> Res.string.accent_orange
    AppAccentColor.Cyan -> Res.string.accent_cyan
}

@Composable
private fun StorageRow(
    title: String,
    path: String,
    buttonLabel: String?,
    onOpen: () -> Unit
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.toolbarSurface, MaterialTheme.shapes.small)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 1.dp)
                .background(ext.shellDivider)
        )
    }
}

