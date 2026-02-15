package de.tradebuddy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.tradebuddy.di.AppContainer
import de.tradebuddy.domain.model.AppThemeStyle
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.ui.AppRootContent
import de.tradebuddy.ui.theme.colorSchemeFor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.logo_tradebuddy
import trade_buddy.composeapp.generated.resources.window_title

fun main() = application {
    val container = remember { AppContainer() }
    val viewModel = remember(container) { container.createSunMoonViewModel() }
    DisposableEffect(viewModel) {
        onDispose { viewModel.close() }
    }
    val state by viewModel.state.collectAsState()
    val icon = themedWindowIcon(state.themeStyle, state.themeMode)

    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.window_title),
        icon = icon
    ) {
        AppRootContent(state = state, viewModel = viewModel)
    }
}

@Composable
private fun themedWindowIcon(
    themeStyle: AppThemeStyle,
    themeMode: AppThemeMode
): Painter {
    val base = painterResource(Res.drawable.logo_tradebuddy)
    val tint = colorSchemeFor(themeStyle, themeMode).primary
    return remember(base, tint) {
        object : Painter() {
            override val intrinsicSize: Size = base.intrinsicSize

            override fun DrawScope.onDraw() {
                with(base) {
                    draw(size, colorFilter = ColorFilter.tint(tint))
                }
            }
        }
    }
}
