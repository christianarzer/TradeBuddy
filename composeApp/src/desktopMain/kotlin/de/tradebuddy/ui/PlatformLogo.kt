package de.tradebuddy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.logo_tradebuddy

@Composable
actual fun appLogoPainter(): Painter =
    painterResource(Res.drawable.logo_tradebuddy)
