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

    fun showDecimals(): Boolean = prefs.getBoolean(KEY_SHOW_DECIMALS, false)
    fun setShowDecimals(v: Boolean) { prefs.edit().putBoolean(KEY_SHOW_DECIMALS, v).apply() }

    fun showPercentage(): Boolean = prefs.getBoolean(KEY_SHOW_PERCENTAGE, false)
    fun setShowPercentage(v: Boolean) { prefs.edit().putBoolean(KEY_SHOW_PERCENTAGE, v).apply() }

    fun getThemeMode(): com.dena.ui.theme.DenaThemeMode {
        return try {
            com.dena.ui.theme.DenaThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, com.dena.ui.theme.DenaThemeMode.SYSTEM.name) ?: com.dena.ui.theme.DenaThemeMode.SYSTEM.name)
        } catch (e: Exception) { com.dena.ui.theme.DenaThemeMode.SYSTEM }
    }
    fun setThemeMode(mode: com.dena.ui.theme.DenaThemeMode) { prefs.edit().putString(KEY_THEME_MODE, mode.name).apply() }

    fun getFontScale(): Float {
        if (prefs.contains(KEY_FONT_SCALE)) return prefs.getFloat(KEY_FONT_SCALE, 1f)
        // migrate legacy small/medium/large
        val legacy = prefs.getString(KEY_FONT_SIZE, null)
        return when (legacy) { "small" -> 0.85f; "large" -> 1.18f; else -> 1f }
    }
    fun setFontScale(v: Float) { prefs.edit().putFloat(KEY_FONT_SCALE, v).apply() }

    fun getDisplayScale(): Float = prefs.getFloat(KEY_DISPLAY_SCALE, 1f)
    fun setDisplayScale(v: Float) { prefs.edit().putFloat(KEY_DISPLAY_SCALE, v).apply() }

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

    // Manual dark theme for pre-Android-10 (API < 29) devices with no system dark mode;
    // defaults to light — the user flips to dark if they prefer
    fun getDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(v: Boolean) { prefs.edit().putBoolean(KEY_DARK_MODE, v).apply() }

    fun getDismissedUpdateTag(): String = prefs.getString(KEY_DISMISSED_UPDATE_TAG, "") ?: ""
    fun setDismissedUpdateTag(v: String) { prefs.edit().putString(KEY_DISMISSED_UPDATE_TAG, v).apply() }

    fun getLastUpdateCheck(): Long = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
    fun setLastUpdateCheck(v: Long) { prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, v).apply() }

    fun getLastNotifiedTag(): String = prefs.getString(KEY_LAST_NOTIFIED_TAG, "") ?: ""
    fun setLastNotifiedTag(v: String) { prefs.edit().putString(KEY_LAST_NOTIFIED_TAG, v).apply() }

    fun showDateHeaders(): Boolean = prefs.getBoolean(KEY_SHOW_DATE_HEADERS, false)
    fun setShowDateHeaders(v: Boolean) { prefs.edit().putBoolean(KEY_SHOW_DATE_HEADERS, v).apply() }

    fun showContactNumber(): Boolean = prefs.getBoolean(KEY_SHOW_CONTACT_NUMBER, false)
    fun setShowContactNumber(v: Boolean) { prefs.edit().putBoolean(KEY_SHOW_CONTACT_NUMBER, v).apply() }

    fun getAppFont(): String {
        val raw = prefs.getString(KEY_APP_FONT, FONT_SPACEGROTESK) ?: FONT_SPACEGROTESK
        // migrate legacy "inter" -> spacegrotesk
        return if (raw == FONT_INTER) FONT_SPACEGROTESK else raw
    }
    fun setAppFont(v: String) { prefs.edit().putString(KEY_APP_FONT, v).apply() }

    companion object {
        const val PREFS_NAME = "dena_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_FOLLOW_SYSTEM_THEME = "follow_system_theme"
        const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LANGUAGE = "language"
        const val KEY_CURRENCY = "currency"
        const val KEY_SHOW_DECIMALS = "show_decimals"
        const val KEY_PALETTE = "palette_id"
        const val KEY_PALETTE_ENABLED = "palette_enabled"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_DISPLAY_SCALE = "display_scale"
        const val KEY_DYNAMIC_SCHEME = "dynamic_scheme"
        const val DYNAMIC_SYSTEM = "system"
        const val DYNAMIC_LIGHT = "light"
        const val DYNAMIC_DARK = "dark"
        const val KEY_SHOW_PERCENTAGE = "show_percentage"
        const val KEY_HISTORY_CLEAN_DAYS = "history_clean_days"
        const val KEY_UNLOCKED = "unlocked"
        const val KEY_DISMISSED_UPDATE_TAG = "dismissed_update_tag"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_LAST_NOTIFIED_TAG = "last_notified_tag"
        const val KEY_SHOW_DATE_HEADERS = "show_date_headers"
        const val KEY_SHOW_CONTACT_NUMBER = "show_contact_number"
        const val KEY_APP_FONT = "app_font"
        const val FONT_INTER = "inter" // legacy, migrated to spacegrotesk
        const val FONT_SPACEGROTESK = "spacegrotesk"
        const val FONT_JBMONO = "jbmono"
        const val FONT_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_BN = "bn"
        const val CURRENCY_BDT = "BDT"
        const val CURRENCY_USD = "USD"
    }
}
