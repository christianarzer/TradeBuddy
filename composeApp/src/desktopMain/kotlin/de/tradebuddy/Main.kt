package de.tradebuddy

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.tradebuddy.ui.AppRoot
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Taskbar
import java.awt.image.BufferedImage

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
    Window(
        onCloseRequest = ::exitApplication,
        title = ""
    ) {
        DisposableEffect(window) {
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
