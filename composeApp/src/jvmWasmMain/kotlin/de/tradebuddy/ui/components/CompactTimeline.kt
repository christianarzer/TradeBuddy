package de.tradebuddy.ui.components

import de.tradebuddy.domain.model.CompactEvent
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.MoveDirection
import de.tradebuddy.domain.model.StatEntry
import de.tradebuddy.domain.util.azimuthToCardinalIndex
import de.tradebuddy.ui.components.shared.SnowToolbar
import de.tradebuddy.ui.components.shared.SnowToolbarIconButton
import de.tradebuddy.ui.components.shared.SnowToolbarPopupPanel
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.extended
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import trade_buddy.composeapp.generated.resources.action_today
import trade_buddy.composeapp.generated.resources.action_copy_times
import trade_buddy.composeapp.generated.resources.azimuth_value
import trade_buddy.composeapp.generated.resources.compact_col_azimuth
import trade_buddy.composeapp.generated.resources.compact_col_city
import trade_buddy.composeapp.generated.resources.compact_col_done
import trade_buddy.composeapp.generated.resources.compact_col_event
import trade_buddy.composeapp.generated.resources.compact_col_local
import trade_buddy.composeapp.generated.resources.compact_col_offset
import trade_buddy.composeapp.generated.resources.compact_col_shift
import trade_buddy.composeapp.generated.resources.compact_col_trend
import trade_buddy.composeapp.generated.resources.compact_col_utc
import trade_buddy.composeapp.generated.resources.compact_chip_city_tomorrow
import trade_buddy.composeapp.generated.resources.compact_chip_city_yesterday
import trade_buddy.composeapp.generated.resources.compact_empty
import trade_buddy.composeapp.generated.resources.compact_filter_all
import trade_buddy.composeapp.generated.resources.compact_filter_event_type
import trade_buddy.composeapp.generated.resources.compact_filter_upcoming_only
import trade_buddy.composeapp.generated.resources.compact_sort_by_city
import trade_buddy.composeapp.generated.resources.compact_sort_by_event
import trade_buddy.composeapp.generated.resources.compact_sort_by_time
import trade_buddy.composeapp.generated.resources.compact_title
import trade_buddy.composeapp.generated.resources.compact_trend_none
import trade_buddy.composeapp.generated.resources.content_done
import trade_buddy.composeapp.generated.resources.event_moonrise
import trade_buddy.composeapp.generated.resources.event_moonset
import trade_buddy.composeapp.generated.resources.event_sunrise
import trade_buddy.composeapp.generated.resources.event_sunset
import trade_buddy.composeapp.generated.resources.format_date_short
import trade_buddy.composeapp.generated.resources.format_datetime_copy
import trade_buddy.composeapp.generated.resources.label_azimuth
import trade_buddy.composeapp.generated.resources.stats_direction_down
import trade_buddy.composeapp.generated.resources.stats_direction_up
import trade_buddy.composeapp.generated.resources.stats_offset_label
import trade_buddy.composeapp.generated.resources.stats_filter_sort
import trade_buddy.composeapp.generated.resources.value_dash

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompactTimelineCard(
    selectedDate: LocalDate,
    userZone: ZoneId,
    timeFmt: DateTimeFormatter,
    events: List<CompactEvent>,
    stats: List<StatEntry>,
    onUpsertStatEntry: (CompactEvent, MoveDirection, Int) -> Unit,
    onDeleteStatEntry: (CompactEvent) -> Unit,
    showUtcTime: Boolean,
    showAzimuth: Boolean,
    isToday: Boolean,
    nowInstant: Instant,
    onShiftDate: (Long) -> Unit,
    onGoToToday: () -> Unit,
    onPickDate: () -> Unit
) {
    val ext = MaterialTheme.extended
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    var eventTypeFilter by rememberSaveable { mutableStateOf<CompactEventType?>(null) }
    var upcomingOnly by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(CompactSortMode.Time) }
    val visibleEvents = remember(events, eventTypeFilter, upcomingOnly, sortMode, isToday, nowInstant) {
        val base = events.asSequence()
            .filter { event -> eventTypeFilter?.let { event.eventType == it } ?: true }
            .filter { event ->
                if (!upcomingOnly || !isToday) true else event.userInstant?.isAfter(nowInstant) == true
            }
            .toList()
        when (sortMode) {
            CompactSortMode.Time -> base.sortedBy { it.userInstant ?: it.utcTime?.toInstant() ?: Instant.ofEpochMilli(0L) }
            CompactSortMode.City -> base.sortedBy { it.cityLabel }
            CompactSortMode.Event -> base.sortedBy { it.eventType.name }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
            val datePattern = stringResource(Res.string.format_date_short)
            val copyPattern = stringResource(Res.string.format_datetime_copy)
            val dateFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern, Locale.GERMANY) }
            val copyFmt = remember(copyPattern) { DateTimeFormatter.ofPattern(copyPattern, Locale.ROOT) }
            val copyToClipboard = rememberCopyTextToClipboard()
            val hasCopyTimes = visibleEvents.any { it.userTime != null }
            val statsByKey = remember(stats) { stats.associateBy(::statKey) }
            val drafts = remember { mutableStateMapOf<String, CompactTrendDraft>() }
            LaunchedEffect(stats) {
                if (stats.isEmpty()) {
                    drafts.clear()
                    return@LaunchedEffect
                }
                val statKeys = stats.map { statKey(it) }.toSet()
                stats.forEach { entry ->
                    val key = statKey(entry)
                    val draft = drafts[key]
                    val offset = entry.offsetMinutes.toString()
                    if (draft == null || draft.selection != entry.direction || draft.offsetText != offset) {
                        drafts[key] = CompactTrendDraft(entry.direction, offset)
                    }
                }
                val staleKeys = drafts.entries
                    .filter { (key, draft) -> key !in statKeys && draft.selection != null }
                    .map { it.key }
                staleKeys.forEach { key ->
                    drafts[key]?.let { draft ->
                        drafts[key] = draft.copy(selection = null)
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(Res.string.compact_title), style = MaterialTheme.typography.titleMedium)
            }

            SnowToolbar {
                Box {
                    SnowToolbarIconButton(icon = SnowIcons.Filter, onClick = { showFilterPopup = true })
                    DropdownMenu(
                        expanded = showFilterPopup,
                        onDismissRequest = { showFilterPopup = false }
                    ) {
                        SnowToolbarPopupPanel(modifier = Modifier.widthIn(max = 420.dp)) {
                            Text(
                                text = stringResource(Res.string.compact_filter_event_type),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = eventTypeFilter == null,
                                    onClick = { eventTypeFilter = null },
                                    label = { Text(stringResource(Res.string.compact_filter_all)) }
                                )
                                FilterChip(
                                    selected = eventTypeFilter == CompactEventType.Sunrise,
                                    onClick = { eventTypeFilter = CompactEventType.Sunrise },
                                    label = { Text(stringResource(Res.string.event_sunrise)) }
                                )
                                FilterChip(
                                    selected = eventTypeFilter == CompactEventType.Sunset,
                                    onClick = { eventTypeFilter = CompactEventType.Sunset },
                                    label = { Text(stringResource(Res.string.event_sunset)) }
                                )
                                FilterChip(
                                    selected = eventTypeFilter == CompactEventType.Moonrise,
                                    onClick = { eventTypeFilter = CompactEventType.Moonrise },
                                    label = { Text(stringResource(Res.string.event_moonrise)) }
                                )
                                FilterChip(
                                    selected = eventTypeFilter == CompactEventType.Moonset,
                                    onClick = { eventTypeFilter = CompactEventType.Moonset },
                                    label = { Text(stringResource(Res.string.event_moonset)) }
                                )
                            }
                            FilterChip(
                                selected = upcomingOnly,
                                onClick = { upcomingOnly = !upcomingOnly },
                                label = { Text(stringResource(Res.string.compact_filter_upcoming_only)) }
                            )
                        }
                    }
                }
                Box {
                    SnowToolbarIconButton(icon = SnowIcons.Sort, onClick = { showSortPopup = true })
                    DropdownMenu(
                        expanded = showSortPopup,
                        onDismissRequest = { showSortPopup = false }
                    ) {
                        SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 220.dp, max = 300.dp)) {
                            Text(
                                text = stringResource(Res.string.stats_filter_sort),
                                style = MaterialTheme.typography.titleSmall
                            )
                            CompactSortMode.entries.forEach { mode ->
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
                Row(
                    modifier = Modifier
                        .background(ext.toolbarSurface, MaterialTheme.shapes.small)
                        .clickable(onClick = onPickDate)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(dateFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                SnowToolbarIconButton(
                    icon = SnowIcons.ArrowLeft,
                    onClick = { onShiftDate(-1) },
                    contentDescription = stringResource(Res.string.action_prev_day)
                )
                SnowToolbarIconButton(
                    icon = SnowIcons.Calendar,
                    onClick = onGoToToday,
                    contentDescription = stringResource(Res.string.action_today)
                )
                SnowToolbarIconButton(
                    icon = SnowIcons.ArrowRight,
                    onClick = { onShiftDate(1) },
                    contentDescription = stringResource(Res.string.action_next_day)
                )
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val copyText = visibleEvents.mapNotNull { it.userTime?.format(copyFmt) }
                            .joinToString(",")
                        copyToClipboard(copyText)
                    },
                    enabled = hasCopyTimes
                ) {
                    Icon(
                        imageVector = SnowIcons.Copy,
                        contentDescription = stringResource(Res.string.action_copy_times),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.action_copy_times))
                }
            }

            val scroll = rememberScrollState()
            val timeWidth = 72.dp
            val utcWidth = 72.dp
            val cityWidth = 140.dp
            val eventWidth = 170.dp +
                if (showUtcTime) 0.dp else utcWidth +
                if (showAzimuth) 0.dp else 110.dp
            val chipsWidth = 190.dp
            val azWidth = 110.dp
            val doneWidth = 96.dp
            val trendWidth = 120.dp
            val offsetWidth = 96.dp

            Row(
                modifier = Modifier.horizontalScroll(scroll),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.compact_col_local),
                    modifier = Modifier.width(timeWidth),
                    style = MaterialTheme.typography.labelLarge
                )
                if (showUtcTime) {
                    Text(
                        stringResource(Res.string.compact_col_utc),
                        modifier = Modifier.width(utcWidth),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    stringResource(Res.string.compact_col_city),
                    modifier = Modifier.width(cityWidth),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(Res.string.compact_col_event),
                    modifier = Modifier.width(eventWidth),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(Res.string.compact_col_shift),
                    modifier = Modifier.width(chipsWidth),
                    style = MaterialTheme.typography.labelLarge
                )
                if (showAzimuth) {
                    Text(
                        stringResource(Res.string.compact_col_azimuth),
                        modifier = Modifier.width(azWidth),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    stringResource(Res.string.compact_col_done),
                    modifier = Modifier.width(doneWidth),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(Res.string.compact_col_trend),
                    modifier = Modifier.width(trendWidth),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(Res.string.compact_col_offset),
                    modifier = Modifier.width(offsetWidth),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            HorizontalDivider()

            if (visibleEvents.isEmpty()) {
                Text(stringResource(Res.string.compact_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(visibleEvents, key = { eventKey(it) }) { e ->
                        val passed = isToday && (e.userInstant?.isBefore(nowInstant) == true)
                        val key = eventKey(e)
                        val entry = statsByKey[key]
                        val draft = drafts[key] ?: CompactTrendDraft(
                            selection = entry?.direction,
                            offsetText = entry?.offsetMinutes?.toString().orEmpty()
                        )
                        CompactEventRow(
                            modifier = Modifier.horizontalScroll(scroll),
                            e = e,
                            timeFmt = timeFmt,
                            passed = passed,
                            showUtcTime = showUtcTime,
                            showAzimuth = showAzimuth,
                            timeWidth = timeWidth,
                            utcWidth = utcWidth,
                            cityWidth = cityWidth,
                            eventWidth = eventWidth,
                            chipsWidth = chipsWidth,
                            azWidth = azWidth,
                            doneWidth = doneWidth,
                            trendWidth = trendWidth,
                            offsetWidth = offsetWidth,
                            draft = draft,
                            onDraftChange = { drafts[key] = it },
                            onUpsertStatEntry = onUpsertStatEntry,
                            onDeleteStatEntry = onDeleteStatEntry
                        )
                    }
                }
            }

    }
}

private enum class CompactSortMode(val label: org.jetbrains.compose.resources.StringResource) {
    Time(Res.string.compact_sort_by_time),
    City(Res.string.compact_sort_by_city),
    Event(Res.string.compact_sort_by_event);

    fun next(): CompactSortMode = when (this) {
        Time -> City
        City -> Event
        Event -> Time
    }
}

@Composable
private fun CompactEventRow(
    modifier: Modifier,
    e: CompactEvent,
    timeFmt: DateTimeFormatter,
    passed: Boolean,
    showUtcTime: Boolean,
    showAzimuth: Boolean,
    timeWidth: Dp,
    utcWidth: Dp,
    cityWidth: Dp,
    eventWidth: Dp,
    chipsWidth: Dp,
    azWidth: Dp,
    doneWidth: Dp,
    trendWidth: Dp,
    offsetWidth: Dp,
    draft: CompactTrendDraft,
    onDraftChange: (CompactTrendDraft) -> Unit,
    onUpsertStatEntry: (CompactEvent, MoveDirection, Int) -> Unit,
    onDeleteStatEntry: (CompactEvent) -> Unit
) {
    val textColor = if (passed) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val dash = stringResource(Res.string.value_dash)
    val timeStr = e.userTime?.format(timeFmt) ?: dash
    val utcStr = e.utcTime?.format(timeFmt) ?: dash
    val azText = e.azimuthDeg?.let { deg ->
        val dir = cardinalLabel(azimuthToCardinalIndex(deg))
        stringResource(Res.string.azimuth_value, deg.roundToInt(), dir)
    } ?: dash
    val shiftChips = buildShiftChips(e)
    val eventLabel = eventLabel(e.eventType)
    val selection = draft.selection
    val offsetText = draft.offsetText
    val canEdit = e.userInstant != null
    val offsetEditable = canEdit && selection != null

    Row(
        modifier
            .padding(vertical = 2.dp)
            .alpha(if (passed) 0.72f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(timeStr, modifier = Modifier.width(timeWidth), color = textColor)
        if (showUtcTime) {
            Text(utcStr, modifier = Modifier.width(utcWidth), color = textColor)
        }
        Text(
            e.cityLabel,
            modifier = Modifier.width(cityWidth),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            eventLabel,
            modifier = Modifier.width(eventWidth),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(
            modifier = Modifier.width(chipsWidth),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            shiftChips.forEach { chip ->
                Box(
                    modifier = Modifier
                        .background(
                            color = if (passed) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        chip,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (passed) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }
        }

        if (showAzimuth) {
            Row(
                modifier = Modifier.width(azWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (e.azimuthDeg != null) {
                    Icon(
                        imageVector = SnowIcons.ArrowRight,
                        contentDescription = stringResource(Res.string.label_azimuth),
                        modifier = Modifier
                            .size(16.dp)
                            .rotate((e.azimuthDeg - 90.0).toFloat()),
                        tint = textColor
                    )
                }
                Text(azText, color = textColor, style = MaterialTheme.typography.bodySmall)
            }
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

        Box(modifier = Modifier.width(trendWidth), contentAlignment = Alignment.CenterStart) {
            TrendSelector(
                selection = selection,
                enabled = canEdit,
                onSelect = selection@{ newSelection ->
                    if (!canEdit) return@selection
                    val normalizedOffset = normalizeOffsetText(draft.offsetText)
                    val offsetValue = normalizedOffset.toIntOrNull() ?: 0
                    val next = draft.copy(selection = newSelection, offsetText = normalizedOffset)
                    onDraftChange(next)
                    onUpsertStatEntry(e, newSelection, offsetValue)
                },
                onClear = {
                    if (!canEdit) return@TrendSelector
                    onDraftChange(draft.copy(selection = null))
                    onDeleteStatEntry(e)
                }
            )
        }

        Box(modifier = Modifier.width(offsetWidth), contentAlignment = Alignment.CenterStart) {
            OffsetInput(
                value = offsetText,
                enabled = offsetEditable,
                editable = offsetEditable,
                modifier = Modifier.width(offsetWidth),
                onValueChange = { value ->
                    if (!offsetEditable) return@OffsetInput
                    val trimmed = value.trim()
                    val valid = trimmed.isEmpty() || trimmed == "-" || OffsetRegex.matches(trimmed)
                    if (valid) {
                        onDraftChange(draft.copy(offsetText = trimmed))
                        val parsed = trimmed.toIntOrNull()
                        if (parsed != null) {
                            onUpsertStatEntry(e, selection, parsed)
                        }
                    }
                },
                onStep = { delta ->
                    if (!offsetEditable) return@OffsetInput
                    val current = offsetText.toIntOrNull() ?: 0
                    val updated = (current + delta).coerceIn(-999, 999)
                    val next = draft.copy(offsetText = updated.toString())
                    onDraftChange(next)
                    onUpsertStatEntry(e, selection, updated)
                }
            )
        }
    }
}

@Composable
private fun buildShiftChips(event: CompactEvent): List<String> {
    val chips = mutableListOf<String>()
    when {
        event.cityDayOffset > 0 -> chips += stringResource(Res.string.compact_chip_city_tomorrow)
        event.cityDayOffset < 0 -> chips += stringResource(Res.string.compact_chip_city_yesterday)
    }
    return chips
}

private data class CompactTrendDraft(
    val selection: MoveDirection?,
    val offsetText: String
)

@Composable
private fun TrendSelector(
    selection: MoveDirection?,
    enabled: Boolean,
    onSelect: (MoveDirection) -> Unit,
    onClear: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        TrendOptionButton(
            icon = SnowIcons.ArrowUp,
            selected = selection == MoveDirection.Up,
            enabled = enabled,
            contentDescription = stringResource(Res.string.stats_direction_up),
            onClick = { onSelect(MoveDirection.Up) }
        )
        TrendOptionButton(
            icon = SnowIcons.ArrowDown,
            selected = selection == MoveDirection.Down,
            enabled = enabled,
            contentDescription = stringResource(Res.string.stats_direction_down),
            onClick = { onSelect(MoveDirection.Down) }
        )
        TrendOptionButton(
            icon = SnowIcons.Close,
            selected = false,
            enabled = enabled,
            contentDescription = stringResource(Res.string.compact_trend_none),
            onClick = onClear
        )
    }
}

@Composable
private fun TrendOptionButton(
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val clickModifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(container, MaterialTheme.shapes.small)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun normalizeOffsetText(text: String): String {
    val trimmed = text.trim()
    return if (trimmed.isEmpty() || trimmed == "-") "0" else trimmed
}

private val OffsetRegex = Regex("^-?\\d{1,3}$")

@Composable
private fun OffsetInput(
    value: String,
    enabled: Boolean,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onStep: (Int) -> Unit
) {
    val fieldMinHeight = 34.dp
    val contentPadding = OutlinedTextFieldDefaults.contentPadding(
        start = 8.dp,
        top = 4.dp,
        end = 8.dp,
        bottom = 4.dp
    )
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

    Row(
        modifier = modifier.heightIn(min = fieldMinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = !editable,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelMedium.copy(color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = fieldMinHeight),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = {
                        Text(
                            stringResource(Res.string.stats_offset_label),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = colors,
                    contentPadding = contentPadding
                )
            }
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepButton(
                icon = SnowIcons.CaretUp,
                enabled = enabled && editable,
                onClick = { onStep(1) }
            )
            StepButton(
                icon = SnowIcons.CaretDown,
                enabled = enabled && editable,
                onClick = { onStep(-1) }
            )
        }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceVariant
    val content = MaterialTheme.colorScheme.onSurfaceVariant
    val clickModifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(container, MaterialTheme.shapes.extraSmall)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(10.dp)
        )
    }
}

private fun eventKey(event: CompactEvent): String {
    val instant = event.userInstant?.toEpochMilli() ?: 0L
    return "${event.cityKey}-${event.eventType.name}-$instant"
}

private fun statKey(entry: StatEntry): String =
    "${entry.cityKey}-${entry.eventType.name}-${entry.eventInstant.toEpochMilli()}"

@Composable
private fun eventLabel(type: CompactEventType): String = when (type) {
    CompactEventType.Sunrise -> stringResource(Res.string.event_sunrise)
    CompactEventType.Sunset -> stringResource(Res.string.event_sunset)
    CompactEventType.Moonrise -> stringResource(Res.string.event_moonrise)
    CompactEventType.Moonset -> stringResource(Res.string.event_moonset)
}

