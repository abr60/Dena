package com.dena.ui.theme

import android.content.Context
import android.os.Build
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
import android.content.res.AssetManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
// Explicit neutral primaryContainer so FABs stay neutral instead of
// inheriting Material 3 stock containers (lavender/purple), plus explicit
// neutral surfaceContainer ramp so dialogs and bottom sheets remain neutral.
private val DenaLightScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0E0), onPrimaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFF5F5F5), onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF0F0F0), onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8E8E8), onSurfaceVariant = Color(0xFF888888),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    outline = Color(0xFFD0D0D0), error = Color(0xFFE57373)
)
private val DenaDarkScheme = darkColorScheme(
    primary = Color(0xFFF0F0F0), onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A2A2A), onPrimaryContainer = Color(0xFFF0F0F0),
    background = Color.Black, onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF0A0A0A), onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1A1A1A), onSurfaceVariant = Color(0xFF999999),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF060606),
    surfaceContainer = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    outline = Color(0xFF333333), error = Color(0xFFE57373)
)

fun buildDynamicScheme(ctx: Context, dark: Boolean): androidx.compose.material3.ColorScheme {
    val d = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    val base = if (dark) DenaDarkScheme else DenaLightScheme
    return base.copy(
        primary = d.primary,
        onPrimary = d.onPrimary,
        primaryContainer = d.primaryContainer,
        onPrimaryContainer = d.onPrimaryContainer,
        inversePrimary = d.inversePrimary,
        secondary = d.secondary,
        onSecondary = d.onSecondary,
        secondaryContainer = d.secondaryContainer,
        onSecondaryContainer = d.onSecondaryContainer,
        tertiary = d.tertiary,
        onTertiary = d.onTertiary,
        tertiaryContainer = d.tertiaryContainer,
        onTertiaryContainer = d.onTertiaryContainer,
        inverseSurface = d.inverseSurface,
        inverseOnSurface = d.inverseOnSurface,
        surfaceTint = d.primary,
        error = Color(0xFFE57373),
        onError = contrastOn(Color(0xFFE57373)),
        errorContainer = blend(Color(0xFFE57373), base.background, 0.25f),
        onErrorContainer = Color(0xFFE57373),
    )
}

fun buildFontFamily(fontKey: String, assets: AssetManager): FontFamily {
    return when (fontKey) {
        "jbmono" -> try {
            FontFamily(
                Font("fonts/JetBrainsMono-Regular.ttf", assets, FontWeight.Normal),
                Font("fonts/JetBrainsMono-Medium.ttf", assets, FontWeight.Medium),
                Font("fonts/JetBrainsMono-SemiBold.ttf", assets, FontWeight.SemiBold),
                Font("fonts/JetBrainsMono-Bold.ttf", assets, FontWeight.Bold),
            )
        } catch (_: Exception) { FontFamily.Monospace }
        "system" -> FontFamily.Default
        else -> try { // "spacegrotesk" default (also migrates legacy "inter")
            FontFamily(
                Font("fonts/SpaceGrotesk-Variable.ttf", assets, FontWeight.Normal),
                Font("fonts/SpaceGrotesk-Variable.ttf", assets, FontWeight.Medium),
                Font("fonts/SpaceGrotesk-Variable.ttf", assets, FontWeight.SemiBold),
                Font("fonts/SpaceGrotesk-Variable.ttf", assets, FontWeight.Bold),
            )
        } catch (_: Exception) { FontFamily.Default }
    }
}

@Composable
fun DenaTheme(
    themeMode: DenaThemeMode = DenaThemeMode.SYSTEM,
    palette: ThemePalette? = null,
    fontScale: Float = 1f,
    displayScale: Float = 1f,
    dynamicScheme: String = "system",
    // Talom clone — perfect word-for-word Follow System logic
    followSystemTheme: Boolean? = null,
    dynamicColorsEnabled: Boolean? = null,
    // Manual dark switch for pre-Android-10 (API < 29): no system dark mode exists there
    manualDark: Boolean? = null,
    fontKey: String = "spacegrotesk",
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    // Talom verbatim: val darkTheme = if (followSystemTheme) isSysDark else !isSysDark
    val darkTheme = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && manualDark != null -> manualDark
        followSystemTheme != null -> if (followSystemTheme) isSystemDark else !isSystemDark
        else -> when (themeMode) {
            DenaThemeMode.SYSTEM -> isSystemDark
            DenaThemeMode.LIGHT -> false
            DenaThemeMode.DARK -> true
            DenaThemeMode.DYNAMIC -> when (dynamicScheme.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }
        }
    }

    val effectiveDynamic = dynamicColorsEnabled ?: (themeMode == DenaThemeMode.DYNAMIC)
    val colorScheme = when {
        effectiveDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> buildDynamicScheme(ctx, darkTheme)
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
    val scaledDensity = Density(baseDensity.density * displayScale, baseDensity.fontScale * fontScale)

    val moneyPalette = remember(palette) {
        if (palette != null) {
            MoneyPalette(
                positive = palette.tokens.green,
                negative = palette.tokens.red,
            )
        } else {
            MoneyPalette(
                positive = Color(0xFF81C784),
                negative = Color(0xFFE57373),
            )
        }
    }

    val fontFamily = remember(fontKey) { buildFontFamily(fontKey, ctx.assets) }
    val typography = MaterialTheme.typography.run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = fontFamily),
            displayMedium = displayMedium.copy(fontFamily = fontFamily),
            displaySmall = displaySmall.copy(fontFamily = fontFamily),
            headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
            headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
            headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
            titleLarge = titleLarge.copy(fontFamily = fontFamily),
            titleMedium = titleMedium.copy(fontFamily = fontFamily),
            titleSmall = titleSmall.copy(fontFamily = fontFamily),
            bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
            bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
            bodySmall = bodySmall.copy(fontFamily = fontFamily),
            labelLarge = labelLarge.copy(fontFamily = fontFamily),
            labelMedium = labelMedium.copy(fontFamily = fontFamily),
            labelSmall = labelSmall.copy(fontFamily = fontFamily),
        )
    }

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalMoneyPalette provides moneyPalette,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}
