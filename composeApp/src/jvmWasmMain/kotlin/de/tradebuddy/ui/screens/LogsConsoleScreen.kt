package de.tradebuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tradebuddy.logging.AppLog
import de.tradebuddy.logging.AppLogEntry
import de.tradebuddy.logging.AppLogLevel
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.rememberCopyTextToClipboard
import de.tradebuddy.ui.components.shared.SnowToolbar
import de.tradebuddy.ui.components.shared.SnowToolbarIconButton
import de.tradebuddy.ui.components.shared.SnowToolbarPopupPanel
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.AppIconSize
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.tradebuddy.ui.theme.extended
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.settings_logs_clear
import trade_buddy.composeapp.generated.resources.settings_logs_copy
import trade_buddy.composeapp.generated.resources.settings_logs_desc
import trade_buddy.composeapp.generated.resources.settings_logs_empty
import trade_buddy.composeapp.generated.resources.settings_logs_filter_all
import trade_buddy.composeapp.generated.resources.settings_logs_filter_error
import trade_buddy.composeapp.generated.resources.settings_logs_filter_info
import trade_buddy.composeapp.generated.resources.settings_logs_filter_sort
import trade_buddy.composeapp.generated.resources.settings_logs_filter_warn
import trade_buddy.composeapp.generated.resources.settings_logs_sort_newest
import trade_buddy.composeapp.generated.resources.settings_logs_sort_oldest
import trade_buddy.composeapp.generated.resources.settings_logs_status_summary
import trade_buddy.composeapp.generated.resources.settings_logs_title

@Composable
fun LogsConsoleScreen(
    @Suppress("UNUSED_PARAMETER") viewModel: SunMoonViewModel,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val entries by AppLog.entries.collectAsState()
    val copyToClipboard = rememberCopyTextToClipboard()
    var levelFilter by rememberSaveable { mutableStateOf<AppLogLevel?>(null) }
    var sortMode by rememberSaveable { mutableStateOf(LogsSortMode.Newest) }
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSortPopup by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(entries, searchQuery, levelFilter, sortMode) {
        val ordered = when (sortMode) {
            LogsSortMode.Newest -> entries.asReversed()
            LogsSortMode.Oldest -> entries
        }
        ordered.filter { entry ->
            val levelMatches = levelFilter?.let { entry.level == it } ?: true
            val queryNormalized = searchQuery.trim()
            val queryMatches = if (queryNormalized.isBlank()) {
                true
            } else {
                val haystack = buildString {
                    append(entry.tag)
                    append(' ')
                    append(entry.message)
                    append(' ')
                    append(entry.details.orEmpty())
                }
                haystack.contains(queryNormalized, ignoreCase = true)
            }
            levelMatches && queryMatches
        }
    }

    fun copyLogs(rows: List<AppLogEntry>) {
        val payload = buildString {
            rows.forEach { entry ->
                append(formatLogEntry(entry))
                if (!entry.details.isNullOrBlank()) {
                    append('\n')
                    append(entry.details)
                }
                append("\n\n")
            }
        }.trim()
        if (payload.isNotEmpty()) {
            copyToClipboard(payload)
        }
    }

    val ext = MaterialTheme.extended
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.settings_logs_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    stringResource(Res.string.settings_logs_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SnowToolbar {
            Box {
                SnowToolbarIconButton(icon = SnowIcons.Filter, onClick = { showFilterPopup = true })
                DropdownMenu(
                    expanded = showFilterPopup,
                    onDismissRequest = { showFilterPopup = false }
                ) {
                    SnowToolbarPopupPanel {
                        FilterChip(
                            selected = levelFilter == null,
                            onClick = { levelFilter = null },
                            label = { Text(stringResource(Res.string.settings_logs_filter_all)) }
                        )
                        FilterChip(
                            selected = levelFilter == AppLogLevel.Info,
                            onClick = { levelFilter = AppLogLevel.Info },
                            label = { Text(stringResource(Res.string.settings_logs_filter_info)) }
                        )
                        FilterChip(
                            selected = levelFilter == AppLogLevel.Warn,
                            onClick = { levelFilter = AppLogLevel.Warn },
                            label = { Text(stringResource(Res.string.settings_logs_filter_warn)) }
                        )
                        FilterChip(
                            selected = levelFilter == AppLogLevel.Error,
                            onClick = { levelFilter = AppLogLevel.Error },
                            label = { Text(stringResource(Res.string.settings_logs_filter_error)) }
                        )
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
                            text = stringResource(Res.string.settings_logs_filter_sort),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FilterChip(
                            selected = sortMode == LogsSortMode.Newest,
                            onClick = {
                                sortMode = LogsSortMode.Newest
                                showSortPopup = false
                            },
                            label = { Text(stringResource(Res.string.settings_logs_sort_newest)) }
                        )
                        FilterChip(
                            selected = sortMode == LogsSortMode.Oldest,
                            onClick = {
                                sortMode = LogsSortMode.Oldest
                                showSortPopup = false
                            },
                            label = { Text(stringResource(Res.string.settings_logs_sort_oldest)) }
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { copyLogs(filtered) },
                enabled = filtered.isNotEmpty()
            ) {
                Icon(SnowIcons.Copy, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(Res.string.settings_logs_copy))
            }
            OutlinedButton(
                onClick = {
                    onSearchQueryChange("")
                    AppLog.clear()
                },
                enabled = entries.isNotEmpty()
            ) {
                Text(stringResource(Res.string.settings_logs_clear))
            }
        }

        val filterLabel = when (levelFilter) {
            AppLogLevel.Info -> stringResource(Res.string.settings_logs_filter_info)
            AppLogLevel.Warn -> stringResource(Res.string.settings_logs_filter_warn)
            AppLogLevel.Error -> stringResource(Res.string.settings_logs_filter_error)
            null -> stringResource(Res.string.settings_logs_filter_all)
        }
        val sortLabel = when (sortMode) {
            LogsSortMode.Newest -> stringResource(Res.string.settings_logs_sort_newest)
            LogsSortMode.Oldest -> stringResource(Res.string.settings_logs_sort_oldest)
        }
        Text(
            text = stringResource(Res.string.settings_logs_status_summary, filterLabel, sortLabel, filtered.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (filtered.isEmpty()) {
            Text(
                stringResource(Res.string.settings_logs_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(filtered) { index, entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            formatLogEntry(entry),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!entry.details.isNullOrBlank()) {
                            Text(
                                entry.details,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < filtered.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 1.dp)
                                .background(ext.shellDivider)
                        )
                    }
                }
            }
        }
    }

}

private enum class LogsSortMode {
    Newest,
    Oldest
}

private fun formatLogEntry(entry: AppLogEntry): String {
    val timestamp = Instant.ofEpochMilli(entry.timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(LogTimeFormatter)
    return "$timestamp [${entry.level.name}] ${entry.tag}: ${entry.message}"
}

private val LogTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)



