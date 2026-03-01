package de.tradebuddy.domain.model

import java.time.Instant

enum class TaskColumn(val label: String) {
    Backlog("Backlog"),
    InProgress("In Arbeit"),
    Completed("Erledigt")
}

enum class TaskPriority(val label: String, val weight: Int) {
    Low("Niedrig", 1),
    Medium("Mittel", 2),
    High("Hoch", 3)
}

data class TaskItem(
    val id: String,
    val title: String,
    val summary: String,
    val label: String? = null,
    val priority: TaskPriority = TaskPriority.Medium,
    val column: TaskColumn = TaskColumn.Backlog,
    val dueDate: String? = null,
    val assignee: String? = null,
    val commentsCount: Int = 0,
    val attachmentsCount: Int = 0,
    val orderInColumn: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class TasksBoard(
    val tasks: List<TaskItem>,
    val updatedAt: Instant
)
