package com.dena.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.dena.core.ThemePalette
import com.dena.core.PaletteTokens

data class MoneyPalette(val positive: Color, val negative: Color)
val LocalMoneyPalette = compositionLocalOf { MoneyPalette(Color(0xFF81C784), Color(0xFFE57373)) }

private fun contrastOn(c: Color): Color = if (c.luminance() < 0.5f) Color.White else Color(0xFF1A1A1A)

private fun blend(c1: Color, c2: Color, ratio: Float): Color {
    val r = c1.red * ratio + c2.red * (1 - ratio)
    val g = c1.green * ratio + c2.green * (1 - ratio)
    val b = c1.blue * ratio + c2.blue * (1 - ratio)
    return Color(r, g, b)
}

fun buildColorScheme(palette: ThemePalette): androidx.compose.material3.ColorScheme {
    val t = palette.tokens
    val dark = palette.isDark
    
    val base = if (dark) darkColorScheme() else lightColorScheme()
    
    return base.copy(
        primary = t.accent,
        onPrimary = contrastOn(t.accent),
        primaryContainer = blend(t.accent, t.background, 0.25f),
        onPrimaryContainer = t.foreground,
        
        secondary = if (dark) t.green else t.cyan,
        onSecondary = contrastOn(if (dark) t.green else t.cyan),
        secondaryContainer = blend(if (dark) t.green else t.cyan, t.background, 0.25f),
        onSecondaryContainer = t.foreground,
        
        tertiary = if (dark) t.magenta else t.orange,
        onTertiary = contrastOn(if (dark) t.magenta else t.orange),
        tertiaryContainer = blend(if (dark) t.magenta else t.orange, t.background, 0.25f),
        onTertiaryContainer = t.foreground,
        
        background = t.background,
        onBackground = t.foreground,
        surface = t.background,
        onSurface = t.foreground,
        surfaceVariant = if (dark) t.lighterBg else t.darkerBg,
        onSurfaceVariant = t.muted,
        outline = t.muted,
        outlineVariant = t.selection,
        
        error = t.red,
        onError = contrastOn(t.red)
    )
}

enum class DenaThemeMode { SYSTEM, LIGHT, DARK, DYNAMIC }

// Monochrome fallback
private val DenaLightScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A), onPrimary = Color.White,
    background = Color(0xFFF5F5F5), onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF0F0F0), onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8E8E8), onSurfaceVariant = Color(0xFF888888),
    outline = Color(0xFFD0D0D0), error = Color(0xFFE57373)
)
private val DenaDarkScheme = darkColorScheme(
    primary = Color(0xFFF0F0F0), onPrimary = Color.Black,
    background = Color.Black, onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF0A0A0A), onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1A1A1A), onSurfaceVariant = Color(0xFF999999),
    outline = Color(0xFF333333), error = Color(0xFFE57373)
)

fun fontScaleFor(size: String): Float = when (size.lowercase()) {
    "small" -> 0.85f
    "large" -> 1.18f
    else -> 1f
}

@Composable
fun DenaTheme(
    themeMode: DenaThemeMode,
    palette: ThemePalette?,
    fontSize: String = "medium",
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        DenaThemeMode.SYSTEM -> isSystemDark
        DenaThemeMode.LIGHT -> false
        DenaThemeMode.DARK -> true
        DenaThemeMode.DYNAMIC -> isSystemDark
    }

    val colorScheme = when {
        themeMode == DenaThemeMode.DYNAMIC -> if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        palette != null -> buildColorScheme(palette)
        darkTheme -> DenaDarkScheme
        else -> DenaLightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val baseDensity = LocalDensity.current
    val scale = fontScaleFor(fontSize)
    val scaledDensity = Density(baseDensity.density, baseDensity.fontScale * scale)

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        androidx.compose.runtime.CompositionLocalProvider(LocalMoneyPalette provides MoneyPalette(Color(0xFF81C784), Color(0xFFE57373))) {
            MaterialTheme(colorScheme = colorScheme, content = content)
        }
    }
}
