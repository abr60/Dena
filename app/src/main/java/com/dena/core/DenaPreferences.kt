package com.dena.core

import android.content.Context

class DenaPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, LANG_EN) ?: LANG_EN
    fun setLanguage(value: String) { prefs.edit().putString(KEY_LANGUAGE, value).apply() }

    fun getCurrency(): String {
        val raw = prefs.getString(KEY_CURRENCY, CURRENCY_BDT) ?: CURRENCY_BDT
        // migrate old symbol storage if any
        return when (raw) {
            "৳", "$" -> if (raw == "৳") CURRENCY_BDT else CURRENCY_USD
            else -> raw.uppercase(java.util.Locale.US)
        }
    }
    fun setCurrency(value: String) { prefs.edit().putString(KEY_CURRENCY, value.uppercase(java.util.Locale.US)).apply() }
    fun getCurrencySymbol(): String = CurrencyRegistry.symbolFor(getCurrency())

    fun showDecimals(): Boolean = prefs.getBoolean(KEY_SHOW_DECIMALS, true)
    fun setShowDecimals(v: Boolean) { prefs.edit().putBoolean(KEY_SHOW_DECIMALS, v).apply() }

    fun showPercentage(): Boolean = prefs.getBoolean(KEY_SHOW_PERCENTAGE, false)
    fun setShowPercentage(v: Boolean) { prefs.edit().putBoolean(KEY_SHOW_PERCENTAGE, v).apply() }

    fun getThemeMode(): com.dena.ui.theme.DenaThemeMode {
        return try {
            com.dena.ui.theme.DenaThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, com.dena.ui.theme.DenaThemeMode.SYSTEM.name) ?: com.dena.ui.theme.DenaThemeMode.SYSTEM.name)
        } catch (e: Exception) { com.dena.ui.theme.DenaThemeMode.SYSTEM }
    }
    fun setThemeMode(mode: com.dena.ui.theme.DenaThemeMode) { prefs.edit().putString(KEY_THEME_MODE, mode.name).apply() }

    fun getFontSize(): String = prefs.getString(KEY_FONT_SIZE, "medium") ?: "medium"
    fun setFontSize(v: String) { prefs.edit().putString(KEY_FONT_SIZE, v).apply() }

    fun getHistoryCleanDays(): Int = prefs.getInt(KEY_HISTORY_CLEAN_DAYS, 0)
    fun setHistoryCleanDays(v: Int) { prefs.edit().putInt(KEY_HISTORY_CLEAN_DAYS, v).apply() }

    fun getPaletteId(): String = prefs.getString(KEY_PALETTE, "") ?: ""
    fun setPaletteId(v: String) { prefs.edit().putString(KEY_PALETTE, v).apply() }
    fun isPaletteEnabled(): Boolean = prefs.getBoolean(KEY_PALETTE_ENABLED, false)
    fun setPaletteEnabled(v: Boolean) { prefs.edit().putBoolean(KEY_PALETTE_ENABLED, v).apply() }

    // Material You scheme override: "system" (follow system), "light", "dark"
    fun getDynamicScheme(): String = prefs.getString(KEY_DYNAMIC_SCHEME, DYNAMIC_SYSTEM) ?: DYNAMIC_SYSTEM
    fun setDynamicScheme(v: String) { prefs.edit().putString(KEY_DYNAMIC_SCHEME, v.lowercase(java.util.Locale.US)).apply() }

    fun isUnlocked(): Boolean = prefs.getBoolean(KEY_UNLOCKED, false)
    fun setUnlocked(v: Boolean) { prefs.edit().putBoolean(KEY_UNLOCKED, v).apply() }

    // Talom clone: Follow system + Dynamic colors (boolean) — verbatim keys & defaults
    fun isFollowSystemTheme(): Boolean {
        // migrate legacy themeMode if new key not yet written
        if (!prefs.contains(KEY_FOLLOW_SYSTEM_THEME) && prefs.contains(KEY_THEME_MODE)) {
            return getThemeMode() == com.dena.ui.theme.DenaThemeMode.SYSTEM
        }
        return prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, true)
    }
    fun setFollowSystemTheme(v: Boolean) { prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM_THEME, v).apply() }

    fun isDynamicColorsEnabled(): Boolean = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
    fun setDynamicColorsEnabled(v: Boolean) { prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, v).apply() }

    companion object {
        const val PREFS_NAME = "dena_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_FOLLOW_SYSTEM_THEME = "follow_system_theme"
        const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        const val KEY_LANGUAGE = "language"
        const val KEY_CURRENCY = "currency"
        const val KEY_SHOW_DECIMALS = "show_decimals"
        const val KEY_PALETTE = "palette_id"
        const val KEY_PALETTE_ENABLED = "palette_enabled"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_DYNAMIC_SCHEME = "dynamic_scheme"
        const val DYNAMIC_SYSTEM = "system"
        const val DYNAMIC_LIGHT = "light"
        const val DYNAMIC_DARK = "dark"
        const val KEY_SHOW_PERCENTAGE = "show_percentage"
        const val KEY_HISTORY_CLEAN_DAYS = "history_clean_days"
        const val KEY_UNLOCKED = "unlocked"
        const val LANG_EN = "en"
        const val LANG_BN = "bn"
        const val CURRENCY_BDT = "BDT"
        const val CURRENCY_USD = "USD"
    }
}
