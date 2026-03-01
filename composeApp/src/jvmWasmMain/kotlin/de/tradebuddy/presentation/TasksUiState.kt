package de.tradebuddy.presentation

import de.tradebuddy.domain.model.TaskColumn
import de.tradebuddy.domain.model.TaskItem
import java.time.Instant

data class TasksUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val tasks: List<TaskItem> = emptyList(),
    val searchQuery: String = "",
    val updatedAt: Instant? = null
) {
    val groupedByColumn: Map<TaskColumn, List<TaskItem>>
        get() = TaskColumn.entries.associateWith { column ->
            tasks.filter { it.column == column }
        }
}

