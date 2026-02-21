package de.tradebuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.model.SunMoonTimes
import de.tradebuddy.presentation.SunMoonTab
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.CityCard
import de.tradebuddy.ui.components.CompactTimelineCard
import de.tradebuddy.ui.components.StatisticsCard
import de.tradebuddy.ui.components.MonthlyTrendCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_next_day
import trade_buddy.composeapp.generated.resources.action_prev_day
import trade_buddy.composeapp.generated.resources.action_pick_date
import trade_buddy.composeapp.generated.resources.action_today
import trade_buddy.composeapp.generated.resources.datepicker_cancel
import trade_buddy.composeapp.generated.resources.datepicker_ok
import trade_buddy.composeapp.generated.resources.format_date_short
import trade_buddy.composeapp.generated.resources.format_time_short
import trade_buddy.composeapp.generated.resources.header_active_cities
import trade_buddy.composeapp.generated.resources.header_sunmoon_subtitle
import trade_buddy.composeapp.generated.resources.header_sunmoon_title
import trade_buddy.composeapp.generated.resources.label_date
import trade_buddy.composeapp.generated.resources.label_timezone

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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SunMoonHeaderCard(
            selectedDate = state.selectedDate,
            userZone = userZone,
            dateFmt = dateFmt,
            activeCities = state.selectedCityKeys.size,
            totalCities = state.allCities.size,
            onShiftDate = viewModel::shiftDate,
            onGoToToday = viewModel::goToToday,
            onPickDate = { viewModel.showDatePicker(true) }
        )

        if (state.isLoading) {
            val progress = state.loadingProgress
            if (progress != null) {
                val clamped = progress.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { clamped },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(clamped * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }

        PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
            SunMoonTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.setTab(tab) },
                    text = { Text(stringResource(tab.label)) }
                )
            }
        }

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
                        nowInstant = state.nowInstant
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

                SunMoonTab.Details -> {
                    CityCardList(
                        timeFmt = timeFmt,
                        userZone = userZone,
                        results = state.filteredResults,
                        sunTimeOffsetMinutes = state.sunTimeOffsetMinutes,
                        moonTimeOffsetMinutes = state.moonTimeOffsetMinutes,
                        showSun = state.showSun,
                        showMoon = state.showMoon,
                        showRise = state.showRise,
                        showSet = state.showSet
                    )
                }

                SunMoonTab.Trend -> {
                    MonthlyTrendCard(
                        state = state.trend,
                        highlightDate = state.selectedDate,
                        onPrevMonth = { viewModel.shiftMonth(-1) },
                        onNextMonth = { viewModel.shiftMonth(1) }
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

@Composable
private fun SunMoonHeaderCard(
    selectedDate: LocalDate,
    userZone: ZoneId,
    dateFmt: DateTimeFormatter,
    activeCities: Int,
    totalCities: Int,
    onShiftDate: (Long) -> Unit,
    onGoToToday: () -> Unit,
    onPickDate: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(Res.string.header_sunmoon_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(Res.string.header_sunmoon_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = selectedDate.format(dateFmt),
                    onValueChange = { },
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    label = { Text(stringResource(Res.string.label_date)) },
                    trailingIcon = {
                        IconButton(onClick = onPickDate) {
                            Icon(
                                Icons.Outlined.DateRange,
                                contentDescription = stringResource(Res.string.action_pick_date)
                            )
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onShiftDate(-1) },
                    label = { Text(stringResource(Res.string.action_prev_day)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) }
                )
                AssistChip(
                    onClick = onGoToToday,
                    label = { Text(stringResource(Res.string.action_today)) },
                    leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onShiftDate(1) },
                    label = { Text(stringResource(Res.string.action_next_day)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.label_timezone, userZone.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(Res.string.header_active_cities, activeCities, totalCities),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CityCardList(
    timeFmt: DateTimeFormatter,
    userZone: ZoneId,
    results: List<SunMoonTimes>,
    sunTimeOffsetMinutes: Int,
    moonTimeOffsetMinutes: Int,
    showSun: Boolean,
    showMoon: Boolean,
    showRise: Boolean,
    showSet: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results) { r ->
            CityCard(
                r = r,
                userZone = userZone,
                timeFmt = timeFmt,
                sunTimeOffsetMinutes = sunTimeOffsetMinutes,
                moonTimeOffsetMinutes = moonTimeOffsetMinutes,
                showSun = showSun,
                showMoon = showMoon,
                showRise = showRise,
                showSet = showSet
            )
        }
    }
}

