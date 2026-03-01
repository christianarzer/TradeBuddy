package de.tradebuddy.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun appElevatedCardColors(): CardColors {
    val ext = MaterialTheme.extended
    return CardDefaults.elevatedCardColors(
        containerColor = ext.toolbarSurface
    )
}

@Composable
fun appBlockCardColors(): CardColors {
    val ext = MaterialTheme.extended
    return CardDefaults.elevatedCardColors(
        containerColor = ext.toolbarSurface
    )
}

@Composable
fun appFlatCardElevation(): CardElevation = CardDefaults.elevatedCardElevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp,
    disabledElevation = 0.dp
)

@Composable
fun appCardBorder(): BorderStroke {
    val ext = MaterialTheme.extended
    return BorderStroke(1.dp, ext.shellDivider.copy(alpha = 0.85f))
}

@Composable
fun appOutlinedCardColors(): CardColors {
    val ext = MaterialTheme.extended
    return CardDefaults.outlinedCardColors(
        containerColor = ext.toolbarSurface
    )
}
