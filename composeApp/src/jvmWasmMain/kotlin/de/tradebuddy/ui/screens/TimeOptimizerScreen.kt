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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
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
import trade_buddy.composeapp.generated.resources.time_optimizer_col_astro
import trade_buddy.composeapp.generated.resources.time_optimizer_col_date
import trade_buddy.composeapp.generated.resources.time_optimizer_col_moon
import trade_buddy.composeapp.generated.resources.time_optimizer_col_sun
import trade_buddy.composeapp.generated.resources.time_optimizer_copy_month
import trade_buddy.composeapp.generated.resources.time_optimizer_empty
import trade_buddy.composeapp.generated.resources.time_optimizer_month_next
import trade_buddy.composeapp.generated.resources.time_optimizer_month_prev
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
    val timePattern = stringResource(Res.string.format_time_short)
    val timeFmt = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern, Locale.GERMANY) }
    val dash = stringResource(Res.string.value_dash)
    val activeCityKeys = remember(cityCandidates) { cityCandidates.map { it.key() }.toSet() }
    val exportCityKeys = remember(optimizerState.exportCityKeys, activeCityKeys) {
        val normalized = optimizerState.exportCityKeys.filter { it in activeCityKeys }.toSet()
        if (normalized.isNotEmpty()) normalized else cityCandidates.firstOrNull()?.key()?.let(::setOf).orEmpty()
    }
    val previewCity = remember(cityCandidates, exportCityKeys) {
        cityCandidates.firstOrNull { it.key() in exportCityKeys } ?: cityCandidates.firstOrNull()
    }
    val previewCityKey = previewCity?.key()
    val displayZone = state.userZone
    val selectedRows = remember(optimizerState.rowsByCity, optimizerState.rows, previewCityKey) {
        if (previewCityKey != null && optimizerState.rowsByCity[previewCityKey] != null) {
            optimizerState.rowsByCity[previewCityKey].orEmpty()
        } else {
            optimizerState.rows
        }
    }
    val exportCityRows = remember(
        state.allCities,
        optimizerState.rowsByCity,
        previewCity,
        selectedRows,
        exportCityKeys
    ) {
        val rowsByCity = if (optimizerState.rowsByCity.isNotEmpty()) {
            optimizerState.rowsByCity
        } else {
            previewCity?.let { mapOf(it.key() to selectedRows) }.orEmpty()
        }
        exportCityKeys.mapNotNull { key ->
            val city = state.allCities.firstOrNull { it.key() == key } ?: return@mapNotNull null
            val rows = rowsByCity[key] ?: return@mapNotNull null
            city to rows
        }
    }
    val dailyPreviewRows = remember(exportCityRows) {
        val byDate = linkedMapOf<java.time.LocalDate, MutableList<Pair<de.tradebuddy.domain.model.City, de.tradebuddy.presentation.TimeOptimizerDayRow>>>()
        exportCityRows.forEach { (city, rows) ->
            rows.forEach { row ->
                byDate.getOrPut(row.date) { mutableListOf() }.add(city to row)
            }
        }
        byDate.entries.map { it.key to it.value.toList() }
    }
    val previewListState = rememberLazyListState()
    val listProgress by remember(dailyPreviewRows, previewListState) {
        derivedStateOf {
            val total = dailyPreviewRows.size.coerceAtLeast(1)
            val visibleCount = previewListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val endIndex = (previewListState.firstVisibleItemIndex + visibleCount).coerceAtMost(total)
            endIndex.toFloat() / total.toFloat()
        }
    }
    val listPositionLabel by remember(dailyPreviewRows, previewListState) {
        derivedStateOf {
            val total = dailyPreviewRows.size
            if (total == 0) {
                "0/0"
            } else {
                val visibleCount = previewListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                val endIndex = (previewListState.firstVisibleItemIndex + visibleCount).coerceAtMost(total)
                "$endIndex/$total"
            }
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
            useUtcTimes = optimizerState.exportUseCityTimeZones,
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
    var exportSettingsExpanded by rememberSaveable { mutableStateOf(false) }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.time_optimizer_export_settings_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    AssistChip(
                        onClick = { exportSettingsExpanded = !exportSettingsExpanded },
                        label = { Text(if (exportSettingsExpanded) "Einklappen" else "Ausklappen") },
                        leadingIcon = {
                            Icon(
                                if (exportSettingsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null
                            )
                        }
                    )
                }
                if (exportSettingsExpanded) {
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
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            ,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (dailyPreviewRows.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { listProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = listPositionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (dailyPreviewRows.isEmpty()) {
            Text(
                stringResource(Res.string.time_optimizer_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state = previewListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dailyPreviewRows, key = { it.first.toString() }) { (date, cityRowsForDay) ->
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
                                "${stringResource(Res.string.time_optimizer_col_date)}: $date",
                                style = MaterialTheme.typography.titleSmall
                            )
                            cityRowsForDay.forEachIndexed { index, (city, row) ->
                                if (index > 0) {
                                    HorizontalDivider()
                                }
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

                                Text(
                                    city.label,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (optimizerState.includeSun) {
                                    Text(
                                        "${stringResource(Res.string.time_optimizer_col_sun)}: $sunrise / $sunset",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (optimizerState.includeMoon) {
                                    Text(
                                        "${stringResource(Res.string.time_optimizer_col_moon)}: $moonrise / $moonset",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (optimizerState.includeAstro) {
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
    }
}

private fun buildMonthlyExportText(
    month: YearMonth,
    userZone: ZoneId,
    cityRows: List<Pair<de.tradebuddy.domain.model.City, List<de.tradebuddy.presentation.TimeOptimizerDayRow>>>,
    useUtcTimes: Boolean,
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
        appendLine("export_zone_mode,${if (useUtcTimes) "utc" else "user"}")
        appendLine("user_zone,${userZone.id}")
        cityRows.forEach { (city, rows) ->
            val zone = if (useUtcTimes) ZoneId.of("UTC") else userZone
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

