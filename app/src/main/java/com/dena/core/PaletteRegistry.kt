package com.dena.core

import androidx.compose.ui.graphics.Color

enum class ThemeCategory { EVERYDAY, MOOD, RETRO, MINIMAL }

data class PaletteTokens(
    val accent: Color, val background: Color, val foreground: Color,
    val selection: Color, val muted: Color,
    val darkerBg: Color, val lighterBg: Color,
    val red: Color, val green: Color, val cyan: Color,
    val yellow: Color, val magenta: Color, val orange: Color
)

data class ThemePalette(
    val id: String, val displayName: String, val category: ThemeCategory,
    val isDark: Boolean, val tokens: PaletteTokens
)

object PaletteRegistry {
    // Helper for ANSI mapping of legacy themes
    private fun legacy(
        accent: String, background: String, foreground: String,
        selectionBg: String, selectionFg: String,
        color1: String, color2: String, color3: String, color4: String, color5: String, color6: String, color8: String,
        isDark: Boolean
    ) = PaletteTokens(
        accent = Color(android.graphics.Color.parseColor(accent)),
        background = Color(android.graphics.Color.parseColor(background)),
        foreground = Color(android.graphics.Color.parseColor(foreground)),
        selection = Color(android.graphics.Color.parseColor(selectionBg)),
        muted = Color(android.graphics.Color.parseColor(color8)),
        darkerBg = Color(android.graphics.Color.parseColor(background)), // Fallback
        lighterBg = Color(android.graphics.Color.parseColor(selectionBg)), // Fallback
        red = Color(android.graphics.Color.parseColor(color1)),
        green = Color(android.graphics.Color.parseColor(color2)),
        cyan = Color(android.graphics.Color.parseColor(color6)),
        yellow = Color(android.graphics.Color.parseColor(color3)),
        magenta = Color(android.graphics.Color.parseColor(color5)),
        orange = Color(android.graphics.Color.parseColor(color3)) // Fallback to yellow
    )

    // A helper for modern v4 tokens
    private fun v4(
        accent: String, background: String, foreground: String, selection: String, muted: String,
        darkerBg: String, lighterBg: String, red: String, green: String, cyan: String,
        yellow: String, magenta: String, orange: String, isDark: Boolean
    ) = PaletteTokens(
        Color(android.graphics.Color.parseColor(accent)), Color(android.graphics.Color.parseColor(background)),
        Color(android.graphics.Color.parseColor(foreground)), Color(android.graphics.Color.parseColor(selection)),
        Color(android.graphics.Color.parseColor(muted)), Color(android.graphics.Color.parseColor(darkerBg)),
        Color(android.graphics.Color.parseColor(lighterBg)), Color(android.graphics.Color.parseColor(red)),
        Color(android.graphics.Color.parseColor(green)), Color(android.graphics.Color.parseColor(cyan)),
        Color(android.graphics.Color.parseColor(yellow)), Color(android.graphics.Color.parseColor(magenta)),
        Color(android.graphics.Color.parseColor(orange))
    )

    val all: List<ThemePalette> = listOf(
        // Belly (Local V4)
        ThemePalette("belly", "Belly", ThemeCategory.EVERYDAY, true, v4("#99a5be", "#0c0c15", "#cbcdb3", "#cbcdb3", "#65656b", "#06060b", "#24242c", "#a07870", "#7e7d4f", "#87a398", "#95947a", "#917b8e", "#ae8c85", true)),
        // Canvas (Local V4)
        ThemePalette("canvas", "Canvas", ThemeCategory.EVERYDAY, true, v4("#9ca4bb", "#0e0e05", "#ddf7ff", "#1f253a", "#7a8a9a", "#050502", "#1e1e0a", "#a05b5b", "#5b9a7a", "#5ba5a5", "#a5a55b", "#a55ba5", "#a57a5b", true)),
        // Catppuccin (V4)
        ThemePalette("catppuccin", "Catppuccin", ThemeCategory.EVERYDAY, true, v4("#89b4fa", "#1e1e2e", "#cdd6f4", "#45475a", "#585b70", "#101019", "#313244", "#f38ba8", "#a6e3a1", "#94e2d5", "#f9e2af", "#f5c2e7", "#f6b6ab", true)),
        // Catppuccin Latte (V4)
        ThemePalette("catppuccin-latte", "Catppuccin Latte", ThemeCategory.EVERYDAY, false, v4("#1e66f5", "#eff1f5", "#4c4f69", "#acb0be", "#9ca0b0", "#e6e9ef", "#ccd0da", "#d20f39", "#40a02b", "#179299", "#df8e1d", "#8839ef", "#fe640b", false)),
        // Emerald (Local V4)
        ThemePalette("emerald", "Emerald", ThemeCategory.EVERYDAY, true, v4("#579789", "#000b01", "#ddf7ff", "#2d3450", "#579789", "#000500", "#1a2a1a", "#975757", "#579789", "#579789", "#979757", "#975797", "#977a57", true)),
        // Ethereal (V4)
        ThemePalette("ethereal", "Ethereal", ThemeCategory.MOOD, true, v4("#c47fd5", "#14141d", "#d1d5da", "#282835", "#5a5a6a", "#0b0b10", "#282835", "#e06c75", "#98c379", "#56b6c2", "#d19a66", "#c678dd", "#d19a66", true)),
        // Everforest (V4)
        ThemePalette("everforest", "Everforest", ThemeCategory.EVERYDAY, true, v4("#a7c080", "#2d353b", "#d3c6aa", "#475258", "#5a666e", "#232a2e", "#374247", "#e67e80", "#a7c080", "#7fbbb3", "#dbbc7f", "#d699b6", "#e69875", true)),
        // Flexoki Light (V4)
        ThemePalette("flexoki-light", "Flexoki Light", ThemeCategory.EVERYDAY, false, v4("#205EA6", "#FFFCF0", "#100F0F", "#CECDC3", "#B7B5AC", "#e5e2d8", "#E6E4D9", "#D14D41", "#879A39", "#3AA99F", "#D0A215", "#CE5D97", "#d0772b", false)),
        // Gruvbox (V4)
        ThemePalette("gruvbox", "Gruvbox", ThemeCategory.EVERYDAY, true, v4("#fabd2f", "#282828", "#ebdbb2", "#504945", "#7c6f64", "#1d2021", "#3c3836", "#cc241d", "#98971a", "#689d6a", "#d79921", "#b16286", "#d65d0e", true)),
        // Hackerman (V4)
        ThemePalette("hackerman", "Hackerman", ThemeCategory.MOOD, true, v4("#00FF41", "#000000", "#00FF41", "#222222", "#444444", "#000000", "#111111", "#FF0000", "#00FF41", "#00FFFF", "#FFFF00", "#FF00FF", "#FF8800", true)),
        // Harbor (Local LEG)
        ThemePalette("harbor", "Harbor", ThemeCategory.RETRO, true, legacy("#5e81ac", "#dfe4c4", "#1c2d28", "#5e81ac", "#1c2d28", "#b14752", "#556753", "#dc8164", "#4c6c94", "#8a5b81", "#3d727d", "#7d8794", true)),
        // Inky Pinky (Local LEG)
        ThemePalette("inkypinky", "Inky Pinky", ThemeCategory.MOOD, true, legacy("#7c7ca8", "#13131D", "#c8c8c8", "#c8c8c8", "#13131D", "#EA90A8", "#a6b2c7", "#D18BA2", "#7c7ca8", "#9f859f", "#919ab7", "#434353", true)),
        // Kanagawa (V4)
        ThemePalette("kanagawa", "Kanagawa", ThemeCategory.EVERYDAY, true, v4("#7e9cd8", "#1f1f28", "#dcd7ba", "#2a2a37", "#54546d", "#16161d", "#2d4f67", "#c34043", "#98bb6c", "#7fb4ca", "#e6c384", "#957fb8", "#ffa066", true)),
        // Last Horizon (V4)
        ThemePalette("last-horizon", "Last Horizon", ThemeCategory.MOOD, true, v4("#e95678", "#1c1e26", "#dcdfe7", "#282a36", "#44475a", "#15171e", "#282a36", "#f07178", "#c9d18e", "#56b6c2", "#f1fa8c", "#bd93f9", "#e68a6d", true)),
        // Lookup (Local V4)
        ThemePalette("lookup", "Lookup", ThemeCategory.MOOD, true, v4("#508188", "#000a01", "#ddf7ff", "#2d3450", "#508188", "#000500", "#1a2a1a", "#815050", "#508188", "#508188", "#818150", "#815081", "#817a50", true)),
        // Lumon (V4)
        ThemePalette("lumon", "Lumon", ThemeCategory.MOOD, true, v4("#FFD700", "#121212", "#E0E0E0", "#333333", "#555555", "#0a0a0a", "#1e1e1e", "#CF6679", "#81C784", "#4DD0E1", "#FFF176", "#BA68C8", "#FFB74D", true)),
        // Lupine (V4)
        ThemePalette("lupine", "Lupine", ThemeCategory.MOOD, true, v4("#9b8bb5", "#16161e", "#c8c8c8", "#232331", "#44445a", "#101014", "#232331", "#e27878", "#b4be82", "#84a598", "#e2a478", "#a093c7", "#e2a478", true)),
        // Matte Black (V4)
        ThemePalette("matte-black", "Matte Black", ThemeCategory.MOOD, true, v4("#e68e0d", "#121212", "#bebebe", "#2a2a2a", "#333333", "#0d0d0d", "#1e1e1e", "#D35F5F", "#FFC107", "#bebebe", "#b91c1c", "#D35F5F", "#c63d3d", true)),
        // Meischevias (Local V4)
        ThemePalette("meischevias", "Meischevias", ThemeCategory.MOOD, true, v4("#b8784b", "#000b01", "#ddf7ff", "#2d3450", "#b8784b", "#000500", "#1a2a1a", "#b84b4b", "#4bb84b", "#4bb8b8", "#b8b84b", "#b84bb8", "#b88a4b", true)),
        // Miasma (V4)
        ThemePalette("miasma", "Miasma", ThemeCategory.MOOD, true, v4("#6272a4", "#282a36", "#f8f8f2", "#44475a", "#6272a4", "#21222c", "#44475a", "#ff5555", "#50fa7b", "#8be9fd", "#f1fa8c", "#ff79c6", "#ffb86c", true)),
        // Nord (V4)
        ThemePalette("nord", "Nord", ThemeCategory.EVERYDAY, true, v4("#88c0d0", "#2e3440", "#d8dee9", "#434c5e", "#4c566a", "#262b33", "#3b4252", "#bf616a", "#a3be8c", "#88c0d0", "#ebcb8b", "#b48ead", "#d08770", true)),
        // Osaka Jade (V4)
        ThemePalette("osaka-jade", "Osaka Jade", ThemeCategory.EVERYDAY, true, v4("#8bba7f", "#111111", "#dcd7ba", "#222222", "#444444", "#0a0a0a", "#222222", "#c34043", "#98bb6c", "#7fb4ca", "#e6c384", "#957fb8", "#ffa066", true)),
        // Retro 82 (V4)
        ThemePalette("retro-82", "Retro 82", ThemeCategory.RETRO, true, v4("#c792ea", "#1a1a1a", "#ffffff", "#333333", "#555555", "#111111", "#333333", "#ff5370", "#c3e88d", "#89ddff", "#ffcb6b", "#c792ea", "#f78c6c", true)),
        // Ristretto (V4)
        ThemePalette("ristretto", "Ristretto", ThemeCategory.MOOD, true, v4("#d3c6aa", "#211f1c", "#d3c6aa", "#474542", "#5c5955", "#181715", "#3a3734", "#e67e80", "#a7c080", "#83c092", "#dbbc7f", "#d699b6", "#e69875", true)),
        // Rose Dune (Local LEG)
        ThemePalette("roseofdune", "Rose Dune", ThemeCategory.RETRO, false, legacy("#76634c", "#F5E6D3", "#35302a", "#c68d95", "#35302a", "#a02b16", "#9e4f5b", "#644535", "#76634c", "#78292e", "#713a56", "#C8AC86", false)),
        // Rosé Pine (V4)
        ThemePalette("rose-pine", "Rosé Pine", ThemeCategory.EVERYDAY, true, v4("#ebbcba", "#191724", "#e0def4", "#26233a", "#403d52", "#13101c", "#2a273f", "#eb6f92", "#9ccfd8", "#c4a7e7", "#f6c177", "#ebbcba", "#ea9a97", true)),
        // Solitude (V4)
        ThemePalette("solitude", "Solitude", ThemeCategory.MOOD, true, v4("#7c7c99", "#111111", "#eeeeee", "#222222", "#444444", "#0a0a0a", "#222222", "#e06c75", "#98c379", "#56b6c2", "#d19a66", "#c678dd", "#d19a66", true)),
        // The Greek (Local LEG)
        ThemePalette("thegreek", "The Greek", ThemeCategory.RETRO, false, legacy("#DE6A41", "#d0d0c8", "#242424", "#242424", "#d0d0c8", "#db0030", "#2e3125", "#51573b", "#DE6A41", "#43432b", "#6b1f2f", "#6b7360", false)),
        // Tokyo Night (V4)
        ThemePalette("tokyo-night", "Tokyo Night", ThemeCategory.EVERYDAY, true, v4("#7aa2f7", "#1a1b26", "#c0caf5", "#292e42", "#414868", "#16161e", "#292e42", "#f7768e", "#9ece6a", "#7dcfff", "#e0af68", "#bb9af7", "#ff9e64", true)),
        // Turbonite (Local LEG)
        ThemePalette("turbonite", "Turbonite", ThemeCategory.RETRO, true, legacy("#6E6A64", "#0B0C0C", "#eae9e3", "#eae9e3", "#0B0C0C", "#f24331", "#6E6A64", "#ed9a1d", "#847B4E", "#A7A483", "#EBD698", "#31363A", true)),
        // Vantablack (V4)
        ThemePalette("vantablack", "Vantablack", ThemeCategory.MOOD, true, v4("#8d8d8d", "#000000", "#ffffff", "#1a1a1a", "#7a7a7a", "#070707", "#1a1a1a", "#a4a4a4", "#b6b6b6", "#b0b0b0", "#cecece", "#9b9b9b", "#b9b9b9", true)),
        // White (V4)
        ThemePalette("white", "White", ThemeCategory.MINIMAL, false, v4("#6e6e6e", "#ffffff", "#000000", "#c0c0c0", "#808080", "#e8e8e8", "#c0c0c0", "#2a2a2a", "#3a3a3a", "#3e3e3e", "#4a4a4a", "#2e2e2e", "#3a3a3a", false)),
        // White Gold (Local LEG)
        ThemePalette("whitegold", "White Gold", ThemeCategory.MINIMAL, false, legacy("#005C32", "#DEDBC8", "#0c0c14", "#9a9078", "#0c0c14", "#4B0304", "#725c0a", "#2E311A", "#005C32", "#762b2f", "#3B3B3B", "#9a9078", false)),
    )
    fun find(id: String) = all.find { it.id == id }
}
