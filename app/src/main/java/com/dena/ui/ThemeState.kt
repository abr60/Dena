package com.dena.ui

import com.dena.ui.theme.DenaThemeMode

data class ThemeState(
    val themeMode: DenaThemeMode,
    val followSystemTheme: Boolean,
    val dynamicColorsEnabled: Boolean,
    val manualDark: Boolean,
    val paletteId: String,
    val paletteEnabled: Boolean,
    val dynamicScheme: String,
    val fontSize: String,
)
