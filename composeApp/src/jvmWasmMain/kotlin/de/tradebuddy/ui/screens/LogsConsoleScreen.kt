package de.tradebuddy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import de.tradebuddy.presentation.AppScreen
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.ui.components.rememberCopyTextToClipboard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.settings_logs_back
import trade_buddy.composeapp.generated.resources.settings_logs_clear
import trade_buddy.composeapp.generated.resources.settings_logs_copy
import trade_buddy.composeapp.generated.resources.settings_logs_desc
import trade_buddy.composeapp.generated.resources.settings_logs_empty
import trade_buddy.composeapp.generated.resources.settings_logs_filter_all
import trade_buddy.composeapp.generated.resources.settings_logs_filter_error
import trade_buddy.composeapp.generated.resources.settings_logs_filter_info
import trade_buddy.composeapp.generated.resources.settings_logs_filter_warn
import trade_buddy.composeapp.generated.resources.settings_logs_search
import trade_buddy.composeapp.generated.resources.settings_logs_title

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogsConsoleScreen(
    viewModel: SunMoonViewModel
) {
    val entries by AppLog.entries.collectAsState()
    val copyToClipboard = rememberCopyTextToClipboard()
    var query by rememberSaveable { mutableStateOf("") }
    var levelFilter by rememberSaveable { mutableStateOf<AppLogLevel?>(null) }

    val filtered = remember(entries, query, levelFilter) {
        entries.asReversed().filter { entry ->
            val levelMatches = levelFilter?.let { entry.level == it } ?: true
            val queryNormalized = query.trim()
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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
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
                    OutlinedButton(onClick = { viewModel.setScreen(AppScreen.Settings) }) {
                        Text(stringResource(Res.string.settings_logs_back))
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    label = { Text(stringResource(Res.string.settings_logs_search)) }
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { copyLogs(filtered) },
                        enabled = filtered.isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.settings_logs_copy))
                    }
                    OutlinedButton(
                        onClick = AppLog::clear,
                        enabled = entries.isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.settings_logs_clear))
                    }
                }
            }
        }

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
                    .heightIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { entry ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    }
                }
            }
        }
    }
}

private fun formatLogEntry(entry: AppLogEntry): String {
    val timestamp = Instant.ofEpochMilli(entry.timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(LogTimeFormatter)
    return "$timestamp [${entry.level.name}] ${entry.tag}: ${entry.message}"
}

private val LogTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

