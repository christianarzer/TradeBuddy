package de.tradebuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.util.key
import de.tradebuddy.presentation.AppScreen
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.rememberCopyTextToClipboard
import de.tradebuddy.ui.theme.LocalExtendedColors
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.tradebuddy.ui.theme.appElevatedCardColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.format_time_short
import trade_buddy.composeapp.generated.resources.time_optimizer_astro_none
import trade_buddy.composeapp.generated.resources.time_optimizer_astro_summary
import trade_buddy.composeapp.generated.resources.time_optimizer_back
import trade_buddy.composeapp.generated.resources.time_optimizer_export_all_active
import trade_buddy.composeapp.generated.resources.time_optimizer_export_cities_title
import trade_buddy.composeapp.generated.resources.time_optimizer_export_clear
import trade_buddy.composeapp.generated.resources.time_optimizer_export_include_astro
import trade_buddy.composeapp.generated.resources.time_optimizer_export_include_moon
import trade_buddy.composeapp.generated.resources.time_optimizer_export_include_sun
import trade_buddy.composeapp.generated.resources.time_optimizer_export_settings_title
import trade_buddy.composeapp.generated.resources.time_optimizer_export_zone_city
import trade_buddy.composeapp.generated.resources.time_optimizer_export_zone_mode_title
import trade_buddy.composeapp.generated.resources.time_optimizer_export_zone_user
import trade_buddy.composeapp.generated.resources.time_optimizer_city_title
import trade_buddy.composeapp.generated.resources.time_optimizer_col_astro
import trade_buddy.composeapp.generated.resources.time_optimizer_col_date
import trade_buddy.composeapp.generated.resources.time_optimizer_col_moon
import trade_buddy.composeapp.generated.resources.time_optimizer_col_sun
import trade_buddy.composeapp.generated.resources.time_optimizer_copy_month
import trade_buddy.composeapp.generated.resources.time_optimizer_empty
import trade_buddy.composeapp.generated.resources.time_optimizer_month_next
import trade_buddy.composeapp.generated.resources.time_optimizer_month_prev
import trade_buddy.composeapp.generated.resources.time_optimizer_recalculate
import trade_buddy.composeapp.generated.resources.time_optimizer_subtitle
import trade_buddy.composeapp.generated.resources.time_optimizer_table_title
import trade_buddy.composeapp.generated.resources.time_optimizer_title
import trade_buddy.composeapp.generated.resources.value_dash

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeOptimizerScreen(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    val optimizerState = state.timeOptimizer
    val ext = LocalExtendedColors.current
    val copyToClipboard = rememberCopyTextToClipboard()
    val cityCandidates = remember(state.allCities, state.selectedCityKeys) {
        val selected = state.allCities.filter { it.key() in state.selectedCityKeys }
        if (selected.isEmpty()) state.allCities else selected
    }
    val selectedCity = cityCandidates.firstOrNull { it.key() == optimizerState.selectedCityKey }
        ?: cityCandidates.firstOrNull()
    val displayZone = remember(selectedCity?.zoneId, state.userZone) {
        selectedCity?.zoneId?.let { ZoneId.of(it) } ?: state.userZone
    }
    val timePattern = stringResource(Res.string.format_time_short)
    val timeFmt = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern, Locale.GERMANY) }
    val dash = stringResource(Res.string.value_dash)
    val selectedCityKey = selectedCity?.key()
    val selectedRows = remember(optimizerState.rowsByCity, optimizerState.rows, selectedCityKey) {
        if (selectedCityKey != null && optimizerState.rowsByCity[selectedCityKey] != null) {
            optimizerState.rowsByCity[selectedCityKey].orEmpty()
        } else {
            optimizerState.rows
        }
    }
    val activeCityKeys = remember(cityCandidates) { cityCandidates.map { it.key() }.toSet() }
    val exportCityKeys = remember(optimizerState.exportCityKeys, activeCityKeys, selectedCityKey) {
        val normalized = optimizerState.exportCityKeys.filter { it in activeCityKeys }.toSet()
        if (normalized.isNotEmpty()) normalized else selectedCityKey?.let(::setOf).orEmpty()
    }
    val exportCityRows = remember(
        state.allCities,
        optimizerState.rowsByCity,
        selectedCity,
        selectedRows,
        exportCityKeys
    ) {
        val rowsByCity = if (optimizerState.rowsByCity.isNotEmpty()) {
            optimizerState.rowsByCity
        } else {
            selectedCity?.let { mapOf(it.key() to selectedRows) }.orEmpty()
        }
        exportCityKeys.mapNotNull { key ->
            val city = state.allCities.firstOrNull { it.key() == key } ?: return@mapNotNull null
            val rows = rowsByCity[key] ?: return@mapNotNull null
            city to rows
        }
    }
    val exportText = remember(
        optimizerState.month,
        state.userZone.id,
        optimizerState.exportUseCityTimeZones,
        optimizerState.includeSun,
        optimizerState.includeMoon,
        optimizerState.includeAstro,
        exportCityRows,
        timeFmt
    ) {
        buildMonthlyExportText(
            month = optimizerState.month,
            userZone = state.userZone,
            cityRows = exportCityRows,
            useCityTimeZones = optimizerState.exportUseCityTimeZones,
            includeSun = optimizerState.includeSun,
            includeMoon = optimizerState.includeMoon,
            includeAstro = optimizerState.includeAstro,
            timeFmt = timeFmt
        )
    }
    val monthLabel = remember(optimizerState.month) {
        optimizerState.month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))
    }
    val allActiveSelected = exportCityKeys.isNotEmpty() && exportCityKeys.size == activeCityKeys.size

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(Modifier.fillMaxWidth(), colors = appElevatedCardColors()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(Res.string.time_optimizer_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            stringResource(Res.string.time_optimizer_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { viewModel.setScreen(AppScreen.Settings) }) {
                        Text(stringResource(Res.string.time_optimizer_back))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { viewModel.shiftTimeOptimizerMonth(-1) }) {
                        Text(stringResource(Res.string.time_optimizer_month_prev))
                    }
                    OutlinedButton(onClick = { viewModel.shiftTimeOptimizerMonth(1) }) {
                        Text(stringResource(Res.string.time_optimizer_month_next))
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Text(
                    stringResource(Res.string.time_optimizer_city_title),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cityCandidates.forEach { city ->
                        FilterChip(
                            selected = city.key() == optimizerState.selectedCityKey,
                            onClick = { viewModel.setTimeOptimizerCity(city.key()) },
                            label = { Text(city.label) }
                        )
                    }
                }

                Text(
                    stringResource(Res.string.time_optimizer_export_cities_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { viewModel.setTimeOptimizerExportAllActive(true) },
                        label = { Text(stringResource(Res.string.time_optimizer_export_all_active)) },
                        leadingIcon = {
                            if (allActiveSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        }
                    )
                    AssistChip(
                        onClick = { viewModel.setTimeOptimizerExportAllActive(false) },
                        label = { Text(stringResource(Res.string.time_optimizer_export_clear)) }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cityCandidates.forEach { city ->
                        val key = city.key()
                        val selected = key in exportCityKeys
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val updated = exportCityKeys.toMutableSet()
                                if (selected) updated.remove(key) else updated.add(key)
                                viewModel.setTimeOptimizerExportCities(updated)
                            },
                            label = { Text(city.label) }
                        )
                    }
                }

                Text(
                    stringResource(Res.string.time_optimizer_export_settings_title),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = optimizerState.includeSun,
                        onClick = { viewModel.setTimeOptimizerIncludeSun(!optimizerState.includeSun) },
                        label = { Text(stringResource(Res.string.time_optimizer_export_include_sun)) }
                    )
                    FilterChip(
                        selected = optimizerState.includeMoon,
                        onClick = { viewModel.setTimeOptimizerIncludeMoon(!optimizerState.includeMoon) },
                        label = { Text(stringResource(Res.string.time_optimizer_export_include_moon)) }
                    )
                    FilterChip(
                        selected = optimizerState.includeAstro,
                        onClick = { viewModel.setTimeOptimizerIncludeAstro(!optimizerState.includeAstro) },
                        label = { Text(stringResource(Res.string.time_optimizer_export_include_astro)) }
                    )
                }

                Text(
                    stringResource(Res.string.time_optimizer_export_zone_mode_title),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !optimizerState.exportUseCityTimeZones,
                        onClick = { viewModel.setTimeOptimizerExportZoneMode(false) },
                        label = { Text(stringResource(Res.string.time_optimizer_export_zone_user)) }
                    )
                    FilterChip(
                        selected = optimizerState.exportUseCityTimeZones,
                        onClick = { viewModel.setTimeOptimizerExportZoneMode(true) },
                        label = { Text(stringResource(Res.string.time_optimizer_export_zone_city)) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = viewModel::refreshTimeOptimizerMonth) {
                        Text(stringResource(Res.string.time_optimizer_recalculate))
                    }
                    OutlinedButton(
                        onClick = { copyToClipboard(exportText) },
                        enabled = exportText.isNotBlank()
                    ) {
                        Text(stringResource(Res.string.time_optimizer_copy_month))
                    }
                }
            }
        }

        if (optimizerState.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        optimizerState.error?.let { error ->
            Text(
                stringResource(error),
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            stringResource(Res.string.time_optimizer_table_title),
            style = MaterialTheme.typography.titleSmall
        )

        if (selectedRows.isEmpty()) {
            Text(
                stringResource(Res.string.time_optimizer_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedRows, key = { it.date.toString() }) { row ->
                    val sunrise = row.sunrise
                        ?.toInstant()
                        ?.atZone(displayZone)
                        ?.format(timeFmt)
                        ?: dash
                    val sunset = row.sunset
                        ?.toInstant()
                        ?.atZone(displayZone)
                        ?.format(timeFmt)
                        ?: dash
                    val moonrise = row.moonrise
                        ?.toInstant()
                        ?.atZone(displayZone)
                        ?.format(timeFmt)
                        ?: dash
                    val moonset = row.moonset
                        ?.toInstant()
                        ?.atZone(displayZone)
                        ?.format(timeFmt)
                        ?: dash
                    val astroFirst = row.firstAstroInstant
                        ?.atZone(displayZone)
                        ?.toLocalTime()
                        ?.format(timeFmt)
                    val astroLast = row.lastAstroInstant
                        ?.atZone(displayZone)
                        ?.toLocalTime()
                        ?.format(timeFmt)
                    val astroSummary = if (row.astroEventCount == 0 || astroFirst == null || astroLast == null) {
                        stringResource(Res.string.time_optimizer_astro_none)
                    } else {
                        stringResource(Res.string.time_optimizer_astro_summary, row.astroEventCount, astroFirst, astroLast)
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = appElevatedCardColors()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${stringResource(Res.string.time_optimizer_col_date)}: ${row.date}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${stringResource(Res.string.time_optimizer_col_sun)}: $sunrise / $sunset",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${stringResource(Res.string.time_optimizer_col_moon)}: $moonrise / $moonset",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${stringResource(Res.string.time_optimizer_col_astro)}: $astroSummary",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.positive
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildMonthlyExportText(
    month: YearMonth,
    userZone: ZoneId,
    cityRows: List<Pair<de.tradebuddy.domain.model.City, List<de.tradebuddy.presentation.TimeOptimizerDayRow>>>,
    useCityTimeZones: Boolean,
    includeSun: Boolean,
    includeMoon: Boolean,
    includeAstro: Boolean,
    timeFmt: DateTimeFormatter
): String {
    fun formatZoned(value: java.time.ZonedDateTime?): String =
        value?.format(timeFmt) ?: "-"

    fun formatAstro(instants: List<java.time.Instant>, zone: ZoneId): String =
        if (instants.isEmpty()) {
            "-"
        } else {
            instants.joinToString("|") { instant ->
                instant.atZone(zone).toLocalTime().format(timeFmt)
            }
        }

    if (cityRows.isEmpty() || (!includeSun && !includeMoon && !includeAstro)) return ""

    return buildString {
        appendLine("# TradeBuddy Monthly Export")
        appendLine("month,$month")
        appendLine("export_zone_mode,${if (useCityTimeZones) "city" else "user"}")
        appendLine("user_zone,${userZone.id}")
        cityRows.forEach { (city, rows) ->
            val zone = if (useCityTimeZones) ZoneId.of(city.zoneId) else userZone
            appendLine()
            appendLine("city,${city.label},zone,${zone.id}")
            val columns = mutableListOf("date")
            if (includeSun) {
                columns += "sunrise"
                columns += "sunset"
            }
            if (includeMoon) {
                columns += "moonrise"
                columns += "moonset"
            }
            if (includeAstro) {
                columns += "astro_times"
            }
            appendLine(columns.joinToString(","))
            rows.forEach { row ->
                val values = mutableListOf(row.date.toString())
                if (includeSun) {
                    values += formatZoned(row.sunrise?.toInstant()?.atZone(zone))
                    values += formatZoned(row.sunset?.toInstant()?.atZone(zone))
                }
                if (includeMoon) {
                    values += formatZoned(row.moonrise?.toInstant()?.atZone(zone))
                    values += formatZoned(row.moonset?.toInstant()?.atZone(zone))
                }
                if (includeAstro) {
                    values += formatAstro(row.astroInstants, zone)
                }
                appendLine(values.joinToString(","))
            }
        }
    }.trim()
}

