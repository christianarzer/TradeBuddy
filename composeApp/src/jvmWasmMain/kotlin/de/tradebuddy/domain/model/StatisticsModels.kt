package de.tradebuddy.domain.model

import java.time.Instant

enum class MoveDirection {
    Up,
    Down
}

data class StatEntry(
    val id: String,
    val cityLabel: String,
    val cityKey: String,
    val eventType: CompactEventType,
    val eventInstant: Instant,
    val direction: MoveDirection,
    val offsetMinutes: Int,
    val createdAt: Instant
)

