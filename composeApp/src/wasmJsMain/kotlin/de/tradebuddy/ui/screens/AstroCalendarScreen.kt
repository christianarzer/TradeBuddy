package de.tradebuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.model.AstroAspectEvent
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroEventStatus
import de.tradebuddy.domain.model.AstroPlanet
import de.tradebuddy.domain.model.AstroWeekDaySummary
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.presentation.AstroCalendarTab
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.MoonPhaseCard
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_next_day
import trade_buddy.composeapp.generated.resources.action_pick_date
import trade_buddy.composeapp.generated.resources.action_prev_day
import trade_buddy.composeapp.generated.resources.action_today
import trade_buddy.composeapp.generated.resources.astro_aspect_conjunction
import trade_buddy.composeapp.generated.resources.astro_aspect_opposition
import trade_buddy.composeapp.generated.resources.astro_aspect_sextile
import trade_buddy.composeapp.generated.resources.astro_aspect_square
import trade_buddy.composeapp.generated.resources.astro_aspect_trine
import trade_buddy.composeapp.generated.resources.astro_countdown_ago
import trade_buddy.composeapp.generated.resources.astro_countdown_in
import trade_buddy.composeapp.generated.resources.astro_countdown_now
import trade_buddy.composeapp.generated.resources.astro_empty
import trade_buddy.composeapp.generated.resources.astro_empty_filtered
import trade_buddy.composeapp.generated.resources.astro_filter_aspects
import trade_buddy.composeapp.generated.resources.astro_filter_planets
import trade_buddy.composeapp.generated.resources.astro_filters_section_title
import trade_buddy.composeapp.generated.resources.astro_filters_toggle_collapse
import trade_buddy.composeapp.generated.resources.astro_filters_toggle_expand
import trade_buddy.composeapp.generated.resources.astro_label_countdown
import trade_buddy.composeapp.generated.resources.astro_label_orb
import trade_buddy.composeapp.generated.resources.astro_orb_controls_title
import trade_buddy.composeapp.generated.resources.astro_orb_reset_defaults
import trade_buddy.composeapp.generated.resources.astro_status_exact
import trade_buddy.composeapp.generated.resources.astro_status_passed
import trade_buddy.composeapp.generated.resources.astro_status_upcoming
import trade_buddy.composeapp.generated.resources.astro_subtitle
import trade_buddy.composeapp.generated.resources.astro_title
import trade_buddy.composeapp.generated.resources.astro_week_title
import trade_buddy.composeapp.generated.resources.astro_planet_jupiter
import trade_buddy.composeapp.generated.resources.astro_planet_mars
import trade_buddy.composeapp.generated.resources.astro_planet_mercury
import trade_buddy.composeapp.generated.resources.astro_planet_moon
import trade_buddy.composeapp.generated.resources.astro_planet_neptune
import trade_buddy.composeapp.generated.resources.astro_planet_pluto
import trade_buddy.composeapp.generated.resources.astro_planet_saturn
import trade_buddy.composeapp.generated.resources.astro_planet_sun
import trade_buddy.composeapp.generated.resources.astro_planet_uranus
import trade_buddy.composeapp.generated.resources.astro_planet_venus
import trade_buddy.composeapp.generated.resources.datepicker_cancel
import trade_buddy.composeapp.generated.resources.datepicker_ok
import trade_buddy.composeapp.generated.resources.format_date_short
import trade_buddy.composeapp.generated.resources.format_time_precise
import trade_buddy.composeapp.generated.resources.label_date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AstroCalendarScreen(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    val astroState = state.astroCalendar
    val datePattern = stringResource(Res.string.format_date_short)
    val preciseTimePattern = stringResource(Res.string.format_time_precise)
    val dateFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern, Locale.GERMANY) }
    val preciseTimeFmt = remember(preciseTimePattern) {
        DateTimeFormatter.ofPattern(preciseTimePattern, Locale.GERMANY)
    }
    val weekFmt = remember { DateTimeFormatter.ofPattern("EEE dd.MM", Locale.GERMANY) }
    val isAspectsTab = state.selectedAstroTab == AstroCalendarTab.Aspects
    var filtersExpanded by rememberSaveable { mutableStateOf(true) }
    val hasCustomOrbs = astroState.aspectOrbs != DEFAULT_ASTRO_ASPECT_ORBS

    val filteredEvents = remember(
        astroState.allEvents,
        astroState.selectedAspects,
        astroState.selectedPlanets
    ) {
        astroState.allEvents
            .filter { event ->
                event.aspectType in astroState.selectedAspects &&
                    event.primaryPlanet.isEnabledBy(astroState.selectedPlanets) &&
                    event.secondaryPlanet.isEnabledBy(astroState.selectedPlanets)
            }
            .sortedBy { it.exactInstant }
    }
    val hasFilteredOutEvents = astroState.allEvents.isNotEmpty() && filteredEvents.isEmpty()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(Res.string.astro_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    stringResource(Res.string.astro_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.selectedDate.format(dateFmt),
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
                        IconButton(onClick = { viewModel.showDatePicker(true) }) {
                            Icon(
                                Icons.Outlined.DateRange,
                                contentDescription = stringResource(Res.string.action_pick_date)
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { viewModel.shiftDate(-1) },
                        label = { Text(stringResource(Res.string.action_prev_day)) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        }
                    )
                    AssistChip(
                        onClick = viewModel::goToToday,
                        label = { Text(stringResource(Res.string.action_today)) },
                        leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { viewModel.shiftDate(1) },
                        label = { Text(stringResource(Res.string.action_next_day)) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                        }
                    )
                }

                PrimaryTabRow(selectedTabIndex = state.selectedAstroTab.ordinal) {
                    AstroCalendarTab.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedAstroTab == tab,
                            onClick = { viewModel.setAstroTab(tab) },
                            text = { Text(stringResource(tab.label)) }
                        )
                    }
                }

                if (isAspectsTab) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(Res.string.astro_filters_section_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        AssistChip(
                            onClick = { filtersExpanded = !filtersExpanded },
                            label = {
                                Text(
                                    stringResource(
                                        if (filtersExpanded) {
                                            Res.string.astro_filters_toggle_collapse
                                        } else {
                                            Res.string.astro_filters_toggle_expand
                                        }
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (filtersExpanded) {
                                        Icons.Outlined.ExpandLess
                                    } else {
                                        Icons.Outlined.ExpandMore
                                    },
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    if (filtersExpanded) {
                        Text(
                            stringResource(Res.string.astro_filter_aspects),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AstroAspectType.entries.forEach { aspect ->
                                val selected = aspect in astroState.selectedAspects
                                val orb = astroState.aspectOrbs[aspect]
                                    ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setAstroAspectEnabled(aspect, !selected) },
                                    label = {
                                        Text("${aspect.label} (${formatDegrees(orb)}°)")
                                    },
                                    leadingIcon = {
                                        AstroGlyphBadge(
                                            glyph = aspect.glyph,
                                            tint = aspectColor(aspect),
                                            selected = selected
                                        )
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(Res.string.astro_orb_controls_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            FilledTonalButton(
                                enabled = hasCustomOrbs,
                                onClick = viewModel::resetAstroAspectOrbs
                            ) {
                                Text(stringResource(Res.string.astro_orb_reset_defaults))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AstroAspectType.entries.forEach { aspect ->
                                val orb = astroState.aspectOrbs[aspect]
                                    ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AstroGlyphBadge(
                                            glyph = aspect.glyph,
                                            tint = aspectColor(aspect),
                                            selected = true
                                        )
                                        Text(
                                            "${aspect.label}: ${formatDegrees(orb)}°",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.adjustAstroAspectOrb(aspect, increase = false) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Remove,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.adjustAstroAspectOrb(aspect, increase = true) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            stringResource(Res.string.astro_filter_planets),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AstroPlanet.entries.forEach { planet ->
                                val selected = planet in astroState.selectedPlanets
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setAstroPlanetEnabled(planet, !selected) },
                                    label = { Text(planet.label) },
                                    leadingIcon = {
                                        AstroGlyphBadge(
                                            glyph = planet.glyph,
                                            tint = planetColor(planet),
                                            selected = selected
                                        )
                                    }
                                )
                            }
                        }
                    }

                }

            }
        }

        if (!isAspectsTab) {
            MoonPhaseCard(
                state = state.moonPhases,
                userZone = state.userZone,
                nowInstant = state.nowInstant,
                onPrevMonth = { viewModel.shiftMonth(-1) },
                onNextMonth = { viewModel.shiftMonth(1) }
            )
        } else if (astroState.week.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(Res.string.astro_week_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(astroState.week, key = { it.date.toString() }) { day ->
                            AstroWeekChip(
                                day = day,
                                selectedDate = state.selectedDate,
                                weekFmt = weekFmt,
                                onSelect = viewModel::setDate
                            )
                        }
                    }
                }
            }
        }

        if (isAspectsTab && astroState.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (isAspectsTab) {
            astroState.error?.let { error ->
                Text(
                    stringResource(error),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (isAspectsTab) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredEvents.isEmpty()) {
                    Text(
                        text = if (hasFilteredOutEvents) {
                            stringResource(Res.string.astro_empty_filtered, astroState.allEvents.size)
                        } else {
                            stringResource(Res.string.astro_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            filteredEvents,
                            key = { event ->
                                "${event.primaryPlanet.name}-${event.secondaryPlanet.name}-${event.aspectType.name}-${event.exactInstant.epochSecond}"
                            }
                        ) { event ->
                            AstroEventRow(
                                event = event,
                                selectedDate = state.selectedDate,
                                nowInstant = state.nowInstant,
                                zoneId = state.userZone,
                                dateFmt = dateFmt,
                                timeFmt = preciseTimeFmt,
                                orbDegrees = astroState.aspectOrbs[event.aspectType]
                                    ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(event.aspectType)
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.atStartOfDay(state.userZone).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(state.userZone).toLocalDate()
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
private fun StatusPill(
    label: String,
    color: Color
) {
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun AstroWeekChip(
    day: AstroWeekDaySummary,
    selectedDate: LocalDate,
    weekFmt: DateTimeFormatter,
    onSelect: (LocalDate) -> Unit
) {
    FilterChip(
        selected = day.date == selectedDate,
        onClick = { onSelect(day.date) },
        label = { Text("${day.date.format(weekFmt)} (${day.eventCount})") }
    )
}

@Composable
private fun AstroEventRow(
    event: AstroAspectEvent,
    selectedDate: LocalDate,
    nowInstant: Instant,
    zoneId: ZoneId,
    dateFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter,
    orbDegrees: Double
) {
    val status = eventStatus(nowInstant = nowInstant, exactInstant = event.exactInstant)
    val statusLabel = when (status) {
        AstroEventStatus.Upcoming -> stringResource(Res.string.astro_status_upcoming)
        AstroEventStatus.Exact -> stringResource(Res.string.astro_status_exact)
        AstroEventStatus.Passed -> stringResource(Res.string.astro_status_passed)
    }
    val statusColor = when (status) {
        AstroEventStatus.Upcoming -> MaterialTheme.colorScheme.primary
        AstroEventStatus.Exact -> MaterialTheme.colorScheme.tertiary
        AstroEventStatus.Passed -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val exactLocal = event.exactInstant.formatForDay(
        selectedDate = selectedDate,
        zoneId = zoneId,
        dateFmt = dateFmt,
        timeFmt = timeFmt
    )
    val exactUtc = event.exactInstant.formatForDay(
        selectedDate = selectedDate,
        zoneId = ZoneOffset.UTC,
        dateFmt = dateFmt,
        timeFmt = timeFmt
    )
    val countdown = countdownLabel(nowInstant, event.exactInstant)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AstroGlyphBadge(
                    glyph = event.primaryPlanet.glyph,
                    tint = planetColor(event.primaryPlanet),
                    selected = true
                )
                AstroGlyphBadge(
                    glyph = event.aspectType.glyph,
                    tint = aspectColor(event.aspectType),
                    selected = true
                )
                AstroGlyphBadge(
                    glyph = event.secondaryPlanet.glyph,
                    tint = planetColor(event.secondaryPlanet),
                    selected = true
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${event.primaryPlanet.label} ${event.aspectType.label} ${event.secondaryPlanet.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${event.aspectType.angleDegrees.toInt()}° · ${stringResource(Res.string.astro_label_orb)} ±${formatDegrees(orbDegrees)}°",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusPill(label = statusLabel, color = statusColor)
            }

            TimeMetricRow(
                localText = exactLocal,
                utcText = exactUtc,
                localZoneId = zoneId.id
                ,
                countdownText = countdown
            )
        }
    }
}

@Composable
private fun TimeMetricRow(
    localText: String,
    utcText: String,
    localZoneId: String,
    countdownText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${localZoneId}: $localText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "UTC: $utcText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${localZoneId} ${stringResource(Res.string.astro_label_countdown)}: $countdownText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "UTC ${stringResource(Res.string.astro_label_countdown)}: $countdownText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AstroGlyphBadge(
    glyph: String,
    tint: Color,
    selected: Boolean
) {
    val icon = astroGlyphDrawable(glyphGlyphKey(glyph))
    Box(
        modifier = Modifier.size(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint.copy(alpha = if (selected) 1f else 0.85f),
            modifier = Modifier.size(19.dp)
        )
    }
}

private fun astroGlyphDrawable(glyphKey: String): DrawableResource = when (glyphKey) {
    "\u260C" -> Res.drawable.astro_aspect_conjunction
    "\u26B9" -> Res.drawable.astro_aspect_sextile
    "\u25A1" -> Res.drawable.astro_aspect_square
    "\u25B3" -> Res.drawable.astro_aspect_trine
    "\u260D" -> Res.drawable.astro_aspect_opposition
    "\u263D" -> Res.drawable.astro_planet_moon
    "\u2609" -> Res.drawable.astro_planet_sun
    "\u263F" -> Res.drawable.astro_planet_mercury
    "\u2640" -> Res.drawable.astro_planet_venus
    "\u2642" -> Res.drawable.astro_planet_mars
    "\u2643" -> Res.drawable.astro_planet_jupiter
    "\u2644" -> Res.drawable.astro_planet_saturn
    "\u2645" -> Res.drawable.astro_planet_uranus
    "\u2646" -> Res.drawable.astro_planet_neptune
    "\u2647" -> Res.drawable.astro_planet_pluto
    else -> Res.drawable.astro_planet_sun
}

private fun eventStatus(nowInstant: Instant, exactInstant: Instant): AstroEventStatus {
    val distanceSeconds = abs(Duration.between(nowInstant, exactInstant).seconds)
    if (distanceSeconds <= 60L) return AstroEventStatus.Exact
    return if (nowInstant.isBefore(exactInstant)) {
        AstroEventStatus.Upcoming
    } else {
        AstroEventStatus.Passed
    }
}

@Composable
private fun countdownLabel(nowInstant: Instant, exactInstant: Instant): String {
    val diffSeconds = Duration.between(nowInstant, exactInstant).seconds
    val absSeconds = abs(diffSeconds)
    if (absSeconds <= 59L) {
        return stringResource(Res.string.astro_countdown_now)
    }

    val hours = absSeconds / 3600
    val minutes = (absSeconds % 3600) / 60
    val durationLabel = when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }

    return if (diffSeconds >= 0L) {
        stringResource(Res.string.astro_countdown_in, durationLabel)
    } else {
        stringResource(Res.string.astro_countdown_ago, durationLabel)
    }
}

private fun Instant.formatForDay(
    selectedDate: LocalDate,
    zoneId: ZoneId,
    dateFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter
): String {
    val zoned = atZone(zoneId)
    val day = zoned.toLocalDate()
    val time = zoned.toLocalTime().format(timeFmt)
    return if (day == selectedDate) {
        time
    } else {
        "${day.format(dateFmt)} $time"
    }
}

private fun AstroPlanet.isEnabledBy(selectedPlanets: Set<AstroPlanet>): Boolean =
    this in selectedPlanets

private fun glyphGlyphKey(glyph: String): String = glyph
    .replace("\uFE0F", "")
    .replace("\uFE0E", "")

private fun formatDegrees(value: Double): String = (kotlin.math.round(value * 10.0) / 10.0).toString()

private fun aspectColor(type: AstroAspectType): Color = when (type) {
    AstroAspectType.Conjunction -> Color(0xFFCA8A04)
    AstroAspectType.Sextile -> Color(0xFF0D9488)
    AstroAspectType.Square -> Color(0xFFEA580C)
    AstroAspectType.Trine -> Color(0xFF16A34A)
    AstroAspectType.Opposition -> Color(0xFFDC2626)
}

private fun planetColor(planet: AstroPlanet): Color = when (planet) {
    AstroPlanet.Moon -> Color(0xFF3B82F6)
    AstroPlanet.Sun -> Color(0xFFF59E0B)
    AstroPlanet.Mercury -> Color(0xFF0EA5E9)
    AstroPlanet.Venus -> Color(0xFFDB2777)
    AstroPlanet.Mars -> Color(0xFFEF4444)
    AstroPlanet.Jupiter -> Color(0xFF14B8A6)
    AstroPlanet.Saturn -> Color(0xFF65A30D)
    AstroPlanet.Uranus -> Color(0xFF06B6D4)
    AstroPlanet.Neptune -> Color(0xFF4F46E5)
    AstroPlanet.Pluto -> Color(0xFF64748B)
}
