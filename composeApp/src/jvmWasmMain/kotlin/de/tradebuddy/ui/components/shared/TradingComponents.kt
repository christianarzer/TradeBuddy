package de.tradebuddy.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import de.tradebuddy.domain.model.MoveDirection
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.LocalExtendedColors

/**
 * Displays a price value colored by its trade direction.
 * If [direction] is null the default text color is used.
 */
@Composable
fun PriceText(
    value: String,
    direction: MoveDirection?,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val color: Color = when (direction) {
        MoveDirection.Up   -> ext.positive
        MoveDirection.Down -> ext.negative
        null               -> Color.Unspecified
    }
    Text(text = value, style = style, color = color, modifier = modifier)
}

/**
 * Pill badge showing a percentage change with Up/Down coloring.
 * Positive values are shown with a leading '+'.
 */
@Composable
fun ChangeBadge(
    changePercent: Double,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val isPositive = changePercent >= 0.0
    val color = if (isPositive) ext.positive else ext.negative
    val abs = kotlin.math.abs(changePercent)
    val hundredths = kotlin.math.round(abs * 100).toLong()
    val intPart = hundredths / 100
    val fracPart = (hundredths % 100).toString().padStart(2, '0')
    val label = "${if (isPositive) "+" else "-"}$intPart.$fracPart%"

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/**
 * A single label-value row with space-between alignment.
 */
@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
