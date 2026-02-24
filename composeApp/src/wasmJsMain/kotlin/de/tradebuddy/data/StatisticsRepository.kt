package de.tradebuddy.data

import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.MoveDirection
import de.tradebuddy.domain.model.StatEntry
import de.tradebuddy.logging.AppLog
import java.time.Instant
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StatisticsRepository {
    suspend fun loadEntries(): List<StatEntry>
    suspend fun addEntry(entry: StatEntry)
    suspend fun updateEntry(entry: StatEntry)
    suspend fun deleteEntry(entryId: String)
    suspend fun resetEntries()
}

class FileStatisticsRepository(
    appName: String = "TradeBuddy",
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : StatisticsRepository {

    private val storageKey = "${appName}.stats.v1"

    override suspend fun loadEntries(): List<StatEntry> = withContext(dispatcher) {
        readEntries()
    }

    override suspend fun addEntry(entry: StatEntry) {
        withContext(dispatcher) {
            runCatching {
                val updated = readEntries() + entry
                writeEntries(updated)
            }.onFailure { error ->
                AppLog.error(
                    tag = "StatisticsRepository",
                    message = "Failed to append stat entry in browser storage",
                    throwable = error
                )
            }.getOrElse { }
        }
    }

    override suspend fun updateEntry(entry: StatEntry) {
        withContext(dispatcher) {
            runCatching {
                val updated = readEntries().map { existing ->
                    if (existing.id == entry.id) entry else existing
                }
                writeEntries(updated)
            }.onFailure { error ->
                AppLog.error(
                    tag = "StatisticsRepository",
                    message = "Failed to update stat entry ${entry.id} in browser storage",
                    throwable = error
                )
            }.getOrElse { }
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        withContext(dispatcher) {
            runCatching {
                val updated = readEntries().filterNot { it.id == entryId }
                writeEntries(updated)
            }.onFailure { error ->
                AppLog.error(
                    tag = "StatisticsRepository",
                    message = "Failed to delete stat entry $entryId in browser storage",
                    throwable = error
                )
            }.getOrElse { }
        }
    }

    override suspend fun resetEntries() {
        withContext(dispatcher) {
            runCatching { window.localStorage.removeItem(storageKey) }
                .onFailure { error ->
                    AppLog.error(
                        tag = "StatisticsRepository",
                        message = "Failed to reset stats in browser storage",
                        throwable = error
                    )
                }
                .getOrElse { }
        }
    }

    private fun readEntries(): List<StatEntry> {
        val raw = window.localStorage.getItem(storageKey) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            raw
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull(::parseLine)
                .toList()
        }.onFailure { error ->
            AppLog.error(
                tag = "StatisticsRepository",
                message = "Corrupt stats payload in browser storage - resetting",
                throwable = error
            )
            runCatching { window.localStorage.removeItem(storageKey) }
        }.getOrElse { emptyList() }
    }

    private fun writeEntries(entries: List<StatEntry>) {
        if (entries.isEmpty()) {
            window.localStorage.removeItem(storageKey)
            return
        }
        val payload = entries.joinToString("\n") { serializeLine(it) }
        window.localStorage.setItem(storageKey, payload)
    }

    private fun serializeLine(entry: StatEntry): String =
        listOf(
            escape(entry.id),
            escape(entry.cityLabel),
            escape(entry.cityKey),
            entry.eventType.name,
            entry.eventInstant.toEpochMilli().toString(),
            entry.direction.name,
            entry.offsetMinutes.toString(),
            entry.createdAt.toEpochMilli().toString()
        ).joinToString("|")

    private fun parseLine(line: String): StatEntry? {
        val parts = splitEscaped(line, '|')
        if (parts.size < 8) return null
        val eventType = runCatching { CompactEventType.valueOf(parts[3]) }.getOrNull() ?: return null
        val eventInstant = runCatching { Instant.ofEpochMilli(parts[4].toLong()) }.getOrNull() ?: return null
        val direction = runCatching { MoveDirection.valueOf(parts[5]) }.getOrNull() ?: return null
        val offsetMinutes = parts[6].toIntOrNull() ?: return null
        val createdAt = runCatching { Instant.ofEpochMilli(parts[7].toLong()) }.getOrNull() ?: return null
        return StatEntry(
            id = unescape(parts[0]),
            cityLabel = unescape(parts[1]),
            cityKey = unescape(parts[2]),
            eventType = eventType,
            eventInstant = eventInstant,
            direction = direction,
            offsetMinutes = offsetMinutes,
            createdAt = createdAt
        )
    }
}

private fun escape(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("|", "\\|")
        .replace("\n", "\\n")

private fun unescape(value: String): String {
    val out = StringBuilder()
    var escaped = false
    value.forEach { ch ->
        if (escaped) {
            out.append(
                when (ch) {
                    'n' -> '\n'
                    else -> ch
                }
            )
            escaped = false
        } else if (ch == '\\') {
            escaped = true
        } else {
            out.append(ch)
        }
    }
    if (escaped) out.append('\\')
    return out.toString()
}

private fun splitEscaped(value: String, separator: Char): List<String> {
    val out = ArrayList<String>()
    val current = StringBuilder()
    var escaped = false
    value.forEach { ch ->
        if (escaped) {
            current.append(ch)
            escaped = false
        } else if (ch == '\\') {
            escaped = true
        } else if (ch == separator) {
            out += current.toString()
            current.clear()
        } else {
            current.append(ch)
        }
    }
    if (escaped) current.append('\\')
    out += current.toString()
    return out
}
