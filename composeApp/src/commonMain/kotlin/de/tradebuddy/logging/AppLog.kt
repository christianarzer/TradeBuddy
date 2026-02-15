package de.tradebuddy.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppLogLevel {
    Info,
    Warn,
    Error
}

data class AppLogEntry(
    val timestampMillis: Long,
    val level: AppLogLevel,
    val tag: String,
    val message: String,
    val details: String? = null
)

object AppLog {
    private const val MaxEntries = 400

    private val _entries = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val entries: StateFlow<List<AppLogEntry>> = _entries.asStateFlow()

    fun info(tag: String, message: String) {
        append(
            level = AppLogLevel.Info,
            tag = tag,
            message = message,
            details = null
        )
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        append(
            level = AppLogLevel.Warn,
            tag = tag,
            message = message,
            details = throwable?.message ?: throwable?.toString()
        )
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        append(
            level = AppLogLevel.Error,
            tag = tag,
            message = message,
            details = throwable?.message ?: throwable?.toString()
        )
    }

    fun clear() {
        _entries.value = emptyList()
    }

    private fun append(
        level: AppLogLevel,
        tag: String,
        message: String,
        details: String?
    ) {
        val entry = AppLogEntry(
            timestampMillis = nowEpochMillis(),
            level = level,
            tag = tag,
            message = message,
            details = details
        )

        _entries.update { current ->
            val tail = if (current.size >= MaxEntries) {
                current.takeLast(MaxEntries - 1)
            } else {
                current
            }
            tail + entry
        }
    }
}
