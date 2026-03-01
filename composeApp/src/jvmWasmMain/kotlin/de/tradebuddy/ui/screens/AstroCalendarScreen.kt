package de.tradebuddy.ui.screens

import de.tradebuddy.domain.model.AstroAspectEvent
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroPlanet
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.presentation.AstroCalendarUiState
import de.tradebuddy.presentation.AstroCalendarTab
import de.tradebuddy.presentation.SunMoonUiState
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.MoonPhaseCard
import de.tradebuddy.ui.components.shared.SnowToolbar
import de.tradebuddy.ui.components.shared.SnowToolbarIconButton
import de.tradebuddy.ui.components.shared.SnowToolbarPopupPanel
import de.tradebuddy.ui.components.SnowTabItem
import de.tradebuddy.ui.components.SnowTabs
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.LocalExtendedColors
import de.tradebuddy.ui.theme.colorFor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_next_day
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
import trade_buddy.composeapp.generated.resources.astro_angle_orb_format
import trade_buddy.composeapp.generated.resources.astro_label_countdown
import trade_buddy.composeapp.generated.resources.astro_label_since_signal
import trade_buddy.composeapp.generated.resources.astro_label_orb
import trade_buddy.composeapp.generated.resources.astro_orb_controls_title
import trade_buddy.composeapp.generated.resources.astro_orb_reset_defaults
import trade_buddy.composeapp.generated.resources.astro_sort_aspect
import trade_buddy.composeapp.generated.resources.astro_sort_time
import trade_buddy.composeapp.generated.resources.astro_sort_title
import trade_buddy.composeapp.generated.resources.astro_status_exact
import trade_buddy.composeapp.generated.resources.astro_status_passed
import trade_buddy.composeapp.generated.resources.astro_status_upcoming
import trade_buddy.composeapp.generated.resources.astro_time_countdown_format
import trade_buddy.composeapp.generated.resources.astro_time_local_format
import trade_buddy.composeapp.generated.resources.astro_time_utc_format
import trade_buddy.composeapp.generated.resources.astro_title
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AstroCalendarScreen(
    state: SunMoonUiState,
    viewModel: SunMoonViewModel
) {
    val ext = LocalExtendedColors.current
    val astroState = state.astroCalendar
    val datePattern = stringResource(Res.string.format_date_short)
    val preciseTimePattern = stringResource(Res.string.format_time_precise)
    val dateFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern, Locale.GERMANY) }
    val preciseTimeFmt = remember(preciseTimePattern) {
        DateTimeFormatter.ofPattern(preciseTimePattern, Locale.GERMANY)
    }
    val isAspectsTab = state.selectedAstroTab == AstroCalendarTab.Aspects
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(AstroListSortMode.Time) }
    val hasCustomOrbs = astroState.aspectOrbs != DEFAULT_ASTRO_ASPECT_ORBS

    val filteredEvents = remember(
        astroState.allEvents,
        astroState.selectedAspects,
        astroState.selectedPlanets,
        sortMode
    ) {
        val base = astroState.allEvents
            .filter { event ->
                event.aspectType in astroState.selectedAspects &&
                    event.primaryPlanet.isEnabledBy(astroState.selectedPlanets) &&
                    event.secondaryPlanet.isEnabledBy(astroState.selectedPlanets)
            }
        when (sortMode) {
            AstroListSortMode.Time -> base.sortedBy { it.exactInstant }
            AstroListSortMode.Aspect -> base.sortedWith(
                compareBy<AstroAspectEvent> { it.aspectType.ordinal }
                    .thenBy { it.primaryPlanet.ordinal }
                    .thenBy { it.secondaryPlanet.ordinal }
                    .thenBy { it.exactInstant }
            )
        }
    }
    val hasFilteredOutEvents = astroState.allEvents.isNotEmpty() && filteredEvents.isEmpty()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(
                    stringResource(Res.string.astro_title),
                    style = MaterialTheme.typography.headlineMedium
                )

                SnowTabs(
                    items = AstroCalendarTab.entries.map { tab ->
                        SnowTabItem(id = tab.name, label = stringResource(tab.label))
                    },
                    selectedId = state.selectedAstroTab.name,
                    onSelect = { id ->
                        AstroCalendarTab.entries.firstOrNull { it.name == id }?.let(viewModel::setAstroTab)
                    }
                )

                if (isAspectsTab) {
                    SnowToolbar {
                        Box {
                            SnowToolbarIconButton(icon = SnowIcons.Filter, onClick = { showFilterPopup = true })
                            DropdownMenu(
                                expanded = showFilterPopup,
                                onDismissRequest = { showFilterPopup = false }
                            ) {
                                SnowToolbarPopupPanel(modifier = Modifier.widthIn(max = 440.dp)) {
                                    AstroFilterPanel(
                                        astroState = astroState,
                                        hasCustomOrbs = hasCustomOrbs,
                                        onSetAspectEnabled = viewModel::setAstroAspectEnabled,
                                        onSetPlanetEnabled = viewModel::setAstroPlanetEnabled,
                                        onAdjustOrb = viewModel::adjustAstroAspectOrb,
                                        onResetOrbs = viewModel::resetAstroAspectOrbs
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
                                        text = stringResource(Res.string.astro_sort_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    AstroListSortMode.entries.forEach { mode ->
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
                                .clickable { viewModel.showDatePicker(true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.selectedDate.format(dateFmt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        SnowToolbarIconButton(
                            icon = SnowIcons.ArrowLeft,
                            onClick = { viewModel.shiftDate(-1) },
                            contentDescription = stringResource(Res.string.action_prev_day)
                        )
                        SnowToolbarIconButton(
                            icon = SnowIcons.Calendar,
                            onClick = viewModel::goToToday,
                            contentDescription = stringResource(Res.string.action_today)
                        )
                        SnowToolbarIconButton(
                            icon = SnowIcons.ArrowRight,
                            onClick = { viewModel.shiftDate(1) },
                            contentDescription = stringResource(Res.string.action_next_day)
                        )
                        Spacer(Modifier.weight(1f))
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
        }


        if (isAspectsTab && astroState.isLoading) {
            val progress = astroState.loadingProgress
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
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(
                            filteredEvents,
                            key = { _, event ->
                                "${event.primaryPlanet.name}-${event.secondaryPlanet.name}-${event.aspectType.name}-${event.exactInstant.epochSecond}"
                            }
                        ) { index, event ->
                            AstroEventRow(
                                event = event,
                                selectedDate = state.selectedDate,
                                nowInstant = state.nowInstant,
                                zoneId = state.userZone,
                                astroTimeOffsetMinutes = state.astroTimeOffsetMinutes,
                                dateFmt = dateFmt,
                                timeFmt = preciseTimeFmt,
                                orbDegrees = astroState.aspectOrbs[event.aspectType]
                                    ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(event.aspectType),
                                showDivider = index < filteredEvents.lastIndex
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
private fun AstroFilterPanel(
    astroState: AstroCalendarUiState,
    hasCustomOrbs: Boolean,
    onSetAspectEnabled: (AstroAspectType, Boolean) -> Unit,
    onSetPlanetEnabled: (AstroPlanet, Boolean) -> Unit,
    onAdjustOrb: (AstroAspectType, Boolean) -> Unit,
    onResetOrbs: () -> Unit
) {
    val ext = LocalExtendedColors.current
    Column(
        modifier = Modifier
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                val orb = astroState.aspectOrbs[aspect] ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
                FilterChip(
                    selected = selected,
                    onClick = { onSetAspectEnabled(aspect, !selected) },
                    label = { Text("${aspect.label} (${formatDegrees(orb)}°)") },
                    leadingIcon = {
                        AstroGlyphBadge(
                            glyph = aspect.glyph,
                            tint = ext.colorFor(aspect),
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
                onClick = onResetOrbs
            ) {
                Text(stringResource(Res.string.astro_orb_reset_defaults))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AstroAspectType.entries.forEach { aspect ->
                val orb = astroState.aspectOrbs[aspect] ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
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
                            tint = ext.colorFor(aspect),
                            selected = true
                        )
                        Text(
                            "${aspect.label}: ${formatDegrees(orb)}°",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(onClick = { onAdjustOrb(aspect, false) }) {
                        Icon(imageVector = SnowIcons.Minus, contentDescription = null)
                    }
                    IconButton(onClick = { onAdjustOrb(aspect, true) }) {
                        Icon(imageVector = SnowIcons.Plus, contentDescription = null)
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
                    onClick = { onSetPlanetEnabled(planet, !selected) },
                    label = { Text(planet.label) },
                    leadingIcon = {
                        AstroGlyphBadge(
                            glyph = planet.glyph,
                            tint = ext.colorFor(planet),
                            selected = selected
                        )
                    }
                )
            }
        }
    }
}

private enum class AstroListSortMode(val label: StringResource) {
    Time(Res.string.astro_sort_time),
    Aspect(Res.string.astro_sort_aspect);

    fun next(): AstroListSortMode = when (this) {
        Time -> Aspect
        Aspect -> Time
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
private fun AstroEventRow(
    event: AstroAspectEvent,
    selectedDate: LocalDate,
    nowInstant: Instant,
    zoneId: ZoneId,
    astroTimeOffsetMinutes: Int,
    dateFmt: DateTimeFormatter,
    timeFmt: DateTimeFormatter,
    orbDegrees: Double,
    showDivider: Boolean
) {
    val ext = LocalExtendedColors.current
    val adjustedExactInstant = event.exactInstant.plus(Duration.ofMinutes(astroTimeOffsetMinutes.toLong()))
    val status = eventStatus(nowInstant = nowInstant, exactInstant = adjustedExactInstant)
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

    val exactLocal = adjustedExactInstant.formatForDay(
        selectedDate = selectedDate,
        zoneId = zoneId,
        dateFmt = dateFmt,
        timeFmt = timeFmt
    )
    val exactUtc = adjustedExactInstant.formatForDay(
        selectedDate = selectedDate,
        zoneId = ZoneOffset.UTC,
        dateFmt = dateFmt,
        timeFmt = timeFmt
    )
    val countdown = countdownState(nowInstant, adjustedExactInstant)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AstroGlyphBadge(
                glyph = event.primaryPlanet.glyph,
                tint = ext.colorFor(event.primaryPlanet),
                selected = true
            )
            AstroGlyphBadge(
                glyph = event.aspectType.glyph,
                tint = ext.colorFor(event.aspectType),
                selected = true
            )
            AstroGlyphBadge(
                glyph = event.secondaryPlanet.glyph,
                tint = ext.colorFor(event.secondaryPlanet),
                selected = true
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${event.primaryPlanet.label} ${event.aspectType.label} ${event.secondaryPlanet.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(
                        Res.string.astro_angle_orb_format,
                        event.aspectType.angleDegrees.toInt(),
                        stringResource(Res.string.astro_label_orb),
                        formatDegrees(orbDegrees)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusPill(label = statusLabel, color = statusColor)
        }

        TimeMetricRow(
            localText = exactLocal,
            utcText = exactUtc,
            localZoneId = zoneId.id,
            countdownText = countdown.text,
            countdownRelation = countdown.relation
        )

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ext.shellDivider)
            )
        }
    }
}

@Composable
private fun TimeMetricRow(
    localText: String,
    utcText: String,
    localZoneId: String,
    countdownText: String,
    countdownRelation: CountdownRelation
) {
    val countdownLabel = when (countdownRelation) {
        CountdownRelation.Since -> stringResource(Res.string.astro_label_since_signal)
        CountdownRelation.Until,
        CountdownRelation.Now -> stringResource(Res.string.astro_label_countdown)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.astro_time_local_format, localZoneId, localText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.astro_time_utc_format, utcText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.astro_time_countdown_format, localZoneId, countdownLabel, countdownText),
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

private enum class AstroEventStatus {
    Upcoming,
    Exact,
    Passed
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
private fun countdownState(nowInstant: Instant, exactInstant: Instant): CountdownState {
    val diffSeconds = Duration.between(nowInstant, exactInstant).seconds
    val absSeconds = abs(diffSeconds)
    if (absSeconds <= 59L) {
        return CountdownState(
            text = stringResource(Res.string.astro_countdown_now),
            relation = CountdownRelation.Now
        )
    }

    val hours = absSeconds / 3600
    val minutes = (absSeconds % 3600) / 60
    val durationLabel = when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }

    return if (diffSeconds >= 0L) {
        CountdownState(
            text = stringResource(Res.string.astro_countdown_in, durationLabel),
            relation = CountdownRelation.Until
        )
    } else {
        CountdownState(
            text = stringResource(Res.string.astro_countdown_ago, durationLabel),
            relation = CountdownRelation.Since
        )
    }
}

private enum class CountdownRelation {
    Until,
    Since,
    Now
}

private data class CountdownState(
    val text: String,
    val relation: CountdownRelation
)

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




