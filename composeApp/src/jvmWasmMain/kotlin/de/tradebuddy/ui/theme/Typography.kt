package de.tradebuddy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.inter_italic
import trade_buddy.composeapp.generated.resources.inter_regular
import trade_buddy.composeapp.generated.resources.inter_semibold
import trade_buddy.composeapp.generated.resources.inter_semibold_italic

private const val InterFontFeatureSettings = "'ss01' 1, 'cv01' 1, 'cv11' 1"

private fun tradeBuddyStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: Int,
    lineHeight: Int
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = InterFontFeatureSettings
)

@Composable
fun appTypography(): Typography {
    val interFontFamily = FontFamily(
        Font(Res.font.inter_regular, weight = FontWeight.Normal),
        Font(Res.font.inter_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
        Font(Res.font.inter_semibold, weight = FontWeight.Medium),
        Font(Res.font.inter_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.inter_semibold, weight = FontWeight.Bold),
        Font(Res.font.inter_semibold_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
        Font(Res.font.inter_semibold_italic, weight = FontWeight.Bold, style = FontStyle.Italic)
    )
    return remember {
        Typography(
            displayLarge = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32, lineHeight = 40),
            displayMedium = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28, lineHeight = 36),
            headlineLarge = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24, lineHeight = 32),
            headlineMedium = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20, lineHeight = 28),
            titleLarge = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20, lineHeight = 28),
            titleMedium = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14, lineHeight = 20),
            titleSmall = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14, lineHeight = 20),
            bodyLarge = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.Normal, fontSize = 14, lineHeight = 20),
            bodyMedium = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.Normal, fontSize = 14, lineHeight = 20),
            bodySmall = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.Normal, fontSize = 12, lineHeight = 18),
            labelLarge = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14, lineHeight = 20),
            labelMedium = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.Normal, fontSize = 12, lineHeight = 18),
            labelSmall = tradeBuddyStyle(fontFamily = interFontFamily, fontWeight = FontWeight.Normal, fontSize = 12, lineHeight = 18)
        )
    }
}
