package de.tradebuddy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
actual fun appLogoPainter(): Painter =
    run {
        val context = LocalContext.current
        val resId = remember(context) {
            context.resources.getIdentifier("logo_tradebuddy", "drawable", context.packageName)
        }
        if (resId != 0) {
            painterResource(resId)
        } else {
            ColorPainter(Color.Transparent)
        }
    }
