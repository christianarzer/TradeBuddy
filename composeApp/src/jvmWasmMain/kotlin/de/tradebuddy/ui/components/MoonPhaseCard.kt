package de.tradebuddy.ui.components

import de.tradebuddy.domain.model.MoonPhaseEvent
import de.tradebuddy.domain.model.MoonPhaseType
import de.tradebuddy.presentation.MoonPhaseUiState
import de.tradebuddy.ui.components.shared.SnowToolbar
import de.tradebuddy.ui.components.shared.SnowToolbarIconButton
import de.tradebuddy.ui.components.shared.SnowToolbarPopupPanel
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.extended
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_next_month
import trade_buddy.composeapp.generated.resources.action_prev_month
import trade_buddy.composeapp.generated.resources.compact_col_done
import trade_buddy.composeapp.generated.resources.compact_col_local
import trade_buddy.composeapp.generated.resources.compact_col_utc
import trade_buddy.composeapp.generated.resources.compact_filter_all
import trade_buddy.composeapp.generated.resources.content_done
import trade_buddy.composeapp.generated.resources.format_date_short
import trade_buddy.composeapp.generated.resources.format_month_year
import trade_buddy.composeapp.generated.resources.format_time_short
import trade_buddy.composeapp.generated.resources.moon_col_phase
import trade_buddy.composeapp.generated.resources.moon_phase_datetime
import trade_buddy.composeapp.generated.resources.moon_phase_empty
import trade_buddy.composeapp.generated.resources.moon_phase_empty_filtered
import trade_buddy.composeapp.generated.resources.moon_phase_first
import trade_buddy.composeapp.generated.resources.moon_phase_full
import trade_buddy.composeapp.generated.resources.moon_phase_last
import trade_buddy.composeapp.generated.resources.moon_phase_new
import trade_buddy.composeapp.generated.resources.moon_phase_sort_phase
import trade_buddy.composeapp.generated.resources.stats_filter_sort
import trade_buddy.composeapp.generated.resources.stats_filter_title
import trade_buddy.composeapp.generated.resources.stats_sort_newest
import trade_buddy.composeapp.generated.resources.stats_sort_oldest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoonPhaseCard(
    state: MoonPhaseUiState,
    userZone: ZoneId,
    nowInstant: Instant,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val ext = MaterialTheme.extended
    val datePattern = stringResource(Res.string.format_date_short)
    val timePattern = stringResource(Res.string.format_time_short)
    val monthPattern = stringResource(Res.string.format_month_year)
    val dateFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern, Locale.GERMANY) }
    val timeFmt = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern, Locale.GERMANY) }
    val monthLabel = remember(state.month, monthPattern) {
        state.month.atDay(1).format(DateTimeFormatter.ofPattern(monthPattern, Locale.GERMANY))
    }
    val currentMonth = remember(nowInstant, userZone) { YearMonth.from(nowInstant.atZone(userZone)) }
    val markPassed = currentMonth == state.month

    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(MoonPhaseSortMode.TimeAsc) }
    var selectedTypes by remember { mutableStateOf(MoonPhaseType.entries.toSet()) }

    val visiblePhases = remember(state.phases, selectedTypes, sortMode) {
        val filtered = state.phases.filter { phase -> phase.type in selectedTypes }
        when (sortMode) {
            MoonPhaseSortMode.TimeAsc -> filtered.sortedBy { it.instant }
            MoonPhaseSortMode.TimeDesc -> filtered.sortedByDescending { it.instant }
            MoonPhaseSortMode.Phase -> filtered.sortedWith(
                compareBy<MoonPhaseEvent> { it.type.ordinal }.thenBy { it.instant }
            )
        }
    }
    val hasFilteredOutItems = state.phases.isNotEmpty() && visiblePhases.isEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        if (state.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

        if (!state.isLoading && state.error == null) {
            SnowToolbar {
                Box {
                    SnowToolbarIconButton(
                        icon = SnowIcons.Filter,
                        onClick = { showFilterPopup = true }
                    )
                    DropdownMenu(
                        expanded = showFilterPopup,
                        onDismissRequest = { showFilterPopup = false }
                    ) {
                        SnowToolbarPopupPanel {
                            Text(
                                text = stringResource(Res.string.stats_filter_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(Res.string.moon_col_phase),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                            ) {
                                FilterChip(
                                    selected = selectedTypes.size == MoonPhaseType.entries.size,
                                    onClick = { selectedTypes = MoonPhaseType.entries.toSet() },
                                    label = { Text(stringResource(Res.string.compact_filter_all)) }
                                )
                                MoonPhaseType.entries.forEach { type ->
                                    val selected = type in selectedTypes
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            val updated = selectedTypes.toMutableSet()
                                            if (selected) {
                                                updated.remove(type)
                                            } else {
                                                updated.add(type)
                                            }
                                            selectedTypes = updated
                                        },
                                        label = { Text(phaseLabel(type)) },
                                        leadingIcon = {
                                            MoonPhaseIcon(type = type, passed = false, size = 12.dp)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Box {
                    SnowToolbarIconButton(
                        icon = SnowIcons.Sort,
                        onClick = { showSortPopup = true }
                    )
                    DropdownMenu(
                        expanded = showSortPopup,
                        onDismissRequest = { showSortPopup = false }
                    ) {
                        SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 220.dp, max = 300.dp)) {
                            Text(
                                text = stringResource(Res.string.stats_filter_sort),
                                style = MaterialTheme.typography.titleSmall
                            )
                            MoonPhaseSortMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = sortMode == mode,
                                    onClick = {
                                        sortMode = mode
                                        showSortPopup = false
                                    },
                                    label = { Text(stringResource(mode.label)) }
                                )
                            }
                        }
                    }
                }
                SnowToolbarIconButton(
                    icon = SnowIcons.ArrowLeft,
                    onClick = onPrevMonth,
                    contentDescription = stringResource(Res.string.action_prev_month)
                )
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleSmall
                )
                SnowToolbarIconButton(
                    icon = SnowIcons.ArrowRight,
                    onClick = onNextMonth,
                    contentDescription = stringResource(Res.string.action_next_month)
                )
                Spacer(Modifier.weight(1f))
            }

            if (visiblePhases.isEmpty()) {
                Text(
                    if (hasFilteredOutItems) {
                        stringResource(Res.string.moon_phase_empty_filtered, state.phases.size)
                    } else {
                        stringResource(Res.string.moon_phase_empty)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val scroll = rememberScrollState()
                val localWidth = 150.dp
                val utcWidth = 150.dp
                val phaseWidth = 220.dp
                val doneWidth = 96.dp

                Row(
                    modifier = Modifier.horizontalScroll(scroll),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.compact_col_local),
                        modifier = Modifier.width(localWidth),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        stringResource(Res.string.compact_col_utc),
                        modifier = Modifier.width(utcWidth),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        stringResource(Res.string.moon_col_phase),
                        modifier = Modifier.width(phaseWidth),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        stringResource(Res.string.compact_col_done),
                        modifier = Modifier.width(doneWidth),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(visiblePhases, key = { it.instant }) { phase ->
                        val passed = markPassed && phase.instant.isBefore(nowInstant)
                        MoonPhaseRow(
                            modifier = Modifier.horizontalScroll(scroll),
                            phase = phase,
                            dateFmt = dateFmt,
                            timeFmt = timeFmt,
                            passed = passed,
                            localWidth = localWidth,
                            utcWidth = utcWidth,
                            phaseWidth = phaseWidth,
                            doneWidth = doneWidth
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(ext.shellDivider)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoonPhaseRow(
    modifier: Modifier,
    phase: MoonPhaseEvent,
    dateFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter,
    passed: Boolean,
    localWidth: Dp,
    utcWidth: Dp,
    phaseWidth: Dp,
    doneWidth: Dp
) {
    val textColor = if (passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val localLabel = stringResource(
        Res.string.moon_phase_datetime,
        phase.time.format(dateFmt),
        phase.time.format(timeFmt)
    )
    val utcLabel = stringResource(
        Res.string.moon_phase_datetime,
        phase.utcTime.format(dateFmt),
        phase.utcTime.format(timeFmt)
    )

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            localLabel,
            modifier = Modifier.width(localWidth),
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        Text(
            utcLabel,
            modifier = Modifier.width(utcWidth),
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        Row(
            modifier = Modifier.width(phaseWidth),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MoonPhaseIcon(
                type = phase.type,
                passed = passed,
                size = 14.dp
            )
            Text(
                phaseLabel(phase.type),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }

        Box(modifier = Modifier.width(doneWidth), contentAlignment = Alignment.Center) {
            if (passed) {
                Icon(
                    imageVector = SnowIcons.Check,
                    contentDescription = stringResource(Res.string.content_done),
                    tint = MaterialTheme.colorScheme.secondary
                )
            } else {
                Spacer(Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MoonPhaseIcon(
    type: MoonPhaseType,
    passed: Boolean,
    size: Dp
) {
    val litColor = if (passed) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        phaseColor(type)
    }
    val shadowColor = if (passed) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (passed) 0.5f else 1f)

    Canvas(Modifier.size(size)) {
        drawCircle(shadowColor)
        when (type) {
            MoonPhaseType.NewMoon -> Unit
            MoonPhaseType.FullMoon -> drawCircle(litColor)
            MoonPhaseType.FirstQuarter -> drawArc(
                color = litColor,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = true
            )
            MoonPhaseType.LastQuarter -> drawArc(
                color = litColor,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true
            )
        }
        drawCircle(outline, style = Stroke(width = 1.1f))
    }
}

@Composable
private fun phaseColor(type: MoonPhaseType): Color {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        MoonPhaseType.NewMoon -> scheme.onSurfaceVariant
        MoonPhaseType.FirstQuarter -> scheme.tertiary
        MoonPhaseType.FullMoon -> scheme.primary
        MoonPhaseType.LastQuarter -> scheme.secondary
    }
}

@Composable
private fun phaseLabel(type: MoonPhaseType): String = when (type) {
    MoonPhaseType.NewMoon -> stringResource(Res.string.moon_phase_new)
    MoonPhaseType.FirstQuarter -> stringResource(Res.string.moon_phase_first)
    MoonPhaseType.FullMoon -> stringResource(Res.string.moon_phase_full)
    MoonPhaseType.LastQuarter -> stringResource(Res.string.moon_phase_last)
}

private enum class MoonPhaseSortMode(val label: StringResource) {
    TimeAsc(Res.string.stats_sort_oldest),
    TimeDesc(Res.string.stats_sort_newest),
    Phase(Res.string.moon_phase_sort_phase);

    fun next(): MoonPhaseSortMode = when (this) {
        TimeAsc -> TimeDesc
        TimeDesc -> Phase
        Phase -> TimeAsc
    }
}
