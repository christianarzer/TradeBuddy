package de.tradebuddy.data

import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.MoveDirection
import de.tradebuddy.domain.model.StatEntry
import de.tradebuddy.logging.AppLog
import java.io.File
import java.time.Instant
import java.util.Base64
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
    private val appName: String = "TradeBuddy",
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : StatisticsRepository {

    private val statsPath: File = AppStoragePaths.statsPath(appName)
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    override suspend fun loadEntries(): List<StatEntry> = withContext(dispatcher) {
        readEntries()
    }

    override suspend fun addEntry(entry: StatEntry) {
        withContext(dispatcher) {
            runCatching {
                statsPath.parentFile?.mkdirs()
                val line = serializeLine(entry)
                statsPath.appendText(line + System.lineSeparator(), Charsets.UTF_8)
            }.onFailure { error ->
                AppLog.error(
                    tag = "StatisticsRepository",
                    message = "Failed to append stat entry to ${statsPath.absolutePath}",
                    throwable = error
                )
            }.getOrElse { }
        }
    }

    override suspend fun updateEntry(entry: StatEntry) {
        withContext(dispatcher) {
            runCatching {
                val current = readEntries()
                if (current.isEmpty()) return@runCatching
                val updated = current.map { existing ->
                    if (existing.id == entry.id) entry else existing
                }
                if (updated == current) return@runCatching
                writeEntries(updated)
            }.onFailure { error ->
                AppLog.error(
                    tag = "StatisticsRepository",
                    message = "Failed to update stat entry ${entry.id}",
                    throwable = error
                )
            }.getOrElse { }
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        withContext(dispatcher) {
            runCatching {
                val current = readEntries()
                if (current.isEmpty()) return@runCatching
                val updated = current.filterNot { it.id == entryId }
                if (updated.size == current.size) return@runCatching
                writeEntries(updated)
            }.onFailure { error ->
                AppLog.error(
                    tag = "StatisticsRepository",
                    message = "Failed to delete stat entry $entryId",
                    throwable = error
                )
            }.getOrElse { }
        }
    }

    override suspend fun resetEntries() {
        withContext(dispatcher) {
            runCatching { statsPath.delete() }
                .onFailure { error ->
                    AppLog.error(
                        tag = "StatisticsRepository",
                        message = "Failed to reset stats file ${statsPath.absolutePath}",
                        throwable = error
                    )
                }
                .getOrElse { }
        }
    }

    private fun serializeLine(entry: StatEntry): String {
        val cityLabel = encode(entry.cityLabel)
        val cityKey = encode(entry.cityKey)
        return listOf(
            entry.id,
            cityLabel,
            cityKey,
            entry.eventType.name,
            entry.eventInstant.toEpochMilli().toString(),
            entry.direction.name,
            entry.offsetMinutes.toString(),
            entry.createdAt.toEpochMilli().toString()
        ).joinToString("|")
    }

    private fun parseLine(line: String): StatEntry? {
        val parts = line.split('|')
        if (parts.size < 8) return null
        val id = parts[0]
        val cityLabel = decode(parts[1])
        val cityKey = decode(parts[2])
        val eventType = runCatching { CompactEventType.valueOf(parts[3]) }.getOrNull() ?: return null
        val eventInstant = runCatching { Instant.ofEpochMilli(parts[4].toLong()) }.getOrNull() ?: return null
        val direction = runCatching { MoveDirection.valueOf(parts[5]) }.getOrNull() ?: return null
        val offsetMinutes = parts[6].toIntOrNull() ?: return null
        val createdAt = runCatching { Instant.ofEpochMilli(parts[7].toLong()) }.getOrNull() ?: return null
        return StatEntry(
            id = id,
            cityLabel = cityLabel,
            cityKey = cityKey,
            eventType = eventType,
            eventInstant = eventInstant,
            direction = direction,
            offsetMinutes = offsetMinutes,
            createdAt = createdAt
        )
    }

    private fun encode(value: String): String =
        encoder.encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decode(value: String): String =
        runCatching { String(decoder.decode(value), Charsets.UTF_8) }.getOrDefault(value)

    private fun readEntries(): List<StatEntry> {
        if (!statsPath.exists()) return emptyList()
        return runCatching {
            statsPath.readLines(Charsets.UTF_8)
                .filter { it.isNotBlank() }
                .mapNotNull(::parseLine)
        }.onFailure { error ->
            AppLog.error(
                tag = "StatisticsRepository",
                message = "Failed to read stats from ${statsPath.absolutePath}",
                throwable = error
            )
        }.getOrElse { emptyList() }
    }

    private fun writeEntries(entries: List<StatEntry>) {
        if (entries.isEmpty()) {
            statsPath.delete()
            return
        }
        statsPath.parentFile?.mkdirs()
        val content = entries.joinToString(System.lineSeparator()) { serializeLine(it) } +
            System.lineSeparator()
        statsPath.writeText(content, Charsets.UTF_8)
    }

}

