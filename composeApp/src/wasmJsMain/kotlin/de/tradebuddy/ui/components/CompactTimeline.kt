package de.tradebuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.model.CompactEvent
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.MoveDirection
import de.tradebuddy.domain.model.StatEntry
import de.tradebuddy.domain.util.azimuthToCardinalIndex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.browser.window
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_copy_times
import trade_buddy.composeapp.generated.resources.azimuth_value
import trade_buddy.composeapp.generated.resources.compact_col_azimuth
import trade_buddy.composeapp.generated.resources.compact_col_city
import trade_buddy.composeapp.generated.resources.compact_col_done
import trade_buddy.composeapp.generated.resources.compact_col_event
import trade_buddy.composeapp.generated.resources.compact_col_local
import trade_buddy.composeapp.generated.resources.compact_col_offset
import trade_buddy.composeapp.generated.resources.compact_col_trend
import trade_buddy.composeapp.generated.resources.compact_col_utc
import trade_buddy.composeapp.generated.resources.compact_date
import trade_buddy.composeapp.generated.resources.compact_empty
import trade_buddy.composeapp.generated.resources.compact_hint
import trade_buddy.composeapp.generated.resources.compact_shift_minus
import trade_buddy.composeapp.generated.resources.compact_shift_plus
import trade_buddy.composeapp.generated.resources.compact_sorted_by
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
import trade_buddy.composeapp.generated.resources.value_dash

@Composable
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
    nowInstant: Instant
) {
    ElevatedCard(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val datePattern = stringResource(Res.string.format_date_short)
            val copyPattern = stringResource(Res.string.format_datetime_copy)
            val dateFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern, Locale.GERMANY) }
            val copyFmt = remember(copyPattern) { DateTimeFormatter.ofPattern(copyPattern, Locale.ROOT) }
            val hasCopyTimes = events.any { it.userTime != null }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(Res.string.compact_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.compact_sorted_by, userZone.id),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(Res.string.compact_date, selectedDate.format(dateFmt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = {
                        val copyText = events.mapNotNull { it.userTime?.format(copyFmt) }
                            .joinToString(",")
                        window.navigator.clipboard.writeText(copyText)
                    },
                    enabled = hasCopyTimes
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(Res.string.action_copy_times),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.action_copy_times))
                }
            }

            val scroll = rememberScrollState()
            val timeWidth = 72.dp
            val utcWidth = 72.dp
            val cityWidth = 140.dp
            val eventWidth = 220.dp +
                if (showUtcTime) 0.dp else utcWidth +
                if (showAzimuth) 0.dp else 110.dp
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

            if (events.isEmpty()) {
                Text(stringResource(Res.string.compact_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(events, key = { eventKey(it) }) { e ->
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

            Text(
                stringResource(Res.string.compact_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    azWidth: Dp,
    doneWidth: Dp,
    trendWidth: Dp,
    offsetWidth: Dp,
    draft: CompactTrendDraft,
    onDraftChange: (CompactTrendDraft) -> Unit,
    onUpsertStatEntry: (CompactEvent, MoveDirection, Int) -> Unit,
    onDeleteStatEntry: (CompactEvent) -> Unit
) {
    val textColor = if (passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    val dash = stringResource(Res.string.value_dash)
    val timeStr = e.userTime?.format(timeFmt) ?: dash
    val utcStr = e.utcTime?.format(timeFmt) ?: dash
    val azText = e.azimuthDeg?.let { deg ->
        val dir = cardinalLabel(azimuthToCardinalIndex(deg))
        stringResource(Res.string.azimuth_value, deg.roundToInt(), dir)
    } ?: dash
    val shiftLabel = when (e.cityDayOffset) {
        1 -> stringResource(Res.string.compact_shift_plus)
        -1 -> stringResource(Res.string.compact_shift_minus)
        else -> null
    }
    val eventLabel = eventLabel(e.eventType)
    val selection = draft.selection
    val offsetText = draft.offsetText
    val canEdit = e.userInstant != null
    val offsetEditable = canEdit && selection != null

    Row(
        modifier
            .padding(vertical = 2.dp),
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
        Row(
            modifier = Modifier.width(eventWidth),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                eventLabel,
                modifier = Modifier.weight(1f),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (shiftLabel != null) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        shiftLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
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
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
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
                    imageVector = Icons.Outlined.Check,
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
            icon = Icons.Outlined.ArrowUpward,
            selected = selection == MoveDirection.Up,
            enabled = enabled,
            contentDescription = stringResource(Res.string.stats_direction_up),
            onClick = { onSelect(MoveDirection.Up) }
        )
        TrendOptionButton(
            icon = Icons.Outlined.ArrowDownward,
            selected = selection == MoveDirection.Down,
            enabled = enabled,
            contentDescription = stringResource(Res.string.stats_direction_down),
            onClick = { onSelect(MoveDirection.Down) }
        )
        TrendOptionButton(
            icon = Icons.Outlined.Close,
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
                icon = Icons.Outlined.KeyboardArrowUp,
                enabled = enabled && editable,
                onClick = { onStep(1) }
            )
            StepButton(
                icon = Icons.Outlined.KeyboardArrowDown,
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
