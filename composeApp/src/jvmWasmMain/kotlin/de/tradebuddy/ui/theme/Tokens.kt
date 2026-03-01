package de.tradebuddy.ui.theme

import androidx.compose.ui.unit.dp

object AppSpacing {
    // Core scale from the Figma source.
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 28.dp
    val xxxl = 40.dp
    val xxxxl = 48.dp
    val mega = 80.dp

    // Semantic aliases.
    val cardPadding = xl
    val screenHPadding = m
    val screenHPaddingWide = xl
    val screenVPadding = m
    val sectionGap = xl
    val itemGap = s
    val chipGap = xs
    val inlineGap = xxs
}

object AppRadius {
    val xs = 6.dp
    val s = 10.dp
    val m = 14.dp
    val l = 16.dp
    val xl = 24.dp
}

object AppIconSize {
    val xs = 16.dp
    val s = 20.dp
    val m = 24.dp
    val l = 28.dp
    val xl = 32.dp
    val xxl = 40.dp
    val xxxl = 48.dp
    val jumbo = 80.dp
}

object AppStroke {
    val thin = 1.dp
    val normal = 1.5.dp
    val thick = 2.dp
}

object AppDurations {
    const val quick = 150
    const val standard = 300
    const val emphasis = 500
}
