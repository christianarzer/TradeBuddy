package de.tradebuddy.ui.theme

import androidx.compose.ui.unit.dp

object AppSpacing {
    val xxs  = 2.dp
    val xs   = 4.dp
    val s    = 8.dp
    val m    = 12.dp
    val l    = 16.dp
    val xl   = 24.dp
    val xxl  = 32.dp
    val xxxl = 48.dp

    // Semantic aliases
    val cardPadding        = l   // 16.dp – standard for all cards
    val screenHPadding     = m   // 12.dp – mobile/normal
    val screenHPaddingWide = l   // 16.dp – wide layout
    val screenVPadding     = m   // 12.dp
    val sectionGap         = l   // 16.dp
    val itemGap            = m   // 12.dp
    val chipGap            = s   // 8.dp
    val inlineGap          = xs  // 4.dp
}

object AppIconSize {
    val xs  = 14.dp
    val s   = 16.dp
    val m   = 20.dp
    val l   = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
}

object AppStroke {
    val thin   = 1.dp
    val normal = 1.5.dp
    val thick  = 2.dp
}

object AppDurations {
    const val quick    = 150
    const val standard = 300
    const val emphasis = 500
}
