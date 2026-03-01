package de.tradebuddy.ui.components.shared

import de.tradebuddy.ui.theme.AppIconSize
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.LocalExtendedColors
import de.tradebuddy.ui.icons.SnowIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SnowToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val ext = LocalExtendedColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ext.toolbarSurface, MaterialTheme.shapes.small)
            .heightIn(min = 36.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun SnowToolbarIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(AppIconSize.s)
        )
    }
}

@Composable
fun SnowToolbarPopupPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        content = content
    )
}

@Composable
fun SnowMonthSwitchToolbar(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousContentDescription: String,
    nextContentDescription: String,
    modifier: Modifier = Modifier
) {
    SnowToolbar(modifier = modifier) {
        SnowToolbarIconButton(
            icon = SnowIcons.ArrowLeft,
            onClick = onPrevious,
            contentDescription = previousContentDescription
        )
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        SnowToolbarIconButton(
            icon = SnowIcons.ArrowRight,
            onClick = onNext,
            contentDescription = nextContentDescription
        )
        Spacer(Modifier.weight(1f))
    }
}
