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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.util.key
import de.tradebuddy.presentation.AppScreen
import de.tradebuddy.presentation.TimeOptimizerDayRow
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.rememberCopyTextToClipboard
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.format_time_short
import trade_buddy.composeapp.generated.resources.time_optimizer_astro_none
import trade_buddy.composeapp.generated.resources.time_optimizer_astro_summary
import trade_buddy.composeapp.generated.resources.time_optimizer_back
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
    val exportText = remember(optimizerState.month, selectedCity?.label, displayZone.id, optimizerState.rows, timeFmt) {
        buildMonthlyExportText(
            month = optimizerState.month,
            cityLabel = selectedCity?.label,
            zone = displayZone,
            rows = optimizerState.rows,
            timeFmt = timeFmt
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { viewModel.shiftTimeOptimizerMonth(-1) }) {
                        Text(stringResource(Res.string.time_optimizer_month_prev))
                    }
                    OutlinedButton(onClick = { viewModel.shiftTimeOptimizerMonth(1) }) {
                        Text(stringResource(Res.string.time_optimizer_month_next))
                    }
                    Text(
                        text = optimizerState.month.toString(),
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = viewModel::refreshTimeOptimizerMonth) {
                        Text(stringResource(Res.string.time_optimizer_recalculate))
                    }
                    OutlinedButton(
                        onClick = { copyToClipboard(exportText) },
                        enabled = optimizerState.rows.isNotEmpty()
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

        if (optimizerState.rows.isEmpty()) {
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
                items(optimizerState.rows, key = { it.date.toString() }) { row ->
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

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
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
                                style = MaterialTheme.typography.bodySmall
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
    cityLabel: String?,
    zone: ZoneId,
    rows: List<TimeOptimizerDayRow>,
    timeFmt: DateTimeFormatter
): String {
    fun formatZoned(value: java.time.ZonedDateTime?): String =
        value?.toInstant()?.atZone(zone)?.format(timeFmt) ?: "-"

    fun formatAstro(instants: List<java.time.Instant>): String =
        if (instants.isEmpty()) {
            "-"
        } else {
            instants.joinToString("|") { instant ->
                instant.atZone(zone).toLocalTime().format(timeFmt)
            }
        }

    return buildString {
        appendLine("# TradeBuddy Monthly Export")
        appendLine("month,$month")
        appendLine("city,${cityLabel ?: "-"}")
        appendLine("zone,${zone.id}")
        appendLine("date,sunrise,sunset,moonrise,moonset,astro_times")
        rows.forEach { row ->
            appendLine(
                "${row.date}," +
                    "${formatZoned(row.sunrise)}," +
                    "${formatZoned(row.sunset)}," +
                    "${formatZoned(row.moonrise)}," +
                    "${formatZoned(row.moonset)}," +
                    formatAstro(row.astroInstants)
            )
        }
    }.trim()
}
