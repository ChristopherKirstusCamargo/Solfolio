package dev.zhar.larpwallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppBackground = Color(0xFF0B0A0D)
val ElevatedSurface = Color(0xFF151319)
val CardSurface = Color(0xFF1B1820)
val Hairline = Color(0xFF2A2630)
val Purple = Color(0xFFA98CFF)
val PurpleBright = Color(0xFFC1AEFF)
val Aqua = Color(0xFF65E6CF)
val SoftWhite = Color(0xFFF7F4FA)
val MutedText = Color(0xFFA9A3B0)
val Positive = Color(0xFF6EE7B7)
val Negative = Color(0xFFFF7B8B)

private val LarpColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color(0xFF17101F),
    secondary = Aqua,
    onSecondary = Color(0xFF06251F),
    background = AppBackground,
    onBackground = SoftWhite,
    surface = ElevatedSurface,
    onSurface = SoftWhite,
    surfaceVariant = CardSurface,
    onSurfaceVariant = MutedText,
    outline = Hairline,
    error = Negative,
)

@Composable
fun LarpWalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LarpColors,
        typography = MaterialTheme.typography.copy(
            displaySmall = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.7).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.3).sp,
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 23.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
        ),
        content = content,
    )
}

