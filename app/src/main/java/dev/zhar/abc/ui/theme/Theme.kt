package dev.zhar.abc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.ColorPalette
import dev.zhar.abc.domain.ThemePreference

private data class Palette(val primary: Color, val secondary: Color, val tertiary: Color, val deep: Color)

private fun palette(value: ColorPalette): Palette = when (value) {
    ColorPalette.SOLANA -> Palette(AbcPurple, AbcMint, SolanaBlue, Color(0xFF6C42D8))
    ColorPalette.VIOLET -> Palette(Color(0xFFD0A8FF), Color(0xFFEF9DE9), Color(0xFF9FA8FF), Color(0xFF7543C8))
    ColorPalette.OCEAN -> Palette(OceanBlue, Color(0xFF67E4E0), Color(0xFF9AA8FF), Color(0xFF2767B5))
    ColorPalette.FOREST -> Palette(ForestGreen, Color(0xFFC2D985), Color(0xFF69D7CF), Color(0xFF277653))
    ColorPalette.SUNSET -> Palette(SunsetOrange, Color(0xFFFF91A8), Color(0xFFFFCE78), Color(0xFFC45E43))
}

private fun darkScheme(c: Palette, amoled: Boolean): ColorScheme = darkColorScheme(
    primary = c.primary, onPrimary = Color(0xFF180B24), primaryContainer = c.deep.copy(alpha = 0.42f),
    onPrimaryContainer = Color(0xFFF2E7FF), secondary = c.secondary, onSecondary = Color(0xFF06251D),
    secondaryContainer = c.secondary.copy(alpha = 0.18f), onSecondaryContainer = Color(0xFFD5FFEF),
    tertiary = c.tertiary, background = if (amoled) AmoledBackground else DarkBackground,
    onBackground = Color(0xFFF2EDF4), surface = if (amoled) AmoledSurface else DarkSurface,
    onSurface = Color(0xFFF2EDF4), surfaceVariant = if (amoled) AmoledSurfaceHigh else Color(0xFF211D25),
    onSurfaceVariant = Color(0xFFCBC3CF), outline = Color(0xFF625A69), outlineVariant = Color(0xFF39323F), error = AbcRed,
)

private fun lightScheme(c: Palette): ColorScheme = lightColorScheme(
    primary = c.deep, onPrimary = Color.White, primaryContainer = c.primary.copy(alpha = 0.24f),
    onPrimaryContainer = Color(0xFF24142F), secondary = c.secondary.copy(alpha = 0.86f),
    onSecondary = Color(0xFF08251E), secondaryContainer = c.secondary.copy(alpha = 0.28f),
    onSecondaryContainer = Color(0xFF09251E), tertiary = c.tertiary, background = LightBackground,
    onBackground = Color(0xFF1E1A20), surface = LightSurface, onSurface = Color(0xFF1E1A20),
    surfaceVariant = Color(0xFFEAE3ED), onSurfaceVariant = Color(0xFF4B454F), outline = Color(0xFF7D747F),
    outlineVariant = Color(0xFFCEC5D0), error = Color(0xFFBA1A1A),
)

@Suppress("DEPRECATION")
@Composable
fun AbcTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.theme) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK, ThemePreference.AMOLED -> true
        ThemePreference.SYSTEM -> systemDark
    }
    val selected = palette(settings.colorPalette)
    val colors = if (isDark) darkScheme(selected, settings.theme == ThemePreference.AMOLED) else lightScheme(selected)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = AbcTypography, shapes = AbcShapes, content = content)
}
