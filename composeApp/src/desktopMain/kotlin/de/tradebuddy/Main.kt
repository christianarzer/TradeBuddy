package de.tradebuddy

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.tradebuddy.ui.AppRoot
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Taskbar
import java.awt.image.BufferedImage
import java.util.prefs.Preferences

private data class PersistedWindowState(
    val widthDp: Float,
    val heightDp: Float,
    val xDp: Float?,
    val yDp: Float?,
    val maximized: Boolean
)

private object DesktopWindowStateStore {
    private val prefs = Preferences.userRoot().node("de.tradebuddy.window")

    private const val KeyWidthDp = "widthDp"
    private const val KeyHeightDp = "heightDp"
    private const val KeyXdp = "xDp"
    private const val KeyYdp = "yDp"
    private const val KeyMaximized = "maximized"

    private const val DefaultWidthDp = 1280f
    private const val DefaultHeightDp = 820f

    fun load(): PersistedWindowState {
        val width = prefs.getFloat(KeyWidthDp, DefaultWidthDp).coerceAtLeast(960f)
        val height = prefs.getFloat(KeyHeightDp, DefaultHeightDp).coerceAtLeast(640f)
        val x = prefs.get(KeyXdp, null)?.toFloatOrNull()
        val y = prefs.get(KeyYdp, null)?.toFloatOrNull()
        val maximized = prefs.getBoolean(KeyMaximized, false)
        return PersistedWindowState(
            widthDp = width,
            heightDp = height,
            xDp = x,
            yDp = y,
            maximized = maximized
        )
    }

    fun save(
        widthDp: Float,
        heightDp: Float,
        xDp: Float?,
        yDp: Float?,
        maximized: Boolean
    ) {
        prefs.putFloat(KeyWidthDp, widthDp)
        prefs.putFloat(KeyHeightDp, heightDp)
        if (xDp != null && yDp != null) {
            prefs.put(KeyXdp, xDp.toString())
            prefs.put(KeyYdp, yDp.toString())
        } else {
            prefs.remove(KeyXdp)
            prefs.remove(KeyYdp)
        }
        prefs.putBoolean(KeyMaximized, maximized)
    }
}

private const val MinWindowWidthPx = 1100
private const val MinWindowHeightPx = 700

private val TradeBuddyWindowIcons: List<BufferedImage> = listOf(
    createTradeBuddyWindowIcon(16),
    createTradeBuddyWindowIcon(24),
    createTradeBuddyWindowIcon(32),
    createTradeBuddyWindowIcon(48),
    createTradeBuddyWindowIcon(64)
)

private fun createTradeBuddyWindowIcon(size: Int): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    val circleInset = (size * 0.08f).toInt()
    graphics.color = Color(0x26, 0x7A, 0xFF)
    graphics.fillOval(circleInset, circleInset, size - (circleInset * 2), size - (circleInset * 2))

    graphics.color = Color(0xFF, 0xFF, 0xFF)
    graphics.font = Font("SansSerif", Font.BOLD, (size * 0.43f).toInt().coerceAtLeast(10))
    val text = "TB"
    val metrics = graphics.fontMetrics
    val x = (size - metrics.stringWidth(text)) / 2
    val y = ((size - metrics.height) / 2) + metrics.ascent
    graphics.drawString(text, x, y)
    graphics.dispose()
    return image
}

fun main() = application {
    val persisted = remember { DesktopWindowStateStore.load() }
    val initialPosition = remember(persisted) {
        if (persisted.xDp != null && persisted.yDp != null) {
            WindowPosition(persisted.xDp.dp, persisted.yDp.dp)
        } else {
            WindowPosition.PlatformDefault
        }
    }
    val windowState = rememberWindowState(
        width = persisted.widthDp.dp,
        height = persisted.heightDp.dp,
        position = initialPosition,
        placement = if (persisted.maximized) WindowPlacement.Maximized else WindowPlacement.Floating
    )

    Window(
        onCloseRequest = {
            val absolutePosition = windowState.position as? WindowPosition.Absolute
            val isFloating = windowState.placement == WindowPlacement.Floating
            DesktopWindowStateStore.save(
                widthDp = windowState.size.width.value,
                heightDp = windowState.size.height.value,
                xDp = absolutePosition?.x?.value,
                yDp = absolutePosition?.y?.value,
                maximized = !isFloating
            )
            exitApplication()
        },
        title = "",
        state = windowState
    ) {
        DisposableEffect(window) {
            window.minimumSize = Dimension(MinWindowWidthPx, MinWindowHeightPx)
            window.iconImages = TradeBuddyWindowIcons
            runCatching {
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar.getTaskbar().iconImage = TradeBuddyWindowIcons.last()
                }
            }
            onDispose {}
        }
        AppRoot()
    }
}
