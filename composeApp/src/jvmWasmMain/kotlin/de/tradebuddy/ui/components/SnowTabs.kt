package de.tradebuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.extended

data class SnowTabItem(
    val id: String,
    val label: String
)

@Composable
fun SnowTabs(
    items: List<SnowTabItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        items(items, key = { it.id }) { item ->
            val selected = item.id == selectedId
            val interactionSource = remember { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()
            val containerColor = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                hovered -> ext.toolbarSurface
                else -> Color.Transparent
            }
            val textColor = when {
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                hovered -> MaterialTheme.colorScheme.onSurface
                else -> ext.sidebarTextMuted
            }
            Box(
                modifier = Modifier
                    .widthIn(min = 84.dp)
                    .heightIn(min = 34.dp)
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f) else ext.shellDivider,
                        shape = MaterialTheme.shapes.medium
                    )
                    .background(containerColor, MaterialTheme.shapes.medium)
                    .hoverable(interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(item.id) }
                    .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xxs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
