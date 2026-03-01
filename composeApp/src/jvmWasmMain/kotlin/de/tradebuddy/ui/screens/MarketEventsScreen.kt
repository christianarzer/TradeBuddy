package de.tradebuddy.ui.screens

import de.tradebuddy.domain.model.EventWatchPreference
import de.tradebuddy.domain.model.MarketEvent
import de.tradebuddy.domain.model.MarketEventImpact
import de.tradebuddy.domain.model.MarketEventType
import de.tradebuddy.domain.model.MarketRegion
import de.tradebuddy.presentation.MarketEventsUiState
import de.tradebuddy.presentation.MarketEventsViewModel
import de.tradebuddy.ui.components.shared.SnowToolbar
import de.tradebuddy.ui.components.shared.SnowToolbarIconButton
import de.tradebuddy.ui.components.shared.SnowToolbarPopupPanel
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.AppIconSize
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.extended
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.market_events_all_days
import trade_buddy.composeapp.generated.resources.market_events_all_regions
import trade_buddy.composeapp.generated.resources.market_events_all_types
import trade_buddy.composeapp.generated.resources.market_events_actual
import trade_buddy.composeapp.generated.resources.market_events_empty
import trade_buddy.composeapp.generated.resources.market_events_error
import trade_buddy.composeapp.generated.resources.market_events_filter_summary
import trade_buddy.composeapp.generated.resources.market_events_forecast
import trade_buddy.composeapp.generated.resources.market_events_header_consensus
import trade_buddy.composeapp.generated.resources.market_events_header_country
import trade_buddy.composeapp.generated.resources.market_events_header_date
import trade_buddy.composeapp.generated.resources.market_events_header_event
import trade_buddy.composeapp.generated.resources.market_events_header_impact
import trade_buddy.composeapp.generated.resources.market_events_header_type
import trade_buddy.composeapp.generated.resources.market_events_high_impact
import trade_buddy.composeapp.generated.resources.market_events_medium_impact
import trade_buddy.composeapp.generated.resources.market_events_not_available
import trade_buddy.composeapp.generated.resources.market_events_previous
import trade_buddy.composeapp.generated.resources.market_events_relative_ago
import trade_buddy.composeapp.generated.resources.market_events_relative_in
import trade_buddy.composeapp.generated.resources.market_events_region_china
import trade_buddy.composeapp.generated.resources.market_events_region_euro_area
import trade_buddy.composeapp.generated.resources.market_events_region_global
import trade_buddy.composeapp.generated.resources.market_events_region_japan
import trade_buddy.composeapp.generated.resources.market_events_region_united_kingdom
import trade_buddy.composeapp.generated.resources.market_events_region_united_states
import trade_buddy.composeapp.generated.resources.market_events_showing_hint
import trade_buddy.composeapp.generated.resources.market_events_sort_impact
import trade_buddy.composeapp.generated.resources.market_events_sort_time_asc
import trade_buddy.composeapp.generated.resources.market_events_sort_time_desc
import trade_buddy.composeapp.generated.resources.market_events_status_high
import trade_buddy.composeapp.generated.resources.market_events_status_low
import trade_buddy.composeapp.generated.resources.market_events_status_medium
import trade_buddy.composeapp.generated.resources.market_events_today
import trade_buddy.composeapp.generated.resources.market_events_title
import trade_buddy.composeapp.generated.resources.market_events_tomorrow
import trade_buddy.composeapp.generated.resources.market_events_type_central_bank
import trade_buddy.composeapp.generated.resources.market_events_type_consumer_confidence
import trade_buddy.composeapp.generated.resources.market_events_type_employment
import trade_buddy.composeapp.generated.resources.market_events_type_gdp
import trade_buddy.composeapp.generated.resources.market_events_type_inflation
import trade_buddy.composeapp.generated.resources.market_events_type_interest_rate
import trade_buddy.composeapp.generated.resources.market_events_type_manufacturing
import trade_buddy.composeapp.generated.resources.market_events_type_other
import trade_buddy.composeapp.generated.resources.market_events_upcoming
import trade_buddy.composeapp.generated.resources.market_events_watchlist
import trade_buddy.composeapp.generated.resources.market_events_weekday
import trade_buddy.composeapp.generated.resources.stats_filter_sort
import trade_buddy.composeapp.generated.resources.stats_filter_title

@Composable
fun MarketEventsScreen(
    state: MarketEventsUiState,
    viewModel: MarketEventsViewModel
) {
    val ext = MaterialTheme.extended
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    val tomorrow = remember(today) { today.plusDays(1) }
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(MarketEventsSortMode.TimeAsc) }
    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM HH:mm", Locale.GERMANY) }

    val groupedRows: List<Pair<LocalDate, List<MarketEvent>>> = remember(state.filteredGroups, sortMode, zoneId) {
        val sortedEvents: List<MarketEvent> = when (sortMode) {
            MarketEventsSortMode.TimeAsc -> state.filteredGroups
                .flatMap { it.events }
                .sortedWith(
                    compareBy<MarketEvent> { it.scheduledAt }
                        .thenByDescending { it.impact.priority }
                        .thenBy { it.title }
                )
            MarketEventsSortMode.TimeDesc -> state.filteredGroups
                .flatMap { it.events }
                .sortedWith(
                    compareByDescending<MarketEvent> { it.scheduledAt }
                        .thenByDescending { it.impact.priority }
                        .thenBy { it.title }
                )
            MarketEventsSortMode.Impact -> state.filteredGroups
                .flatMap { it.events }
                .sortedWith(
                    compareByDescending<MarketEvent> { it.impact.priority }
                        .thenBy { it.scheduledAt }
                        .thenBy { it.title }
                )
        }
        val groupedByDate: Map<LocalDate, List<MarketEvent>> = sortedEvents
            .groupBy { event -> event.scheduledAt.atZone(zoneId).toLocalDate() }
        groupedByDate
            .entries
            .sortedBy { entry -> entry.key }
            .map { entry -> entry.key to entry.value }
    }
    val visibleCount = groupedRows.sumOf { row -> row.second.size }
    val watchlistCount = state.watchPreferences.size
    val highImpactCount = groupedRows.sumOf { row ->
        row.second.count { event -> event.impact == MarketEventImpact.High }
    }
    val mediumImpactCount = groupedRows.sumOf { row ->
        row.second.count { event -> event.impact == MarketEventImpact.Medium }
    }
    val nextCountdown = groupedRows
        .asSequence()
        .flatMap { row -> row.second.asSequence() }
        .filter { event -> event.scheduledAt.isAfter(state.nowInstant) }
        .sortedBy { event -> event.scheduledAt }
        .firstOrNull()
        ?.relativeLabel(state.nowInstant)
        ?: stringResource(Res.string.market_events_not_available)
    val lastUpdatedText = state.lastUpdated?.let { instant ->
        instant.atZone(zoneId).format(dateTimeFormatter)
    } ?: stringResource(Res.string.market_events_not_available)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        Text(text = stringResource(Res.string.market_events_title), style = MaterialTheme.typography.titleMedium)
        MarketStatsRow(
            visibleCount = visibleCount,
            highImpactCount = highImpactCount,
            mediumImpactCount = mediumImpactCount,
            watchlistCount = watchlistCount,
            nextCountdown = nextCountdown
        )

        ToolbarRow(
            showFilterPopup = showFilterPopup,
            onShowFilterPopupChange = { showFilterPopup = it },
            showSortPopup = showSortPopup,
            onShowSortPopupChange = { showSortPopup = it },
            watchlistOnly = state.filter.watchlistOnly,
            onToggleWatchlist = { viewModel.setWatchlistOnly(!state.filter.watchlistOnly) },
            selectedDay = state.selectedDay,
            today = today,
            tomorrow = tomorrow,
            selectedImpacts = state.filter.impacts,
            onSelectToday = viewModel::selectToday,
            onSelectTomorrow = viewModel::selectTomorrow,
            onSelectAllDays = viewModel::clearDaySelection,
            onToggleImpact = viewModel::toggleImpact,
            availableRegions = state.availableRegions,
            selectedRegions = state.filter.regions,
            onClearRegions = viewModel::clearRegionFilters,
            onToggleRegion = viewModel::toggleRegion,
            availableEventTypes = state.availableEventTypes,
            selectedEventTypes = state.filter.eventTypes,
            onClearEventTypes = viewModel::clearEventTypeFilters,
            onToggleEventType = viewModel::toggleEventType,
            sortMode = sortMode,
            onSortModeChange = { sortMode = it },
            onClearFilters = viewModel::clearFilters,
            visibleCount = visibleCount,
            watchlistCount = watchlistCount,
            lastUpdatedText = lastUpdatedText
        )

        Column(modifier = Modifier.fillMaxSize()) {
            EventsTableHeader()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ext.shellDivider)
            )

            when {
                state.isLoading -> {
                    LazyColumn(modifier = Modifier.weight(1f, fill = true)) {
                        items(8) { index -> SkeletonRow(index) }
                    }
                }

                state.errorMessage != null -> {
                    MessageRow(
                        message = stringResource(Res.string.market_events_error) + ": ${state.errorMessage}",
                        modifier = Modifier.weight(1f)
                    )
                }

                visibleCount == 0 -> {
                    MessageRow(
                        message = stringResource(Res.string.market_events_empty),
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.weight(1f, fill = true)) {
                        groupedRows.forEach { row ->
                            val date = row.first
                            val eventsForDate = row.second
                            item(key = "day-${date}") {
                                DaySeparator(
                                    date = date,
                                    today = today,
                                    tomorrow = tomorrow,
                                    zoneId = zoneId
                                )
                            }
                            items(items = eventsForDate, key = { event: MarketEvent -> event.id }) { event ->
                                val watchPreference = state.watchPreferences.firstOrNull { preference ->
                                    preference.eventId == event.id
                                }
                                EventsTableRow(
                                    event = event,
                                    watchPreference = watchPreference,
                                    zoneId = zoneId,
                                    now = state.nowInstant,
                                    onToggleWatch = { enabled -> viewModel.toggleWatchlist(event.id, enabled) },
                                    onToggleReminder = { enabled ->
                                        viewModel.setReminder(event.id, enabled, if (enabled) 30 else null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            FooterHintRow(visibleCount = visibleCount)
        }
    }
}

@Composable
private fun MarketStatsRow(
    visibleCount: Int,
    highImpactCount: Int,
    mediumImpactCount: Int,
    watchlistCount: Int,
    nextCountdown: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 980.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    MarketStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.market_events_upcoming),
                        value = visibleCount.toString()
                    )
                    MarketStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.market_events_high_impact),
                        value = highImpactCount.toString(),
                        valueColor = MaterialTheme.extended.impactHigh
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    MarketStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.market_events_medium_impact),
                        value = mediumImpactCount.toString(),
                        valueColor = MaterialTheme.extended.impactMedium
                    )
                    MarketStatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.market_events_watchlist),
                        value = watchlistCount.toString(),
                        subtitle = nextCountdown
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                MarketStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.market_events_upcoming),
                    value = visibleCount.toString()
                )
                MarketStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.market_events_high_impact),
                    value = highImpactCount.toString(),
                    valueColor = MaterialTheme.extended.impactHigh
                )
                MarketStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.market_events_medium_impact),
                    value = mediumImpactCount.toString(),
                    valueColor = MaterialTheme.extended.impactMedium
                )
                MarketStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.market_events_watchlist),
                    value = watchlistCount.toString(),
                    subtitle = nextCountdown
                )
            }
        }
    }
}

@Composable
private fun MarketStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(ext.toolbarSurface)
            .border(width = 1.dp, color = ext.shellDivider, shape = MaterialTheme.shapes.small)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = ext.sidebarTextMuted
            )
        }
    }
}

@Composable
private fun ToolbarRow(
    showFilterPopup: Boolean,
    onShowFilterPopupChange: (Boolean) -> Unit,
    showSortPopup: Boolean,
    onShowSortPopupChange: (Boolean) -> Unit,
    watchlistOnly: Boolean,
    onToggleWatchlist: () -> Unit,
    selectedDay: LocalDate?,
    today: LocalDate,
    tomorrow: LocalDate,
    selectedImpacts: Set<MarketEventImpact>,
    onSelectToday: () -> Unit,
    onSelectTomorrow: () -> Unit,
    onSelectAllDays: () -> Unit,
    onToggleImpact: (MarketEventImpact) -> Unit,
    availableRegions: Set<MarketRegion>,
    selectedRegions: Set<MarketRegion>,
    onClearRegions: () -> Unit,
    onToggleRegion: (MarketRegion) -> Unit,
    availableEventTypes: Set<MarketEventType>,
    selectedEventTypes: Set<MarketEventType>,
    onClearEventTypes: () -> Unit,
    onToggleEventType: (MarketEventType) -> Unit,
    sortMode: MarketEventsSortMode,
    onSortModeChange: (MarketEventsSortMode) -> Unit,
    onClearFilters: () -> Unit,
    visibleCount: Int,
    watchlistCount: Int,
    lastUpdatedText: String
) {
    val ext = MaterialTheme.extended
    SnowToolbar {
        Box {
            SnowToolbarIconButton(
                icon = if (watchlistOnly) SnowIcons.Bookmark else SnowIcons.Filter,
                onClick = { onShowFilterPopupChange(true) }
            )
            DropdownMenu(
                expanded = showFilterPopup,
                onDismissRequest = { onShowFilterPopupChange(false) }
            ) {
                MarketEventsFilterPanel(
                    selectedDay = selectedDay,
                    today = today,
                    tomorrow = tomorrow,
                    selectedImpacts = selectedImpacts,
                    onSelectToday = onSelectToday,
                    onSelectTomorrow = onSelectTomorrow,
                    onSelectAllDays = onSelectAllDays,
                    onToggleImpact = onToggleImpact,
                    watchlistOnly = watchlistOnly,
                    onToggleWatchlist = onToggleWatchlist,
                    availableRegions = availableRegions,
                    selectedRegions = selectedRegions,
                    onClearRegions = onClearRegions,
                    onToggleRegion = onToggleRegion,
                    availableEventTypes = availableEventTypes,
                    selectedEventTypes = selectedEventTypes,
                    onClearEventTypes = onClearEventTypes,
                    onToggleEventType = onToggleEventType,
                    onClearFilters = onClearFilters
                )
            }
        }
        Box {
            SnowToolbarIconButton(
                icon = SnowIcons.Sort,
                onClick = { onShowSortPopupChange(true) }
            )
            DropdownMenu(
                expanded = showSortPopup,
                onDismissRequest = { onShowSortPopupChange(false) }
            ) {
                SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 220.dp, max = 300.dp)) {
                    Text(
                        text = stringResource(Res.string.stats_filter_sort),
                        style = MaterialTheme.typography.titleSmall
                    )
                    MarketEventsSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = {
                                onSortModeChange(mode)
                                onShowSortPopupChange(false)
                            },
                            label = { Text(stringResource(mode.label), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(
                Res.string.market_events_filter_summary,
                visibleCount,
                watchlistCount,
                lastUpdatedText
            ),
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted
        )
    }
}

@Composable
private fun MarketEventsFilterPanel(
    selectedDay: LocalDate?,
    today: LocalDate,
    tomorrow: LocalDate,
    selectedImpacts: Set<MarketEventImpact>,
    onSelectToday: () -> Unit,
    onSelectTomorrow: () -> Unit,
    onSelectAllDays: () -> Unit,
    onToggleImpact: (MarketEventImpact) -> Unit,
    watchlistOnly: Boolean,
    onToggleWatchlist: () -> Unit,
    availableRegions: Set<MarketRegion>,
    selectedRegions: Set<MarketRegion>,
    onClearRegions: () -> Unit,
    onToggleRegion: (MarketRegion) -> Unit,
    availableEventTypes: Set<MarketEventType>,
    selectedEventTypes: Set<MarketEventType>,
    onClearEventTypes: () -> Unit,
    onToggleEventType: (MarketEventType) -> Unit,
    onClearFilters: () -> Unit
) {
    SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 320.dp, max = 460.dp)) {
        Text(
            text = stringResource(Res.string.stats_filter_title),
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDay == today,
                onClick = onSelectToday,
                label = { Text(stringResource(Res.string.market_events_today), style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = selectedDay == tomorrow,
                onClick = onSelectTomorrow,
                label = { Text(stringResource(Res.string.market_events_tomorrow), style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = selectedDay == null || (selectedDay != today && selectedDay != tomorrow),
                onClick = onSelectAllDays,
                label = { Text(stringResource(Res.string.market_events_all_days), style = MaterialTheme.typography.labelSmall) }
            )
        }

        Text(
            text = stringResource(Res.string.market_events_header_impact),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImpactChip(impact = MarketEventImpact.High, selected = MarketEventImpact.High in selectedImpacts, onToggleImpact = onToggleImpact)
            ImpactChip(impact = MarketEventImpact.Medium, selected = MarketEventImpact.Medium in selectedImpacts, onToggleImpact = onToggleImpact)
            ImpactChip(impact = MarketEventImpact.Low, selected = MarketEventImpact.Low in selectedImpacts, onToggleImpact = onToggleImpact)
            FilterChip(
                selected = watchlistOnly,
                onClick = onToggleWatchlist,
                label = { Text(stringResource(Res.string.market_events_watchlist), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(SnowIcons.Bookmark, contentDescription = null, modifier = Modifier.size(AppIconSize.xs)) }
            )
        }

        Text(
            text = stringResource(Res.string.market_events_all_regions),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedRegions.isEmpty(),
                onClick = onClearRegions,
                label = { Text(stringResource(Res.string.market_events_all_regions), style = MaterialTheme.typography.labelSmall) }
            )
            availableRegions.toList().sortedBy { it.ordinal }.forEach { region ->
                FilterChip(
                    selected = region in selectedRegions,
                    onClick = { onToggleRegion(region) },
                    label = { Text(regionLabel(region), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Text(
            text = stringResource(Res.string.market_events_all_types),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedEventTypes.isEmpty(),
                onClick = onClearEventTypes,
                label = { Text(stringResource(Res.string.market_events_all_types), style = MaterialTheme.typography.labelSmall) }
            )
            availableEventTypes.toList().sortedBy { it.ordinal }.forEach { eventType ->
                FilterChip(
                    selected = eventType in selectedEventTypes,
                    onClick = { onToggleEventType(eventType) },
                    label = { Text(eventTypeLabel(eventType), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        AssistChip(
            onClick = onClearFilters,
            label = { Text(stringResource(Res.string.market_events_all_days), style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(SnowIcons.Close, contentDescription = null, modifier = Modifier.size(AppIconSize.xs)) }
        )
    }
}

@Composable
private fun ImpactChip(
    impact: MarketEventImpact,
    selected: Boolean,
    onToggleImpact: (MarketEventImpact) -> Unit
) {
    val color = when (impact) {
        MarketEventImpact.High -> MaterialTheme.extended.impactHigh
        MarketEventImpact.Medium -> MaterialTheme.extended.impactMedium
        MarketEventImpact.Low -> MaterialTheme.extended.impactLow
    }
    FilterChip(
        selected = selected,
        onClick = { onToggleImpact(impact) },
        label = { Text(impactLabel(impact), style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Dot(color = color) }
    )
}
@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun EventsTableHeader() {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(" ", columnWeight = 0.6f)
        HeaderCell(stringResource(Res.string.market_events_header_country), columnWeight = 1.7f)
        HeaderCell(stringResource(Res.string.market_events_header_event), columnWeight = 3.0f)
        HeaderCell(stringResource(Res.string.market_events_header_type), columnWeight = 1.6f)
        HeaderCell(stringResource(Res.string.market_events_header_date), columnWeight = 1.8f)
        HeaderCell(stringResource(Res.string.market_events_header_consensus), columnWeight = 1.8f)
        HeaderCell(stringResource(Res.string.market_events_header_impact), columnWeight = 1.2f)
        HeaderCell(" ", columnWeight = 0.8f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ext.shellDivider)
    )
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    columnWeight: Float
) {
    val ext = MaterialTheme.extended
    Text(
        text = text,
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = ext.sidebarTextMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DaySeparator(
    date: LocalDate,
    today: LocalDate,
    tomorrow: LocalDate,
    zoneId: ZoneId
) {
    val ext = MaterialTheme.extended
    val prefix = when (date) {
        today -> stringResource(Res.string.market_events_today)
        tomorrow -> stringResource(Res.string.market_events_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern(stringResource(Res.string.market_events_weekday), Locale.GERMANY))
    }
    val formatted = date.format(DateTimeFormatter.ofPattern("dd.MM", Locale.GERMANY))
    Text(
        text = "$prefix | $formatted | ${zoneId.id}",
        style = MaterialTheme.typography.labelSmall,
        color = ext.sidebarTextMuted,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun EventsTableRow(
    event: MarketEvent,
    watchPreference: EventWatchPreference?,
    zoneId: ZoneId,
    now: Instant,
    onToggleWatch: (Boolean) -> Unit,
    onToggleReminder: (Boolean) -> Unit
) {
    val ext = MaterialTheme.extended
    val isWatchlisted = watchPreference != null
    val reminderEnabled = watchPreference?.notifyEnabled == true
    val rowBackground = when {
        isWatchlisted -> ext.tableRowSelected
        event.isMarketMoving -> ext.tableRowHover
        else -> Color.Transparent
    }
    val statusColor = when (event.impact) {
        MarketEventImpact.Low -> ext.impactLow
        MarketEventImpact.Medium -> ext.impactMedium
        MarketEventImpact.High -> ext.impactHigh
    }
    val statusText = when (event.impact) {
        MarketEventImpact.Low -> stringResource(Res.string.market_events_status_low)
        MarketEventImpact.Medium -> stringResource(Res.string.market_events_status_medium)
        MarketEventImpact.High -> stringResource(Res.string.market_events_status_high)
    }
    val dateText = remember(event.scheduledAt, zoneId) {
        val zoned = event.scheduledAt.atZone(zoneId)
        val dayPart = zoned.format(DateTimeFormatter.ofPattern("dd.MM", Locale.GERMANY))
        val timePart = zoned.format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY))
        "$dayPart, $timePart"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .padding(horizontal = 2.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(0.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onToggleWatch(!isWatchlisted) }, modifier = Modifier.size(22.dp)) {
                Icon(
                    imageVector = if (isWatchlisted) SnowIcons.CheckSquare else SnowIcons.Square,
                    contentDescription = null,
                    tint = if (isWatchlisted) ext.watchlist else ext.sidebarTextMuted,
                    modifier = Modifier.size(AppIconSize.xs)
                )
            }
        }

        RegionCell(event = event, columnWeight = 1.7f)
        EventCell(event = event, columnWeight = 3.0f)
        ValueCell(eventTypeLabel(event.type), columnWeight = 1.6f)
        DateCell(text = dateText, relativeText = event.relativeLabel(now), columnWeight = 1.8f)
        ConsensusCell(event = event, columnWeight = 1.8f)
        StatusCell(text = statusText, color = statusColor, columnWeight = 1.2f)
        ActionCell(
            columnWeight = 0.8f,
            watchlisted = isWatchlisted,
            reminderEnabled = reminderEnabled,
            onToggleWatch = { onToggleWatch(!isWatchlisted) },
            onToggleReminder = { onToggleReminder(!reminderEnabled) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ext.shellDivider.copy(alpha = 0.75f))
    )
}

@Composable
private fun RowScope.EventCell(
    event: MarketEvent,
    columnWeight: Float
) {
    val ext = MaterialTheme.extended
    val secondaryText = event.source
        .takeIf { it.isNotBlank() }
        ?: event.description
            ?.takeIf { it.isNotBlank() }
            ?.take(50)
            .orEmpty()

    Column(
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (secondaryText.isNotBlank()) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.labelSmall,
                color = ext.sidebarTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowScope.ValueCell(
    text: String,
    columnWeight: Float
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RowScope.RegionCell(
    event: MarketEvent,
    columnWeight: Float
) {
    Row(
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(event.regionColor()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = event.countryCode.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = regionLabel(event.region),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.ConsensusCell(
    event: MarketEvent,
    columnWeight: Float
) {
    val ext = MaterialTheme.extended
    val emptyValue = stringResource(Res.string.market_events_not_available)
    val primaryText = event.actual?.let { "${stringResource(Res.string.market_events_actual)} $it" }
        ?: "${stringResource(Res.string.market_events_forecast)} ${event.forecast ?: emptyValue}"
    val secondaryText = if (event.actual != null) {
        "${stringResource(Res.string.market_events_forecast)} ${event.forecast ?: emptyValue} | ${stringResource(Res.string.market_events_previous)} ${event.previous ?: emptyValue}"
    } else {
        "${stringResource(Res.string.market_events_previous)} ${event.previous ?: emptyValue}"
    }

    Column(
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = primaryText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = secondaryText,
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.DateCell(
    text: String,
    relativeText: String,
    columnWeight: Float
) {
    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = relativeText,
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.StatusCell(
    text: String,
    color: Color,
    columnWeight: Float
) {
    Row(
        modifier = Modifier
            .weight(columnWeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Dot(color = color)
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RowScope.ActionCell(
    columnWeight: Float,
    watchlisted: Boolean,
    reminderEnabled: Boolean,
    onToggleWatch: () -> Unit,
    onToggleReminder: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(columnWeight)
            .padding(end = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleWatch, modifier = Modifier.size(22.dp)) {
            Icon(
                imageVector = if (watchlisted) SnowIcons.Bookmark else SnowIcons.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(AppIconSize.xs),
                tint = if (watchlisted) MaterialTheme.extended.watchlist else MaterialTheme.extended.sidebarTextMuted
            )
        }
        IconButton(onClick = onToggleReminder, modifier = Modifier.size(22.dp)) {
            Icon(
                imageVector = if (reminderEnabled) SnowIcons.BellActive else SnowIcons.Bell,
                contentDescription = null,
                modifier = Modifier.size(AppIconSize.xs),
                tint = if (reminderEnabled) MaterialTheme.extended.watchlist else MaterialTheme.extended.sidebarTextMuted
            )
        }
        Icon(SnowIcons.Dots, contentDescription = null, modifier = Modifier.size(AppIconSize.xs), tint = MaterialTheme.extended.sidebarTextMuted)
    }
}

@Composable
private fun SkeletonRow(index: Int) {
    val ext = MaterialTheme.extended
    val alpha = if (index % 2 == 0) 0.45f else 0.28f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(8) { cell ->
            val width = when (cell) {
                0 -> 20.dp
                1 -> 78.dp
                2 -> 160.dp
                3 -> 90.dp
                4 -> 96.dp
                5 -> 120.dp
                6 -> 72.dp
                else -> 32.dp
            }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(8.dp)
                    .width(width)
                    .clip(MaterialTheme.shapes.small)
                    .background(ext.tableRowHover.copy(alpha = alpha))
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ext.shellDivider.copy(alpha = 0.65f))
    )
}

@Composable
private fun MessageRow(
    message: String,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = ext.sidebarTextMuted)
    }
}

@Composable
private fun FooterHintRow(
    visibleCount: Int
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.market_events_showing_hint, visibleCount),
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted
        )
    }
}

private enum class MarketEventsSortMode(val label: org.jetbrains.compose.resources.StringResource) {
    TimeAsc(Res.string.market_events_sort_time_asc),
    TimeDesc(Res.string.market_events_sort_time_desc),
    Impact(Res.string.market_events_sort_impact);

    fun next(): MarketEventsSortMode = when (this) {
        TimeAsc -> TimeDesc
        TimeDesc -> Impact
        Impact -> TimeAsc
    }
}

@Composable
private fun MarketEvent.relativeLabel(now: Instant): String {
    val deltaMillis = scheduledAt.toEpochMilli() - now.toEpochMilli()
    val absMinutes = kotlin.math.abs(deltaMillis) / 60_000L
    val hours = absMinutes / 60L
    val minutes = absMinutes % 60L
    val label = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    return if (deltaMillis < 0L) {
        stringResource(Res.string.market_events_relative_ago, label)
    } else {
        stringResource(Res.string.market_events_relative_in, label)
    }
}

@Composable
private fun impactLabel(impact: MarketEventImpact): String = when (impact) {
    MarketEventImpact.Low -> stringResource(Res.string.market_events_status_low)
    MarketEventImpact.Medium -> stringResource(Res.string.market_events_status_medium)
    MarketEventImpact.High -> stringResource(Res.string.market_events_status_high)
}

@Composable
private fun regionLabel(region: MarketRegion): String = when (region) {
    MarketRegion.UnitedStates -> stringResource(Res.string.market_events_region_united_states)
    MarketRegion.EuroArea -> stringResource(Res.string.market_events_region_euro_area)
    MarketRegion.UnitedKingdom -> stringResource(Res.string.market_events_region_united_kingdom)
    MarketRegion.Japan -> stringResource(Res.string.market_events_region_japan)
    MarketRegion.China -> stringResource(Res.string.market_events_region_china)
    MarketRegion.Global -> stringResource(Res.string.market_events_region_global)
}

@Composable
private fun eventTypeLabel(type: MarketEventType): String = when (type) {
    MarketEventType.InterestRate -> stringResource(Res.string.market_events_type_interest_rate)
    MarketEventType.Inflation -> stringResource(Res.string.market_events_type_inflation)
    MarketEventType.Employment -> stringResource(Res.string.market_events_type_employment)
    MarketEventType.Gdp -> stringResource(Res.string.market_events_type_gdp)
    MarketEventType.CentralBank -> stringResource(Res.string.market_events_type_central_bank)
    MarketEventType.Manufacturing -> stringResource(Res.string.market_events_type_manufacturing)
    MarketEventType.ConsumerConfidence -> stringResource(Res.string.market_events_type_consumer_confidence)
    MarketEventType.Other -> stringResource(Res.string.market_events_type_other)
}

@Composable
private fun MarketEvent.regionColor(): Color = when (region) {
    MarketRegion.UnitedStates -> MaterialTheme.extended.regionUnitedStates
    MarketRegion.EuroArea -> MaterialTheme.extended.regionEuroArea
    MarketRegion.UnitedKingdom -> MaterialTheme.extended.regionUnitedKingdom
    MarketRegion.Japan -> MaterialTheme.extended.regionJapan
    MarketRegion.China -> MaterialTheme.extended.regionChina
    MarketRegion.Global -> MaterialTheme.extended.regionGlobal
}

