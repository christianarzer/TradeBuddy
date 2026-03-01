package de.tradebuddy.ui.screens

import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.util.key
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.presentation.TimeOptimizerDayRow
import de.tradebuddy.ui.components.rememberCopyTextToClipboard
import de.tradebuddy.ui.components.shared.SnowToolbar
import de.tradebuddy.ui.components.shared.SnowToolbarIconButton
import de.tradebuddy.ui.components.shared.SnowToolbarPopupPanel
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.LocalExtendedColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.time_optimizer_astro_none
import trade_buddy.composeapp.generated.resources.time_optimizer_astro_summary
import trade_buddy.composeapp.generated.resources.time_optimizer_copy_tradingview
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
import trade_buddy.composeapp.generated.resources.time_optimizer_col_date
import trade_buddy.composeapp.generated.resources.time_optimizer_copy_month
import trade_buddy.composeapp.generated.resources.time_optimizer_empty
import trade_buddy.composeapp.generated.resources.time_optimizer_month_next
import trade_buddy.composeapp.generated.resources.time_optimizer_month_prev
import trade_buddy.composeapp.generated.resources.time_optimizer_sort_asc
import trade_buddy.composeapp.generated.resources.time_optimizer_sort_desc
import trade_buddy.composeapp.generated.resources.time_optimizer_subtitle
import trade_buddy.composeapp.generated.resources.time_optimizer_table_title
import trade_buddy.composeapp.generated.resources.time_optimizer_title
import trade_buddy.composeapp.generated.resources.value_dash

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeOptimizerScreen(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel,
    @Suppress("UNUSED_PARAMETER") showBackToSettings: Boolean = false
) {
    val optimizerState = state.timeOptimizer
    val ext = LocalExtendedColors.current
    val copyToClipboard = rememberCopyTextToClipboard()
    val cityCandidates = remember(state.allCities, state.selectedCityKeys) {
        val selected = state.allCities.filter { it.key() in state.selectedCityKeys }
        if (selected.isEmpty()) state.allCities else selected
    }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY) }
    val dateTimeCopyFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.GERMANY) }
    val dash = stringResource(Res.string.value_dash)
    val activeCityKeys = remember(cityCandidates) { cityCandidates.map { it.key() }.toSet() }
    val exportCityKeys = remember(optimizerState.exportCityKeys, activeCityKeys) {
        val normalized = optimizerState.exportCityKeys.filter { it in activeCityKeys }.toSet()
        if (normalized.isNotEmpty()) normalized else activeCityKeys
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
        val byDate = linkedMapOf<java.time.LocalDate, MutableList<Pair<City, TimeOptimizerDayRow>>>()
        exportCityRows.forEach { (city, rows) ->
            rows.forEach { row ->
                byDate.getOrPut(row.date) { mutableListOf() }.add(city to row)
            }
        }
        byDate.entries.map { it.key to it.value.toList() }
    }
    var sortAscending by rememberSaveable { mutableStateOf(true) }
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    val sortedDailyPreviewRows = remember(dailyPreviewRows, sortAscending) {
        if (sortAscending) {
            dailyPreviewRows.sortedBy { it.first }
        } else {
            dailyPreviewRows.sortedByDescending { it.first }
        }
    }
    val previewListState = rememberLazyListState()
    val listProgress by remember(sortedDailyPreviewRows, previewListState) {
        derivedStateOf {
            val total = sortedDailyPreviewRows.size.coerceAtLeast(1)
            val visibleCount = previewListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val endIndex = (previewListState.firstVisibleItemIndex + visibleCount).coerceAtMost(total)
            endIndex.toFloat() / total.toFloat()
        }
    }
    val listPositionLabel by remember(sortedDailyPreviewRows, previewListState) {
        derivedStateOf {
            val total = sortedDailyPreviewRows.size
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
    val tradingViewInputText = remember(
        optimizerState.month,
        state.userZone.id,
        optimizerState.exportUseCityTimeZones,
        optimizerState.includeSun,
        optimizerState.includeMoon,
        optimizerState.includeAstro,
        exportCityRows,
        dateTimeCopyFmt
    ) {
        buildTradingViewInputText(
            month = optimizerState.month,
            userZone = state.userZone,
            cityRows = exportCityRows,
            useUtcTimes = optimizerState.exportUseCityTimeZones,
            includeSun = optimizerState.includeSun,
            includeMoon = optimizerState.includeMoon,
            includeAstro = optimizerState.includeAstro,
            dateTimeFmt = dateTimeCopyFmt
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
        Text(
            stringResource(Res.string.time_optimizer_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            stringResource(Res.string.time_optimizer_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SnowToolbar {
            Box {
                SnowToolbarIconButton(icon = SnowIcons.Filter, onClick = { showFilterPopup = true })
                DropdownMenu(
                    expanded = showFilterPopup,
                    onDismissRequest = { showFilterPopup = false }
                ) {
                    SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 320.dp, max = 460.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { viewModel.setTimeOptimizerExportAllActive(true) },
                                label = { Text(stringResource(Res.string.time_optimizer_export_all_active)) },
                                leadingIcon = {
                                    if (allActiveSelected) {
                                        Icon(SnowIcons.Check, contentDescription = null)
                                    }
                                }
                            )
                            AssistChip(
                                onClick = { viewModel.setTimeOptimizerExportAllActive(false) },
                                label = { Text(stringResource(Res.string.time_optimizer_export_clear)) }
                            )
                        }
                        Text(
                            stringResource(Res.string.time_optimizer_export_cities_title),
                            style = MaterialTheme.typography.titleSmall
                        )
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
                            stringResource(Res.string.time_optimizer_export_zone_mode_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = optimizerState.exportUseCityTimeZones,
                                onClick = { viewModel.setTimeOptimizerExportZoneMode(true) },
                                label = { Text(stringResource(Res.string.time_optimizer_export_zone_city)) }
                            )
                            FilterChip(
                                selected = !optimizerState.exportUseCityTimeZones,
                                onClick = { viewModel.setTimeOptimizerExportZoneMode(false) },
                                label = { Text(stringResource(Res.string.time_optimizer_export_zone_user)) }
                            )
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
                    }
                }
            }
            Box {
                SnowToolbarIconButton(icon = SnowIcons.Sort, onClick = { showSortPopup = true })
                DropdownMenu(
                    expanded = showSortPopup,
                    onDismissRequest = { showSortPopup = false }
                ) {
                    SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 220.dp, max = 280.dp)) {
                        FilterChip(
                            selected = sortAscending,
                            onClick = {
                                sortAscending = true
                                showSortPopup = false
                            },
                            label = { Text(stringResource(Res.string.time_optimizer_sort_asc)) }
                        )
                        FilterChip(
                            selected = !sortAscending,
                            onClick = {
                                sortAscending = false
                                showSortPopup = false
                            },
                            label = { Text(stringResource(Res.string.time_optimizer_sort_desc)) }
                        )
                    }
                }
            }
            SnowToolbarIconButton(
                icon = SnowIcons.ArrowLeft,
                onClick = { viewModel.shiftTimeOptimizerMonth(-1) },
                contentDescription = stringResource(Res.string.time_optimizer_month_prev)
            )
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleSmall
            )
            SnowToolbarIconButton(
                icon = SnowIcons.ArrowRight,
                onClick = { viewModel.shiftTimeOptimizerMonth(1) },
                contentDescription = stringResource(Res.string.time_optimizer_month_next)
            )
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.OutlinedButton(
                onClick = { copyToClipboard(exportText) },
                enabled = exportText.isNotBlank()
            ) {
                Text(stringResource(Res.string.time_optimizer_copy_month))
            }
            androidx.compose.material3.OutlinedButton(
                onClick = { copyToClipboard(tradingViewInputText) },
                enabled = tradingViewInputText.isNotBlank()
            ) {
                Text(stringResource(Res.string.time_optimizer_copy_tradingview))
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
        if (sortedDailyPreviewRows.isNotEmpty()) {
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

        if (sortedDailyPreviewRows.isEmpty()) {
            Text(
                stringResource(Res.string.time_optimizer_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state = previewListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(sortedDailyPreviewRows, key = { it.first.toString() }) { (date, cityRowsForDay) ->
                    val dayAstroInstants = cityRowsForDay
                        .flatMap { it.second.astroInstants }
                        .distinct()
                        .sorted()
                    val dayAstroSummary = if (dayAstroInstants.isEmpty()) {
                        stringResource(Res.string.time_optimizer_astro_none)
                    } else {
                        val first = dayAstroInstants.first().atZone(displayZone).toLocalTime().format(timeFmt)
                        val last = dayAstroInstants.last().atZone(displayZone).toLocalTime().format(timeFmt)
                        stringResource(Res.string.time_optimizer_astro_summary, dayAstroInstants.size, first, last)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${stringResource(Res.string.time_optimizer_col_date)}: $date",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Lokal (${displayZone.id}) | UTC",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (optimizerState.includeAstro) {
                            Text(
                                "Astro: $dayAstroSummary",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.positive
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cityRowsForDay.forEach { (city, row) ->
                                fun formatDual(value: java.time.ZonedDateTime?): String {
                                    if (value == null) return dash
                                    val instant = value.toInstant()
                                    val local = instant.atZone(displayZone).format(timeFmt)
                                    val utc = instant.atZone(ZoneId.of("UTC")).format(timeFmt)
                                    return "$local | $utc"
                                }

                                val sunRiseDual = formatDual(row.sunrise)
                                val sunSetDual = formatDual(row.sunset)
                                val moonRiseDual = formatDual(row.moonrise)
                                val moonSetDual = formatDual(row.moonset)

                                Column(
                                    modifier = Modifier
                                        .widthIn(min = 210.dp)
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        city.label,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (optimizerState.includeSun) {
                                        Text(
                                            "Sonne \u2191 $sunRiseDual",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "Sonne \u2193 $sunSetDual",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (optimizerState.includeMoon) {
                                        Text(
                                            "Mond \u2191 $moonRiseDual",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "Mond \u2193 $moonSetDual",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(ext.shellDivider)
                    )
                }
            }
        }
    }
}

private fun buildMonthlyExportText(
    month: YearMonth,
    userZone: ZoneId,
    cityRows: List<Pair<City, List<TimeOptimizerDayRow>>>,
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

private fun buildTradingViewInputText(
    month: YearMonth,
    userZone: ZoneId,
    cityRows: List<Pair<City, List<TimeOptimizerDayRow>>>,
    useUtcTimes: Boolean,
    includeSun: Boolean,
    includeMoon: Boolean,
    includeAstro: Boolean,
    dateTimeFmt: DateTimeFormatter
): String {
    if (cityRows.isEmpty() || (!includeSun && !includeMoon && !includeAstro)) return ""

    data class TradingViewEvent(
        val instant: java.time.Instant,
        val icon: String,
        val cityLabel: String
    )

    val exportZone = if (useUtcTimes) ZoneId.of("UTC") else userZone
    val events = mutableListOf<TradingViewEvent>()
    val astroInstants = linkedSetOf<java.time.Instant>()

    cityRows.forEach { (city, rows) ->
        rows.forEach { row ->
            if (includeSun) {
                row.sunrise?.toInstant()?.let { events += TradingViewEvent(it, "SUN_UP", city.label) }
                row.sunset?.toInstant()?.let { events += TradingViewEvent(it, "SUN_DOWN", city.label) }
            }
            if (includeMoon) {
                row.moonrise?.toInstant()?.let { events += TradingViewEvent(it, "MOON_UP", city.label) }
                row.moonset?.toInstant()?.let { events += TradingViewEvent(it, "MOON_DOWN", city.label) }
            }
            if (includeAstro) {
                row.astroInstants.forEach { instant ->
                    if (astroInstants.add(instant)) {
                        events += TradingViewEvent(instant, "ASTRO", "Astro")
                    }
                }
            }
        }
    }

    if (events.isEmpty()) return ""

    val sorted = events.sortedWith(compareBy<TradingViewEvent> { it.instant }.thenBy { it.cityLabel })
    return buildString {
        appendLine("# TradeBuddy TradingView Input")
        appendLine("# month=$month")
        appendLine("# timezone=${exportZone.id}")
        appendLine("# format=yyyy-MM-dd HH:mm|icon|city")
        sorted.forEach { event ->
            val dateTime = event.instant.atZone(exportZone).format(dateTimeFmt)
            appendLine("$dateTime|${event.icon}|${event.cityLabel}")
        }
    }.trim()
}
