package de.tradebuddy.presentation

import de.tradebuddy.data.TasksRepository
import de.tradebuddy.domain.model.TaskColumn
import de.tradebuddy.domain.model.TaskItem
import de.tradebuddy.domain.model.TaskPriority
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repository: TasksRepository
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var boardTasks: List<TaskItem> = emptyList()

    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadBoard() }
                .onSuccess { board ->
                    boardTasks = board.tasks
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            tasks = boardTasks.applySearch(current.searchQuery),
                            updatedAt = board.updatedAt
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Aufgaben konnten nicht geladen werden"
                        )
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            current.copy(
                searchQuery = query,
                tasks = boardTasks.applySearch(query)
            )
        }
    }

    fun createTask(
        title: String,
        summary: String,
        label: String?,
        priority: TaskPriority,
        column: TaskColumn,
        dueDate: String?,
        assignee: String?
    ) {
        if (title.isBlank()) return
        scope.launch {
            val now = Instant.now()
            repository.saveTask(
                TaskItem(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    summary = summary.trim(),
                    label = label?.trim().takeUnless { it.isNullOrEmpty() },
                    priority = priority,
                    column = column,
                    dueDate = dueDate,
                    assignee = assignee?.trim().takeUnless { it.isNullOrEmpty() },
                    commentsCount = 0,
                    attachmentsCount = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )
            refresh()
        }
    }

    fun updateTask(task: TaskItem) {
        scope.launch {
            repository.saveTask(task.copy(updatedAt = Instant.now()))
            refresh()
        }
    }

    fun moveTask(taskId: String, to: TaskColumn, toIndex: Int? = null) {
        scope.launch {
            repository.moveTask(taskId, to, toIndex)
            refresh()
        }
    }

    fun reorderTask(taskId: String, column: TaskColumn, toIndex: Int) {
        scope.launch {
            repository.reorderTask(taskId, column, toIndex)
            refresh()
        }
    }

    fun deleteTask(taskId: String) {
        scope.launch {
            repository.deleteTask(taskId)
            refresh()
        }
    }

    fun reset() {
        scope.launch {
            repository.reset()
            refresh()
        }
    }

    override fun close() {
        scope.cancel()
    }
}

private fun List<TaskItem>.applySearch(query: String): List<TaskItem> {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return this
    return filter { task ->
        task.title.lowercase().contains(normalized) ||
            task.summary.lowercase().contains(normalized) ||
            (task.label?.lowercase()?.contains(normalized) == true) ||
            (task.assignee?.lowercase()?.contains(normalized) == true)
    }
}
