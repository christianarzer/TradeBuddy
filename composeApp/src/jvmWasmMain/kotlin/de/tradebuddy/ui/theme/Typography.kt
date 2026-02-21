package de.tradebuddy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.sora_bold
import trade_buddy.composeapp.generated.resources.sora_medium
import trade_buddy.composeapp.generated.resources.sora_regular

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun soraFontFamily(): FontFamily = FontFamily(
    Font(Res.font.sora_regular, weight = FontWeight.Normal),
    Font(Res.font.sora_medium, weight = FontWeight.Medium),
    Font(Res.font.sora_bold, weight = FontWeight.Bold)
)

@Composable
fun appTypography(): Typography {
    val sora = soraFontFamily()
    return remember(sora) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 52.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp
            ),
            titleLarge = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 26.sp
            ),
            titleMedium = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            bodySmall = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            labelLarge = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            labelMedium = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp
            ),
            labelSmall = TextStyle(
                fontFamily = sora,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        )
    }
}

