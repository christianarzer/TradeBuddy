package de.tradebuddy.data

import de.tradebuddy.domain.model.TaskColumn
import de.tradebuddy.domain.model.TaskItem
import de.tradebuddy.domain.model.TaskPriority
import de.tradebuddy.domain.model.TasksBoard
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface TasksRepository {
    suspend fun loadBoard(): TasksBoard
    suspend fun saveTask(task: TaskItem)
    suspend fun deleteTask(taskId: String)
    suspend fun moveTask(taskId: String, column: TaskColumn, toIndex: Int? = null)
    suspend fun reorderTask(taskId: String, column: TaskColumn, toIndex: Int)
    suspend fun reset()
}

class DefaultTasksRepository(
    private val document: StorageDocument
) : TasksRepository {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    override suspend fun loadBoard(): TasksBoard = readPayload().toDomain()

    override suspend fun saveTask(task: TaskItem) {
        mutate { payload ->
            val existing = payload.tasks.firstOrNull { it.id == task.id }
            val cleaned = payload.tasks.filterNot { it.id == task.id }
            val resolvedOrder = when {
                existing == null -> nextOrderForColumn(cleaned, task.column)
                existing.column != task.column.name -> nextOrderForColumn(cleaned, task.column)
                task.orderInColumn > 0 -> task.orderInColumn
                existing.orderInColumn > 0 -> existing.orderInColumn
                else -> nextOrderForColumn(cleaned, task.column)
            }

            val merged = cleaned
                .plus(task.copy(orderInColumn = resolvedOrder).toDto())
                .normalizeColumnOrders()
            payload.copy(tasks = merged)
        }
    }

    override suspend fun deleteTask(taskId: String) {
        mutate { payload ->
            payload.copy(tasks = payload.tasks.filterNot { it.id == taskId })
        }
    }

    override suspend fun moveTask(taskId: String, column: TaskColumn, toIndex: Int?) {
        mutate { payload ->
            val now = Instant.now().toEpochMilli()
            val current = payload.tasks.firstOrNull { it.id == taskId } ?: return@mutate payload
            val cleaned = payload.tasks.filterNot { it.id == taskId }
            val moved = current.copy(
                column = column.name,
                updatedAtEpochMillis = now,
                orderInColumn = 0
            )

            val destination = cleaned
                .filter { it.column == column.name }
                .sortedWithinColumn()
                .toMutableList()
            val resolvedIndex = toIndex?.coerceIn(0, destination.size) ?: destination.size
            destination.add(resolvedIndex, moved)
            val reorderedDestination = destination.mapIndexed { index, dto ->
                dto.copy(orderInColumn = (index + 1) * OrderStep)
            }
            val others = cleaned.filter { it.column != column.name }
            payload.copy(tasks = (others + reorderedDestination).normalizeColumnOrders())
        }
    }

    override suspend fun reorderTask(taskId: String, column: TaskColumn, toIndex: Int) {
        mutate { payload ->
            val currentColumn = payload.tasks
                .filter { it.column == column.name }
                .sortedWithinColumn()
                .toMutableList()
            val fromIndex = currentColumn.indexOfFirst { it.id == taskId }
            if (fromIndex == -1) return@mutate payload

            val moved = currentColumn.removeAt(fromIndex).copy(
                column = column.name,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
            val targetIndex = toIndex.coerceIn(0, currentColumn.size)
            currentColumn.add(targetIndex, moved)

            val reordered = currentColumn.mapIndexed { index, dto ->
                dto.copy(orderInColumn = (index + 1) * OrderStep)
            }
            val others = payload.tasks.filterNot { it.column == column.name }
            payload.copy(tasks = others.plus(reordered).normalizeColumnOrders())
        }
    }

    override suspend fun reset() {
        document.clear()
    }

    private suspend fun mutate(transform: (TasksPayload) -> TasksPayload) {
        val current = readPayload()
        val changed = transform(current).withUpdatedTimestamp()
        document.writeText(json.encodeToString(TasksPayload.serializer(), changed))
    }

    private suspend fun readPayload(): TasksPayload {
        val raw = document.readText() ?: return TasksPayload()
        return runCatching { json.decodeFromString(TasksPayload.serializer(), raw) }
            .getOrElse { TasksPayload() }
    }

    private fun TasksPayload.withUpdatedTimestamp(): TasksPayload = copy(
        updatedAtEpochMillis = Instant.now().toEpochMilli()
    )
}

@Serializable
private data class TasksPayload(
    val schemaVersion: Int = 1,
    val updatedAtEpochMillis: Long = Instant.now().toEpochMilli(),
    val tasks: List<TaskItemDto> = emptyList()
)

@Serializable
private data class TaskItemDto(
    val id: String,
    val title: String,
    val summary: String,
    val label: String? = null,
    val priority: String = TaskPriority.Medium.name,
    val column: String = TaskColumn.Backlog.name,
    val dueDate: String? = null,
    val assignee: String? = null,
    val commentsCount: Int = 0,
    val attachmentsCount: Int = 0,
    val orderInColumn: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

private fun TasksPayload.toDomain(): TasksBoard = TasksBoard(
    tasks = tasks
        .normalizeColumnOrders()
        .mapNotNull { it.toDomainOrNull() },
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)

private fun TaskItem.toDto(): TaskItemDto = TaskItemDto(
    id = id,
    title = title,
    summary = summary,
    label = label,
    priority = priority.name,
    column = column.name,
    dueDate = dueDate,
    assignee = assignee,
    commentsCount = commentsCount,
    attachmentsCount = attachmentsCount,
    orderInColumn = orderInColumn,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

private fun TaskItemDto.toDomainOrNull(): TaskItem? {
    val resolvedPriority = runCatching { TaskPriority.valueOf(priority) }.getOrNull() ?: return null
    val resolvedColumn = runCatching { TaskColumn.valueOf(column) }.getOrNull() ?: return null
    return TaskItem(
        id = id,
        title = title,
        summary = summary,
        label = label,
        priority = resolvedPriority,
        column = resolvedColumn,
        dueDate = dueDate,
        assignee = assignee,
        commentsCount = commentsCount,
        attachmentsCount = attachmentsCount,
        orderInColumn = orderInColumn,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
    )
}

private fun nextOrderForColumn(tasks: List<TaskItemDto>, column: TaskColumn): Int {
    val currentMax = tasks
        .asSequence()
        .filter { it.column == column.name }
        .map { it.orderInColumn }
        .maxOrNull()
        ?: 0
    return currentMax + OrderStep
}

private fun List<TaskItemDto>.normalizeColumnOrders(): List<TaskItemDto> {
    val knownColumns = TaskColumn.entries
    val normalized = buildList {
        knownColumns.forEach { column ->
            val forColumn = this@normalizeColumnOrders
                .filter { it.column == column.name }
                .sortedWithinColumn()
                .mapIndexed { index, dto ->
                    dto.copy(orderInColumn = (index + 1) * OrderStep)
                }
            addAll(forColumn)
        }
        val unknown = this@normalizeColumnOrders
            .filter { dto -> knownColumns.none { it.name == dto.column } }
            .sortedWithinColumn()
            .mapIndexed { index, dto ->
                dto.copy(orderInColumn = (index + 1) * OrderStep)
            }
        addAll(unknown)
    }
    return normalized.sortedWith(
        compareBy<TaskItemDto> { taskColumnSortOrder(it.column) }
            .thenBy { it.orderInColumn }
            .thenByDescending { it.updatedAtEpochMillis }
            .thenBy { it.title.lowercase() }
    )
}

private fun List<TaskItemDto>.sortedWithinColumn(): List<TaskItemDto> = sortedWith(
    compareBy<TaskItemDto> { if (it.orderInColumn > 0) 0 else 1 }
        .thenBy { if (it.orderInColumn > 0) it.orderInColumn else Int.MAX_VALUE }
        .thenByDescending { it.updatedAtEpochMillis }
        .thenBy { it.title.lowercase() }
)

private fun taskColumnSortOrder(name: String): Int =
    runCatching { TaskColumn.valueOf(name).ordinal }.getOrDefault(Int.MAX_VALUE)

private const val OrderStep: Int = 1_000
