package de.tradebuddy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.MoveDirection
import de.tradebuddy.domain.model.StatEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.datepicker_cancel
import trade_buddy.composeapp.generated.resources.event_moonrise
import trade_buddy.composeapp.generated.resources.event_moonset
import trade_buddy.composeapp.generated.resources.event_sunrise
import trade_buddy.composeapp.generated.resources.event_sunset
import trade_buddy.composeapp.generated.resources.format_date_short
import trade_buddy.composeapp.generated.resources.format_time_short
import trade_buddy.composeapp.generated.resources.stats_delete_entry
import trade_buddy.composeapp.generated.resources.stats_direction_down
import trade_buddy.composeapp.generated.resources.stats_direction_up
import trade_buddy.composeapp.generated.resources.stats_empty
import trade_buddy.composeapp.generated.resources.stats_empty_filtered
import trade_buddy.composeapp.generated.resources.stats_filter_all
import trade_buddy.composeapp.generated.resources.stats_filter_clear
import trade_buddy.composeapp.generated.resources.stats_filter_direction
import trade_buddy.composeapp.generated.resources.stats_filter_event
import trade_buddy.composeapp.generated.resources.stats_filter_range
import trade_buddy.composeapp.generated.resources.stats_filter_search
import trade_buddy.composeapp.generated.resources.stats_filter_sort
import trade_buddy.composeapp.generated.resources.stats_filter_title
import trade_buddy.composeapp.generated.resources.stats_list_title
import trade_buddy.composeapp.generated.resources.stats_range_30d
import trade_buddy.composeapp.generated.resources.stats_range_7d
import trade_buddy.composeapp.generated.resources.stats_range_90d
import trade_buddy.composeapp.generated.resources.stats_reset
import trade_buddy.composeapp.generated.resources.stats_reset_confirm_action
import trade_buddy.composeapp.generated.resources.stats_reset_confirm_body
import trade_buddy.composeapp.generated.resources.stats_reset_confirm_title
import trade_buddy.composeapp.generated.resources.stats_sort_newest
import trade_buddy.composeapp.generated.resources.stats_sort_offset
import trade_buddy.composeapp.generated.resources.stats_sort_oldest
import trade_buddy.composeapp.generated.resources.stats_section_trend
import trade_buddy.composeapp.generated.resources.stats_subtitle
import trade_buddy.composeapp.generated.resources.stats_summary_offset
import trade_buddy.composeapp.generated.resources.stats_summary_total
import trade_buddy.composeapp.generated.resources.stats_summary_up
import trade_buddy.composeapp.generated.resources.stats_title
import trade_buddy.composeapp.generated.resources.stats_visible_count

@Composable
fun StatisticsCard(
    selectedDate: LocalDate,
    userZone: ZoneId,
    stats: List<StatEntry>,
    onReset: () -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    val datePattern = stringResource(Res.string.format_date_short)
    val timePattern = stringResource(Res.string.format_time_short)
    val dateFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern, Locale.GERMANY) }
    val timeFmt = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern, Locale.GERMANY) }

    var query by rememberSaveable { mutableStateOf("") }
    var directionFilter by rememberSaveable { mutableStateOf<MoveDirection?>(null) }
    var eventFilter by rememberSaveable { mutableStateOf<CompactEventType?>(null) }
    var rangeFilter by rememberSaveable { mutableStateOf(StatsRange.All) }
    var sortMode by rememberSaveable { mutableStateOf(StatsSort.Newest) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val sunriseLabel = stringResource(Res.string.event_sunrise)
    val sunsetLabel = stringResource(Res.string.event_sunset)
    val moonriseLabel = stringResource(Res.string.event_moonrise)
    val moonsetLabel = stringResource(Res.string.event_moonset)
    val eventLabels = remember(sunriseLabel, sunsetLabel, moonriseLabel, moonsetLabel) {
        mapOf(
            CompactEventType.Sunrise to sunriseLabel,
            CompactEventType.Sunset to sunsetLabel,
            CompactEventType.Moonrise to moonriseLabel,
            CompactEventType.Moonset to moonsetLabel
        )
    }

    val nowInstant = Instant.now()
    val cutoff = rangeFilter.cutoff(nowInstant)
    val normalizedQuery = query.trim().lowercase()
    val filtered = remember(
        stats,
        normalizedQuery,
        directionFilter,
        eventFilter,
        rangeFilter,
        sortMode,
        cutoff
    ) {
        stats.asSequence()
            .filter { entry ->
                if (normalizedQuery.isBlank()) return@filter true
                val eventLabel = eventLabels[entry.eventType].orEmpty()
                entry.cityLabel.lowercase().contains(normalizedQuery) ||
                    eventLabel.lowercase().contains(normalizedQuery)
            }
            .filter { entry -> directionFilter?.let { entry.direction == it } ?: true }
            .filter { entry -> eventFilter?.let { entry.eventType == it } ?: true }
            .filter { entry -> cutoff?.let { entry.eventInstant.isAfter(it) } ?: true }
            .toList()
    }
    val visibleStats = remember(filtered, sortMode) {
        when (sortMode) {
            StatsSort.Newest -> filtered.sortedByDescending { it.eventInstant }
            StatsSort.Oldest -> filtered.sortedBy { it.eventInstant }
            StatsSort.Offset -> filtered.sortedByDescending { abs(it.offsetMinutes) }
        }
    }

    val total = visibleStats.size
    val upCount = visibleStats.count { it.direction == MoveDirection.Up }
    val downCount = total - upCount
    val upPct = if (total == 0) 0 else (upCount.toDouble() / total * 100).roundToInt()
    val avgOffset = if (total == 0) 0 else visibleStats.map { it.offsetMinutes }.average().roundToInt()

    ElevatedCard(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(Res.string.stats_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(Res.string.stats_subtitle, selectedDate.format(dateFmt), userZone.id),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (stats.isNotEmpty()) {
                            Text(
                                stringResource(Res.string.stats_visible_count, total, stats.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        enabled = stats.isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.stats_reset))
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatMetric(
                        label = stringResource(Res.string.stats_summary_total),
                        value = total.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = stringResource(Res.string.stats_summary_up),
                        value = if (total == 0) "-" else "${upPct}%",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetric(
                        label = stringResource(Res.string.stats_summary_offset),
                        value = if (total == 0) "-" else "${avgOffset}m",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (total > 0) {
                item {
                    TrendSection(
                        upCount = upCount,
                        downCount = downCount
                    )
                }
            }

            item {
                StatsFilterPanel(
                    query = query,
                    onQueryChange = { query = it },
                    directionFilter = directionFilter,
                    onDirectionChange = { directionFilter = it },
                    eventFilter = eventFilter,
                    onEventChange = { eventFilter = it },
                    rangeFilter = rangeFilter,
                    onRangeChange = { rangeFilter = it },
                    sortMode = sortMode,
                    onSortChange = { sortMode = it },
                    onClear = {
                        query = ""
                        directionFilter = null
                        eventFilter = null
                        rangeFilter = StatsRange.All
                        sortMode = StatsSort.Newest
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.stats_list_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.weight(1f))
                    if (stats.isNotEmpty()) {
                        Text(
                            "${visibleStats.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (stats.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.stats_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (visibleStats.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.stats_empty_filtered),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(visibleStats, key = { it.id }) { entry ->
                    StatEntryRow(
                        entry = entry,
                        userZone = userZone,
                        dateFmt = dateFmt,
                        timeFmt = timeFmt,
                        eventLabel = eventLabels[entry.eventType].orEmpty(),
                        onDelete = { onDeleteEntry(entry.id) }
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(Res.string.stats_reset_confirm_title)) },
            text = { Text(stringResource(Res.string.stats_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onReset()
                }) {
                    Text(stringResource(Res.string.stats_reset_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(Res.string.datepicker_cancel))
                }
            }
        )
    }
}

@Composable
private fun TrendSection(
    upCount: Int,
    downCount: Int
) {
    val total = upCount + downCount
    val upRatio = if (total == 0) 0f else upCount.toFloat() / total
    val upColor = MaterialTheme.colorScheme.primary
    val downColor = MaterialTheme.colorScheme.tertiary

    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(Res.string.stats_section_trend), style = MaterialTheme.typography.titleSmall)
            TrendRatioBar(upRatio = upRatio, upColor = upColor, downColor = downColor)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TrendLegend(
                    label = stringResource(Res.string.stats_direction_up),
                    count = upCount,
                    color = upColor
                )
                TrendLegend(
                    label = stringResource(Res.string.stats_direction_down),
                    count = downCount,
                    color = downColor
                )
            }
        }
    }
}

@Composable
private fun StatsFilterPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    directionFilter: MoveDirection?,
    onDirectionChange: (MoveDirection?) -> Unit,
    eventFilter: CompactEventType?,
    onEventChange: (CompactEventType?) -> Unit,
    rangeFilter: StatsRange,
    onRangeChange: (StatsRange) -> Unit,
    sortMode: StatsSort,
    onSortChange: (StatsSort) -> Unit,
    onClear: () -> Unit
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.stats_filter_title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) {
                    Text(stringResource(Res.string.stats_filter_clear))
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
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
                label = { Text(stringResource(Res.string.stats_filter_search)) }
            )

            FilterRow(label = stringResource(Res.string.stats_filter_direction)) {
                FilterChip(
                    selected = directionFilter == null,
                    onClick = { onDirectionChange(null) },
                    label = { Text(stringResource(Res.string.stats_filter_all)) }
                )
                FilterChip(
                    selected = directionFilter == MoveDirection.Up,
                    onClick = { onDirectionChange(MoveDirection.Up) },
                    label = { Text(stringResource(Res.string.stats_direction_up)) }
                )
                FilterChip(
                    selected = directionFilter == MoveDirection.Down,
                    onClick = { onDirectionChange(MoveDirection.Down) },
                    label = { Text(stringResource(Res.string.stats_direction_down)) }
                )
            }

            FilterRow(label = stringResource(Res.string.stats_filter_event)) {
                FilterChip(
                    selected = eventFilter == null,
                    onClick = { onEventChange(null) },
                    label = { Text(stringResource(Res.string.stats_filter_all)) }
                )
                FilterChip(
                    selected = eventFilter == CompactEventType.Sunrise,
                    onClick = { onEventChange(CompactEventType.Sunrise) },
                    label = { Text(stringResource(Res.string.event_sunrise)) }
                )
                FilterChip(
                    selected = eventFilter == CompactEventType.Sunset,
                    onClick = { onEventChange(CompactEventType.Sunset) },
                    label = { Text(stringResource(Res.string.event_sunset)) }
                )
                FilterChip(
                    selected = eventFilter == CompactEventType.Moonrise,
                    onClick = { onEventChange(CompactEventType.Moonrise) },
                    label = { Text(stringResource(Res.string.event_moonrise)) }
                )
                FilterChip(
                    selected = eventFilter == CompactEventType.Moonset,
                    onClick = { onEventChange(CompactEventType.Moonset) },
                    label = { Text(stringResource(Res.string.event_moonset)) }
                )
            }

            FilterRow(label = stringResource(Res.string.stats_filter_range)) {
                StatsRange.entries.forEach { range ->
                    FilterChip(
                        selected = rangeFilter == range,
                        onClick = { onRangeChange(range) },
                        label = { Text(stringResource(range.label)) }
                    )
                }
            }

            FilterRow(label = stringResource(Res.string.stats_filter_sort)) {
                StatsSort.entries.forEach { sort ->
                    FilterChip(
                        selected = sortMode == sort,
                        onClick = { onSortChange(sort) },
                        label = { Text(stringResource(sort.label)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun StatEntryRow(
    entry: StatEntry,
    userZone: ZoneId,
    dateFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter,
    eventLabel: String,
    onDelete: () -> Unit
) {
    val localTime = entry.eventInstant.atZone(userZone)
    val dateText = localTime.format(dateFmt)
    val timeText = localTime.format(timeFmt)

    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${entry.cityLabel} · $eventLabel · $dateText $timeText",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DirectionBadge(direction = entry.direction)
            Text("${entry.offsetMinutes}m", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.stats_delete_entry),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DirectionBadge(direction: MoveDirection) {
    val (label, color, icon) = when (direction) {
        MoveDirection.Up -> Triple(
            stringResource(Res.string.stats_direction_up),
            MaterialTheme.colorScheme.primary,
            Icons.Outlined.ArrowUpward
        )
        MoveDirection.Down -> Triple(
            stringResource(Res.string.stats_direction_down),
            MaterialTheme.colorScheme.tertiary,
            Icons.Outlined.ArrowDownward
        )
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun TrendRatioBar(
    upRatio: Float,
    upColor: Color,
    downColor: Color
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        drawRoundRect(
            color = track,
            cornerRadius = CornerRadius(8f, 8f)
        )
        val upWidth = size.width * upRatio.coerceIn(0f, 1f)
        if (upWidth > 0f) {
            drawRoundRect(
                color = upColor,
                size = Size(upWidth, size.height),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
        val downWidth = size.width - upWidth
        if (downWidth > 0f) {
            drawRoundRect(
                color = downColor,
                topLeft = Offset(upWidth, 0f),
                size = Size(downWidth, size.height),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
private fun TrendLegend(
    label: String,
    count: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text("$label $count", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private enum class StatsRange(
    val label: StringResource,
    private val days: Long?
) {
    All(Res.string.stats_filter_all, null),
    Last7(Res.string.stats_range_7d, 7),
    Last30(Res.string.stats_range_30d, 30),
    Last90(Res.string.stats_range_90d, 90);

    fun cutoff(now: Instant): Instant? = days?.let { now.minus(Duration.ofDays(it)) }
}

private enum class StatsSort(val label: StringResource) {
    Newest(Res.string.stats_sort_newest),
    Oldest(Res.string.stats_sort_oldest),
    Offset(Res.string.stats_sort_offset);
}
