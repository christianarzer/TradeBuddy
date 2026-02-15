package de.tradebuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle

@Composable
fun AppTheme(
    themeStyle: AppThemeStyle,
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(themeStyle, themeMode),
        typography = appTypography(),
        shapes = AppShapes,
        content = content
    )
}

