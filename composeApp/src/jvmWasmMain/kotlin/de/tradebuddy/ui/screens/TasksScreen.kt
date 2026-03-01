package de.tradebuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.tradebuddy.domain.model.TaskColumn
import de.tradebuddy.domain.model.TaskItem
import de.tradebuddy.domain.model.TaskPriority
import de.tradebuddy.presentation.TasksUiState
import de.tradebuddy.presentation.TasksViewModel
import de.tradebuddy.ui.components.SnowTabItem
import de.tradebuddy.ui.components.SnowTabs
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.AppIconSize
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.extended
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.datepicker_cancel
import trade_buddy.composeapp.generated.resources.datepicker_ok
import trade_buddy.composeapp.generated.resources.tasks_action_add_target
import trade_buddy.composeapp.generated.resources.tasks_column_backlog
import trade_buddy.composeapp.generated.resources.tasks_column_completed
import trade_buddy.composeapp.generated.resources.tasks_column_in_progress
import trade_buddy.composeapp.generated.resources.tasks_create
import trade_buddy.composeapp.generated.resources.tasks_editor_assignee
import trade_buddy.composeapp.generated.resources.tasks_editor_due_date
import trade_buddy.composeapp.generated.resources.tasks_editor_edit
import trade_buddy.composeapp.generated.resources.tasks_editor_label
import trade_buddy.composeapp.generated.resources.tasks_editor_new
import trade_buddy.composeapp.generated.resources.tasks_editor_priority_high
import trade_buddy.composeapp.generated.resources.tasks_editor_priority_low
import trade_buddy.composeapp.generated.resources.tasks_editor_priority_medium
import trade_buddy.composeapp.generated.resources.tasks_editor_summary
import trade_buddy.composeapp.generated.resources.tasks_editor_title
import trade_buddy.composeapp.generated.resources.tasks_error
import trade_buddy.composeapp.generated.resources.tasks_no_tasks
import trade_buddy.composeapp.generated.resources.tasks_save
import trade_buddy.composeapp.generated.resources.tasks_tab_overview

private enum class TaskBoardTab {
    Overview
}

@Composable
fun TasksScreen(state: TasksUiState, viewModel: TasksViewModel) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m)
    ) {
        TasksTabs()
        TasksToolbar(
            onAddTask = {
                editingTask = null
                showEditor = true
            }
        )
        state.errorMessage?.let { message ->
            Text(
                text = stringResource(Res.string.tasks_error, message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extended.negative
            )
        }
        BoardColumns(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = state,
            onReorderTask = viewModel::reorderTask,
            onMoveTask = { taskId, targetColumn, targetIndex ->
                viewModel.moveTask(taskId = taskId, to = targetColumn, toIndex = targetIndex)
            },
            onDeleteTask = viewModel::deleteTask,
            onEditTask = {
                editingTask = it
                showEditor = true
            }
        )
    }

    if (showEditor) {
        TaskEditorDialog(
            initialTask = editingTask,
            onDismiss = {
                showEditor = false
                editingTask = null
            },
            onSave = { payload ->
                val existing = editingTask
                if (existing == null) {
                    viewModel.createTask(
                        title = payload.title,
                        summary = payload.summary,
                        label = payload.label,
                        priority = payload.priority,
                        column = payload.column,
                        dueDate = payload.dueDateIso,
                        assignee = payload.assignee
                    )
                } else {
                    viewModel.updateTask(
                        existing.copy(
                            title = payload.title,
                            summary = payload.summary,
                            label = payload.label,
                            priority = payload.priority,
                            column = payload.column,
                            dueDate = payload.dueDateIso,
                            assignee = payload.assignee
                        )
                    )
                }
                showEditor = false
                editingTask = null
            }
        )
    }
}

@Composable
private fun TasksTabs(
) {
    val tabs = listOf(
        SnowTabItem(TaskBoardTab.Overview.name, stringResource(Res.string.tasks_tab_overview))
    )
    SnowTabs(
        items = tabs,
        selectedId = TaskBoardTab.Overview.name,
        onSelect = {}
    )
}

@Composable
private fun TasksToolbar(
    onAddTask: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        ActionPill(
            text = stringResource(Res.string.tasks_action_add_target),
            onClick = onAddTask,
            icon = SnowIcons.Plus,
            emphasized = true
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean
) {
    val ext = MaterialTheme.extended
    val background = if (emphasized) ext.sidebarSelection else ext.tableRowHover
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(AppIconSize.xs),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BoardColumns(
    modifier: Modifier,
    state: TasksUiState,
    onReorderTask: (String, TaskColumn, Int) -> Unit,
    onMoveTask: (String, TaskColumn, Int) -> Unit,
    onDeleteTask: (String) -> Unit,
    onEditTask: (TaskItem) -> Unit
) {
    val grouped = state.groupedByColumn
    val tasksById = remember(state.tasks) { state.tasks.associateBy(TaskItem::id) }
    var boardOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var dragPreview by remember { mutableStateOf<TaskDragPreview?>(null) }
    var columnBounds by remember { mutableStateOf<Map<TaskColumn, ColumnBounds>>(emptyMap()) }
    var columnDropLayouts by remember { mutableStateOf<Map<TaskColumn, ColumnDropLayout>>(emptyMap()) }
    val activeDropColumn = dragPreview?.let { preview ->
        val centerX = preview.originInRoot.x + preview.offsetX + (preview.size.width / 2f)
        resolveDropColumnByBounds(
            centerX = centerX,
            source = preview.sourceColumn,
            bounds = columnBounds
        )
    }
    val activeDropIndex = dragPreview
        ?.takeIf { activeDropColumn != null }
        ?.let { preview ->
            resolveDropIndexForColumn(
                centerY = preview.originInRoot.y + preview.offsetY + (preview.size.height / 2f),
                draggedHeight = preview.size.height.toFloat(),
                layout = columnDropLayouts[activeDropColumn]
            )
        }

    BoxWithConstraints(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                boardOriginInRoot = coordinates.positionInRoot()
            }
    ) {
        if (maxWidth >= 900.dp) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)
            ) {
                TaskColumn.entries.forEach { column ->
                    TaskColumnBoard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        column = column,
                        tasks = grouped[column].orEmpty(),
                        onReorderTask = onReorderTask,
                        onMoveTask = onMoveTask,
                        onDeleteTask = onDeleteTask,
                        onEditTask = onEditTask,
                        onDragPreviewStart = { taskId, taskSnapshot, originInRoot, size, sourceColumn ->
                            dragPreview = TaskDragPreview(
                                taskId = taskId,
                                taskSnapshot = taskSnapshot,
                                originInRoot = originInRoot,
                                size = size,
                                sourceColumn = sourceColumn
                            )
                        },
                        onDragPreviewMove = { taskId, offsetX, offsetY ->
                            dragPreview = dragPreview
                                ?.takeIf { it.taskId == taskId }
                                ?.copy(offsetX = offsetX, offsetY = offsetY)
                        },
                        onDragPreviewEnd = { dragPreview = null },
                        onColumnBoundsChanged = { resolvedColumn, bounds ->
                            columnBounds = columnBounds.toMutableMap().apply { put(resolvedColumn, bounds) }
                        },
                        onDropLayoutChanged = { resolvedColumn, layout ->
                            columnDropLayouts = columnDropLayouts.toMutableMap().apply { put(resolvedColumn, layout) }
                        },
                        resolveDropColumnByCenterX = { centerX, source ->
                            resolveDropColumnByBounds(
                                centerX = centerX,
                                source = source,
                                bounds = columnBounds
                            )
                        },
                        resolveDropIndexForTarget = { targetColumn, centerY, draggedHeight ->
                            resolveDropIndexForColumn(
                                centerY = centerY,
                                draggedHeight = draggedHeight,
                                layout = columnDropLayouts[targetColumn]
                            )
                        },
                        isExternalDropTarget = dragPreview != null &&
                            activeDropColumn == column &&
                            dragPreview?.sourceColumn != column,
                        externalDropIndex = if (activeDropColumn == column) activeDropIndex else null
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)
            ) {
                items(TaskColumn.entries) { column ->
                    TaskColumnBoard(
                        modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).fillMaxHeight(),
                        column = column,
                        tasks = grouped[column].orEmpty(),
                        onReorderTask = onReorderTask,
                        onMoveTask = onMoveTask,
                        onDeleteTask = onDeleteTask,
                        onEditTask = onEditTask,
                        onDragPreviewStart = { taskId, taskSnapshot, originInRoot, size, sourceColumn ->
                            dragPreview = TaskDragPreview(
                                taskId = taskId,
                                taskSnapshot = taskSnapshot,
                                originInRoot = originInRoot,
                                size = size,
                                sourceColumn = sourceColumn
                            )
                        },
                        onDragPreviewMove = { taskId, offsetX, offsetY ->
                            dragPreview = dragPreview
                                ?.takeIf { it.taskId == taskId }
                                ?.copy(offsetX = offsetX, offsetY = offsetY)
                        },
                        onDragPreviewEnd = { dragPreview = null },
                        onColumnBoundsChanged = { resolvedColumn, bounds ->
                            columnBounds = columnBounds.toMutableMap().apply { put(resolvedColumn, bounds) }
                        },
                        onDropLayoutChanged = { resolvedColumn, layout ->
                            columnDropLayouts = columnDropLayouts.toMutableMap().apply { put(resolvedColumn, layout) }
                        },
                        resolveDropColumnByCenterX = { centerX, source ->
                            resolveDropColumnByBounds(
                                centerX = centerX,
                                source = source,
                                bounds = columnBounds
                            )
                        },
                        resolveDropIndexForTarget = { targetColumn, centerY, draggedHeight ->
                            resolveDropIndexForColumn(
                                centerY = centerY,
                                draggedHeight = draggedHeight,
                                layout = columnDropLayouts[targetColumn]
                            )
                        },
                        isExternalDropTarget = dragPreview != null &&
                            activeDropColumn == column &&
                            dragPreview?.sourceColumn != column,
                        externalDropIndex = if (activeDropColumn == column) activeDropIndex else null
                    )
                }
            }
        }

        val preview = dragPreview
        if (preview != null) {
            val previewTask = tasksById[preview.taskId] ?: preview.taskSnapshot
            val density = LocalDensity.current
            val widthDp = with(density) { preview.size.width.toDp() }
            val heightDp = with(density) { preview.size.height.toDp() }
            val x = preview.originInRoot.x - boardOriginInRoot.x + preview.offsetX
            val y = preview.originInRoot.y - boardOriginInRoot.y + preview.offsetY
            TaskBoardDragPreview(
                task = previewTask,
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .width(widthDp)
                    .height(heightDp)
                    .zIndex(200f)
            )
        }
    }
}

@Composable
private fun TaskColumnBoard(
    modifier: Modifier,
    column: TaskColumn,
    tasks: List<TaskItem>,
    onReorderTask: (String, TaskColumn, Int) -> Unit,
    onMoveTask: (String, TaskColumn, Int) -> Unit,
    onDeleteTask: (String) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDragPreviewStart: (
        taskId: String,
        taskSnapshot: TaskItem,
        originInRoot: Offset,
        size: IntSize,
        sourceColumn: TaskColumn
    ) -> Unit,
    onDragPreviewMove: (taskId: String, offsetX: Float, offsetY: Float) -> Unit,
    onDragPreviewEnd: () -> Unit,
    onColumnBoundsChanged: (TaskColumn, ColumnBounds) -> Unit,
    onDropLayoutChanged: (TaskColumn, ColumnDropLayout) -> Unit,
    resolveDropColumnByCenterX: (centerX: Float, source: TaskColumn) -> TaskColumn?,
    resolveDropIndexForTarget: (target: TaskColumn, centerY: Float, draggedHeight: Float) -> Int,
    isExternalDropTarget: Boolean,
    externalDropIndex: Int?
) {
    val sortedTasks = remember(tasks) { tasks.sortedBy { it.orderInColumn } }
    var orderedTasks by remember(column) { mutableStateOf(sortedTasks) }
    var draggingTaskId by remember(column) { mutableStateOf<String?>(null) }
    var draggedTaskOffsetX by remember(column) { mutableStateOf(0f) }
    var draggedTaskOffsetY by remember(column) { mutableStateOf(0f) }
    var itemHeightPx by remember(column) { mutableStateOf(88f) }
    var reorderedInColumn by remember(column) { mutableStateOf(false) }
    var dragStartIndex by remember(column) { mutableStateOf(-1) }
    var preferCanonicalOrder by remember(column) { mutableStateOf(false) }
    var taskBounds by remember(column) { mutableStateOf<Map<String, TaskItemBounds>>(emptyMap()) }
    val density = LocalDensity.current
    val itemSpacingPx = with(density) { AppSpacing.s.toPx() }
    val ext = MaterialTheme.extended
    val displayTasks = if (preferCanonicalOrder) sortedTasks else orderedTasks

    LaunchedEffect(sortedTasks) {
        if (draggingTaskId == null) {
            orderedTasks = sortedTasks
        } else {
            val latestById = sortedTasks.associateBy(TaskItem::id)
            val persistedOrder = orderedTasks.mapNotNull { latestById[it.id] }
            val persistedIds = persistedOrder.asSequence().map(TaskItem::id).toSet()
            val missing = sortedTasks.filterNot { it.id in persistedIds }
            orderedTasks = persistedOrder + missing
        }
    }

    LaunchedEffect(displayTasks) {
        val knownIds = displayTasks.asSequence().map(TaskItem::id).toSet()
        taskBounds = taskBounds.filterKeys { it in knownIds }
    }

    LaunchedEffect(taskBounds, displayTasks) {
        val slots = displayTasks.mapIndexedNotNull { index, task ->
            taskBounds[task.id]?.let { bounds ->
                DropSlot(index = index, top = bounds.top, bottom = bounds.bottom)
            }
        }
        onDropLayoutChanged(
            column,
            ColumnDropLayout(
                slots = slots,
                itemCount = displayTasks.size
            )
        )
    }

    Column(
        modifier = modifier
            .zIndex(if (draggingTaskId != null) 20f else 0f)
            .graphicsLayer { clip = false }
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                onColumnBoundsChanged(
                    column,
                    ColumnBounds(
                        left = position.x,
                        right = position.x + coordinates.size.width
                    )
                )
            },
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        Text(text = "${columnLabel(column)} ${displayTasks.size}", style = MaterialTheme.typography.titleSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(columnAccent(column))
        )
        if (displayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(ext.toolbarSurface)
                    .padding(AppSpacing.m)
            ) {
                Text(
                    text = stringResource(Res.string.tasks_no_tasks),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.sidebarTextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = false },
                verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
            ) {
                val targetDropIndex = externalDropIndex?.coerceIn(0, displayTasks.size)
                if (isExternalDropTarget && targetDropIndex == 0) {
                    item(key = "drop-placeholder-${column.name}") {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with(density) { itemHeightPx.toDp() })
                        )
                    }
                }
                itemsIndexed(displayTasks, key = { _, task -> task.id }) { index, task ->
                    val showInlineDropPlaceholder = isExternalDropTarget && targetDropIndex == index && targetDropIndex != 0
                    val isDragging = draggingTaskId == task.id
                    var taskOriginInRoot by remember(task.id) { mutableStateOf(Offset.Zero) }
                    var taskSize by remember(task.id) { mutableStateOf(IntSize.Zero) }
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        if (showInlineDropPlaceholder) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(with(density) { itemHeightPx.toDp() })
                            )
                        }
                        TaskBoardCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    alpha = if (isDragging) 0f else 1f
                                    clip = false
                                }
                                .onGloballyPositioned { coordinates ->
                                    taskOriginInRoot = coordinates.positionInRoot()
                                    taskSize = coordinates.size
                                    if (coordinates.size.height > 0) itemHeightPx = coordinates.size.height.toFloat()
                                    val top = coordinates.positionInRoot().y
                                    taskBounds = taskBounds.toMutableMap().apply {
                                        put(task.id, TaskItemBounds(top = top, bottom = top + coordinates.size.height))
                                    }
                                }
                                .pointerInput(task.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            preferCanonicalOrder = false
                                            draggingTaskId = task.id
                                            draggedTaskOffsetX = 0f
                                            draggedTaskOffsetY = 0f
                                            reorderedInColumn = false
                                            dragStartIndex = orderedTasks.indexOfFirst { it.id == task.id }
                                            if (taskSize.width > 0 && taskSize.height > 0) {
                                                onDragPreviewStart(task.id, task, taskOriginInRoot, taskSize, column)
                                            }
                                        },
                                        onDragCancel = {
                                            draggingTaskId = null
                                            draggedTaskOffsetX = 0f
                                            draggedTaskOffsetY = 0f
                                            reorderedInColumn = false
                                            dragStartIndex = -1
                                            preferCanonicalOrder = false
                                            onDragPreviewEnd()
                                        },
                                        onDragEnd = {
                                            val finishedTaskId = draggingTaskId
                                            val finalOffsetX = draggedTaskOffsetX
                                            val finalOffsetY = draggedTaskOffsetY
                                            val didReorderInColumn = reorderedInColumn
                                            val initialIndex = dragStartIndex
                                            draggingTaskId = null
                                            draggedTaskOffsetX = 0f
                                            draggedTaskOffsetY = 0f
                                            reorderedInColumn = false
                                            dragStartIndex = -1
                                            preferCanonicalOrder = false
                                            onDragPreviewEnd()
                                            if (finishedTaskId != null) {
                                                val droppedCenterX = taskOriginInRoot.x + finalOffsetX + (taskSize.width / 2f)
                                                val droppedCenterY = taskOriginInRoot.y + finalOffsetY + (taskSize.height / 2f)
                                                val resolvedTarget = resolveDropColumnByCenterX(droppedCenterX, column)
                                                if (resolvedTarget != null && resolvedTarget != column) {
                                                    val targetIndex = resolveDropIndexForTarget(
                                                        resolvedTarget,
                                                        droppedCenterY,
                                                        taskSize.height.toFloat()
                                                    )
                                                    orderedTasks = sortedTasks.filterNot { it.id == finishedTaskId }
                                                    onMoveTask(finishedTaskId, resolvedTarget, targetIndex)
                                                    return@detectDragGestures
                                                }
                                            }
                                            if (finishedTaskId != null) {
                                                val finalIndex = orderedTasks.indexOfFirst { it.id == finishedTaskId }
                                                val changedPosition = finalIndex >= 0 && finalIndex != initialIndex
                                                if (didReorderInColumn || changedPosition) {
                                                    onReorderTask(finishedTaskId, column, finalIndex.coerceAtLeast(0))
                                                }
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val id = draggingTaskId ?: return@detectDragGestures
                                            draggedTaskOffsetX += dragAmount.x
                                            draggedTaskOffsetY += dragAmount.y
                                            onDragPreviewMove(id, draggedTaskOffsetX, draggedTaskOffsetY)

                                            val draggedCenterX = taskOriginInRoot.x +
                                                draggedTaskOffsetX +
                                                (taskSize.width / 2f)
                                            val externalHoverColumn = resolveDropColumnByCenterX(draggedCenterX, column)
                                            if (externalHoverColumn != null && externalHoverColumn != column) {
                                                preferCanonicalOrder = true
                                                if (orderedTasks != sortedTasks) {
                                                    orderedTasks = sortedTasks
                                                }
                                                reorderedInColumn = false
                                                return@detectDragGestures
                                            }
                                            preferCanonicalOrder = false

                                            val currentIndex = orderedTasks.indexOfFirst { it.id == id }
                                            if (currentIndex == -1) return@detectDragGestures

                                            val baseIndex = dragStartIndex.takeIf { it >= 0 } ?: currentIndex
                                            val stepPx = (itemHeightPx + itemSpacingPx).coerceAtLeast(1f)
                                            val shiftCount = (draggedTaskOffsetY / stepPx).roundToInt()
                                            val targetIndex = (baseIndex + shiftCount).coerceIn(0, orderedTasks.lastIndex)

                                            if (targetIndex != currentIndex) {
                                                orderedTasks = orderedTasks.toMutableList().apply {
                                                    add(targetIndex, removeAt(currentIndex))
                                                }
                                                reorderedInColumn = true
                                            }
                                        }
                                    )
                                }
                                .clip(MaterialTheme.shapes.medium),
                            task = task,
                            onEdit = { onEditTask(task) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
                if (isExternalDropTarget && targetDropIndex == displayTasks.size) {
                    item(key = "drop-placeholder-end-${column.name}") {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with(density) { itemHeightPx.toDp() })
                        )
                    }
                }
            }
        }
    }
}

private data class ColumnBounds(
    val left: Float,
    val right: Float
)

private data class TaskItemBounds(
    val top: Float,
    val bottom: Float
)

private data class DropSlot(
    val index: Int,
    val top: Float,
    val bottom: Float
)

private data class ColumnDropLayout(
    val slots: List<DropSlot>,
    val itemCount: Int
)

private fun resolveDropColumnByBounds(
    centerX: Float,
    source: TaskColumn,
    bounds: Map<TaskColumn, ColumnBounds>
): TaskColumn? {
    if (bounds.isEmpty()) return null
    return bounds.entries.firstOrNull { (_, area) ->
        centerX >= area.left && centerX <= area.right
    }?.key?.takeIf { it != source }
}

private fun resolveDropIndexForColumn(
    centerY: Float,
    draggedHeight: Float,
    layout: ColumnDropLayout?
): Int {
    if (layout == null || layout.itemCount == 0) return 0
    if (layout.slots.isEmpty()) return 0

    val dragTop = centerY - (draggedHeight / 2f)
    val dragBottom = centerY + (draggedHeight / 2f)

    val bestOverlap = layout.slots
        .map { slot ->
            val overlap = (minOf(dragBottom, slot.bottom) - maxOf(dragTop, slot.top)).coerceAtLeast(0f)
            slot to overlap
        }
        .maxByOrNull { it.second }

    if (bestOverlap != null && bestOverlap.second > 0f) {
        val slot = bestOverlap.first
        val slotCenter = (slot.top + slot.bottom) / 2f
        val insertIndex = if (centerY < slotCenter) slot.index else slot.index + 1
        return insertIndex.coerceIn(0, layout.itemCount)
    }

    val sorted = layout.slots.sortedBy { (it.top + it.bottom) / 2f }
    sorted.firstOrNull { centerY < ((it.top + it.bottom) / 2f) }?.let { slot ->
        return slot.index.coerceIn(0, layout.itemCount)
    }
    return layout.itemCount
}

private data class TaskDragPreview(
    val taskId: String,
    val taskSnapshot: TaskItem,
    val originInRoot: Offset,
    val size: IntSize,
    val sourceColumn: TaskColumn,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

@Composable
private fun TaskBoardDragPreview(
    task: TaskItem,
    modifier: Modifier
) {
    val ext = MaterialTheme.extended
    val stripeColor = priorityColor(task.priority)
    val dueText = formatDueDate(task.dueDate)
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .graphicsLayer {
                alpha = 0.98f
                shadowElevation = 18f
            }
            .background(ext.toolbarSurface)
            .drawBehind {
                val stripeWidth = 3.dp.toPx()
                drawRect(
                    color = stripeColor,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(stripeWidth, size.height)
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(AppSpacing.s),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    task.label?.takeIf { it.isNotBlank() }?.let {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(ext.tableRowHover)
                                .padding(horizontal = AppSpacing.xs, vertical = 3.dp)
                        ) {
                            Text(text = it, style = MaterialTheme.typography.labelSmall, color = ext.sidebarTextMuted)
                        }
                    }
                }
                Text(text = task.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = task.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (dueText != null) {
                    Text(text = dueText, style = MaterialTheme.typography.labelSmall, color = ext.sidebarTextMuted)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AssigneeBadge(name = task.assignee)
                    Spacer(Modifier.weight(1f))
                    MetaCount(icon = SnowIcons.Paperclip, value = task.attachmentsCount)
                }
            }
        }
    }
}

@Composable
private fun TaskBoardCard(
    modifier: Modifier,
    task: TaskItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = MaterialTheme.extended
    val stripeColor = priorityColor(task.priority)
    val dueText = formatDueDate(task.dueDate)
    Box(
        modifier = modifier
            .background(ext.toolbarSurface)
            .drawBehind {
                val stripeWidth = 3.dp.toPx()
                drawRect(
                    color = stripeColor,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(stripeWidth, size.height)
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(AppSpacing.s),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    task.label?.takeIf { it.isNotBlank() }?.let {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(ext.tableRowHover)
                                .padding(horizontal = AppSpacing.xs, vertical = 3.dp)
                        ) {
                            Text(text = it, style = MaterialTheme.typography.labelSmall, color = ext.sidebarTextMuted)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = SnowIcons.Edit,
                            contentDescription = stringResource(Res.string.tasks_editor_edit),
                            modifier = Modifier.size(AppIconSize.xs),
                            tint = ext.sidebarTextMuted
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = SnowIcons.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(AppIconSize.xs),
                            tint = ext.sidebarTextMuted
                        )
                    }
                }
                Text(text = task.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = task.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (dueText != null) {
                    Text(text = dueText, style = MaterialTheme.typography.labelSmall, color = ext.sidebarTextMuted)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AssigneeBadge(name = task.assignee)
                    Spacer(Modifier.weight(1f))
                    MetaCount(icon = SnowIcons.Paperclip, value = task.attachmentsCount)
                }
            }
        }
    }
}

@Composable
private fun AssigneeBadge(name: String?) {
    val text = name?.trim().orEmpty()
    val initials = text
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "?" }
    val palette = MaterialTheme.extended.portfolioPalette
    val index = (text.hashCode().toUInt().toInt() and Int.MAX_VALUE) % palette.size
    val color = palette[index]
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun MetaCount(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int
) {
    val ext = MaterialTheme.extended
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(AppIconSize.xs),
            tint = ext.sidebarTextMuted
        )
        Text(text = value.toString(), style = MaterialTheme.typography.labelSmall, color = ext.sidebarTextMuted)
    }
}

@Composable
private fun columnAccent(column: TaskColumn): Color = when (column) {
    TaskColumn.Backlog -> MaterialTheme.extended.regionUnitedStates
    TaskColumn.InProgress -> MaterialTheme.extended.regionEuroArea
    TaskColumn.Completed -> MaterialTheme.extended.positive
}

@Composable
private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.Low -> MaterialTheme.extended.impactLow
    TaskPriority.Medium -> MaterialTheme.extended.impactMedium
    TaskPriority.High -> MaterialTheme.extended.impactHigh
}

private data class TaskEditorPayload(
    val title: String,
    val summary: String,
    val label: String?,
    val priority: TaskPriority,
    val column: TaskColumn,
    val dueDateIso: String?,
    val assignee: String?
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    initialTask: TaskItem?,
    onDismiss: () -> Unit,
    onSave: (TaskEditorPayload) -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    var title by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.title.orEmpty()) }
    var summary by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.summary.orEmpty()) }
    var label by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.label.orEmpty()) }
    var assignee by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.assignee.orEmpty()) }
    var dueDateIso by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.dueDate.orEmpty()) }
    var priority by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.priority ?: TaskPriority.Medium) }
    var column by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.column ?: TaskColumn.Backlog) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialTask == null) {
                    stringResource(Res.string.tasks_editor_new)
                } else {
                    stringResource(Res.string.tasks_editor_edit)
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.tasks_editor_title)) }
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.tasks_editor_summary)) }
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.tasks_editor_label)) }
                )
                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.tasks_editor_assignee)) }
                )
                OutlinedTextField(
                    value = formatDueDate(dueDateIso).orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    label = { Text(stringResource(Res.string.tasks_editor_due_date)) },
                    leadingIcon = { Icon(SnowIcons.Calendar, contentDescription = null) },
                    trailingIcon = {
                        Row {
                            if (dueDateIso.isNotBlank()) {
                                IconButton(onClick = { dueDateIso = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(SnowIcons.Close, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                                }
                            }
                            IconButton(onClick = { showDatePicker = true }, modifier = Modifier.size(24.dp)) {
                                Icon(SnowIcons.Calendar, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                            }
                        }
                    }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    items(TaskPriority.entries) { candidate ->
                        AssistChip(
                            onClick = { priority = candidate },
                            label = { Text(priorityLabel(candidate)) },
                            leadingIcon = if (priority == candidate) {
                                { Icon(SnowIcons.Check, contentDescription = null, modifier = Modifier.size(AppIconSize.xs)) }
                            } else null
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    items(TaskColumn.entries) { candidate ->
                        AssistChip(
                            onClick = { column = candidate },
                            label = { Text(columnLabel(candidate)) },
                            leadingIcon = if (column == candidate) {
                                { Icon(SnowIcons.Check, contentDescription = null, modifier = Modifier.size(AppIconSize.xs)) }
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && summary.isNotBlank(),
                onClick = {
                    onSave(
                        TaskEditorPayload(
                            title = title.trim(),
                            summary = summary.trim(),
                            label = label.trim().takeUnless { it.isBlank() },
                            priority = priority,
                            column = column,
                            dueDateIso = dueDateIso.trim().takeUnless { it.isBlank() },
                            assignee = assignee.trim().takeUnless { it.isBlank() }
                        )
                    )
                }
            ) {
                Text(
                    if (initialTask == null) {
                        stringResource(Res.string.tasks_create)
                    } else {
                        stringResource(Res.string.tasks_save)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.datepicker_cancel))
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = null)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        dueDateIso = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.datepicker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.datepicker_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun columnLabel(column: TaskColumn): String = when (column) {
    TaskColumn.Backlog -> stringResource(Res.string.tasks_column_backlog)
    TaskColumn.InProgress -> stringResource(Res.string.tasks_column_in_progress)
    TaskColumn.Completed -> stringResource(Res.string.tasks_column_completed)
}

@Composable
private fun priorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.Low -> stringResource(Res.string.tasks_editor_priority_low)
    TaskPriority.Medium -> stringResource(Res.string.tasks_editor_priority_medium)
    TaskPriority.High -> stringResource(Res.string.tasks_editor_priority_high)
}

private fun formatDueDate(value: String?): String? {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return null
    val match = ISO_DATE_REGEX.matchEntire(raw) ?: return raw
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    return "$day.$month.$year"
}

private val ISO_DATE_REGEX = Regex("""(\d{4})-(\d{2})-(\d{2})""")
