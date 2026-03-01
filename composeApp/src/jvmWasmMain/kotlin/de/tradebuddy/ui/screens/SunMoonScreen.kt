package de.tradebuddy.ui.screens

import de.tradebuddy.presentation.SunMoonTab
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.CompactTimelineCard
import de.tradebuddy.ui.components.MonthlyTrendCard
import de.tradebuddy.ui.components.SnowTabItem
import de.tradebuddy.ui.components.SnowTabs
import de.tradebuddy.ui.components.StatisticsCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.datepicker_cancel
import trade_buddy.composeapp.generated.resources.datepicker_ok
import trade_buddy.composeapp.generated.resources.format_date_short
import trade_buddy.composeapp.generated.resources.format_time_short

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunMoonScreen(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    val userZone = state.userZone
    val datePattern = stringResource(Res.string.format_date_short)
    val timePattern = stringResource(Res.string.format_time_short)
    val dateFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern, Locale.GERMANY) }
    val timeFmt = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern, Locale.GERMANY) }
    val isToday = state.selectedDate == LocalDate.now(userZone)
    var showLoadingIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            showLoadingIndicator = false
            delay(180)
            showLoadingIndicator = true
        } else {
            showLoadingIndicator = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showLoadingIndicator && state.isLoading) {
            val progress = state.loadingProgress
            if (progress != null) {
                val clamped = progress.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { clamped },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }

        SnowTabs(
            items = SunMoonTab.entries.map { tab ->
                SnowTabItem(id = tab.name, label = stringResource(tab.label))
            },
            selectedId = state.selectedTab.name,
            onSelect = { id ->
                SunMoonTab.entries.firstOrNull { it.name == id }?.let(viewModel::setTab)
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (state.selectedTab) {
                SunMoonTab.Compact -> {
                    CompactTimelineCard(
                        selectedDate = state.selectedDate,
                        userZone = userZone,
                        timeFmt = timeFmt,
                        events = state.compactEvents,
                        stats = state.stats,
                        onUpsertStatEntry = viewModel::upsertStatEntry,
                        onDeleteStatEntry = viewModel::deleteStatEntryForEvent,
                        showUtcTime = state.showUtcTime,
                        showAzimuth = state.showAzimuth,
                        isToday = isToday,
                        nowInstant = state.nowInstant,
                        onShiftDate = viewModel::shiftDate,
                        onGoToToday = viewModel::goToToday,
                        onPickDate = { viewModel.showDatePicker(true) }
                    )
                }

                SunMoonTab.Statistics -> {
                    StatisticsCard(
                        selectedDate = state.selectedDate,
                        userZone = userZone,
                        stats = state.stats,
                        onReset = viewModel::resetStatistics,
                        onDeleteEntry = viewModel::deleteStatEntry
                    )
                }

                SunMoonTab.Trend -> {
                    MonthlyTrendCard(
                        state = state.trend,
                        onPrevMonth = { viewModel.shiftMonth(-1) },
                        onNextMonth = { viewModel.shiftMonth(1) }
                    )
                }

                SunMoonTab.Astro -> {
                    AstroCalendarScreen(
                        state = state,
                        viewModel = viewModel
                    )
                }

                SunMoonTab.Export -> {
                    TimeOptimizerScreen(
                        state = state,
                        viewModel = viewModel,
                        showBackToSettings = false
                    )
                }
            }
        }
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.atStartOfDay(userZone).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(userZone).toLocalDate()
                        viewModel.setDate(picked)
                    }
                    viewModel.showDatePicker(false)
                }) { Text(stringResource(Res.string.datepicker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDatePicker(false) }) {
                    Text(stringResource(Res.string.datepicker_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
