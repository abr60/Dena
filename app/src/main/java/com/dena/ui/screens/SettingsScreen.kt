package com.dena.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dena.BuildConfig
import com.dena.core.*
import com.dena.data.DenaDatabase
import com.dena.ui.components.CurrencyPickerDialog
import com.dena.ui.components.DenaSelect
import com.dena.ui.components.LanguagePickerDialog
import com.dena.ui.components.NavRow
import com.dena.ui.components.SectionHeader
import com.dena.ui.components.SelectOption
import com.dena.ui.components.SettingsGroup
import com.dena.ui.components.SettingsRow
import com.dena.ui.components.SettingsSubpageScaffold
import com.dena.ui.components.ToggleRow
import com.dena.ui.components.DynamicSchemePickerSheet
import com.dena.ui.theme.DenaThemeMode
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SettingsScreen(
    themeMode: DenaThemeMode,
    onThemeChange: (DenaThemeMode) -> Unit,
    paletteId: String,
    onPaletteChange: (String) -> Unit,
    onPaletteEnabledChange: (Boolean) -> Unit,
    paletteEnabled: Boolean,
    fontScale: Float = 1f,
    onFontScaleChange: (Float) -> Unit = {},
    displayScale: Float = 1f,
    onDisplayScaleChange: (Float) -> Unit = {},
    dynamicScheme: String = "system",
    onDynamicSchemeChange: (String) -> Unit = {},
    // Talom clone — Follow System verbatim (added, default keeps backward compat)
    followSystemTheme: Boolean = true,
    onFollowSystemThemeChange: (Boolean) -> Unit = {},
    dynamicColorsEnabled: Boolean = false,
    onDynamicColorsChange: (Boolean) -> Unit = {},
    // Pre-Android-10 manual switch (light by default)
    manualDark: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    fontKey: String = "spacegrotesk",
    onFontKeyChange: (String) -> Unit = {},
    database: DenaDatabase? = null,
) {
    val context = LocalContext.current
    val prefs = remember { DenaPreferences(context) }
    var subpage by remember { mutableStateOf<String?>(null) }
    var isUnlocked by remember { mutableStateOf(prefs.isUnlocked()) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapMs by remember { mutableStateOf(0L) }
    var showDataSheet by remember { mutableStateOf(false) }
    
    val currentPalette = PaletteRegistry.find(paletteId)

    BackHandler(enabled = subpage != null) { subpage = null }

    AnimatedContent(
        targetState = subpage,
        transitionSpec = {
            val isEnter = targetState != null && initialState == null
            val isPop = targetState == null && initialState != null
            when {
                isEnter -> slideInHorizontally(tween(260), initialOffsetX = { it / 3 }) + fadeIn(tween(220)) togetherWith
                    slideOutHorizontally(tween(260), targetOffsetX = { -it / 3 }) + fadeOut(tween(220))
                isPop -> slideInHorizontally(tween(260), initialOffsetX = { -it / 3 }) + fadeIn(tween(220)) togetherWith
                    slideOutHorizontally(tween(260), targetOffsetX = { it / 3 }) + fadeOut(tween(220))
                else -> slideInHorizontally(tween(260), initialOffsetX = { it / 3 }) + fadeIn(tween(220)) togetherWith
                    slideOutHorizontally(tween(260), targetOffsetX = { -it / 3 }) + fadeOut(tween(220))
            }
        },
        label = "SettingsRoute",
    ) { target ->
        when (target) {
            "activity" -> RecentActivitySubpage(database = database, onBack = { subpage = null })
            "appearance" -> AppearanceSubpage(
                themeMode, onThemeChange, paletteId, onPaletteChange,
                onPaletteEnabledChange, paletteEnabled, isUnlocked,
                fontScale, onFontScaleChange,
                displayScale, onDisplayScaleChange,
                dynamicScheme, onDynamicSchemeChange,
                followSystemTheme, onFollowSystemThemeChange,
                dynamicColorsEnabled, onDynamicColorsChange,
                manualDark, onDarkModeChange,
                fontKey, onFontKeyChange,
                onUnlock = { isUnlocked = true; prefs.setUnlocked(true) }
            ) { subpage = null }
            "userinterface" -> UserPreferencesSubpage(database, onBack = { subpage = null })
            "update" -> UpdateSubpage(onBack = { subpage = null })
            "about" -> AboutSubpage(onBack = { subpage = null })
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapMs > 700) tapCount = 1 else tapCount++
                            lastTapMs = now
                            if (tapCount >= 4) {
                                tapCount = 0
                                lastTapMs = 0L
                                isUnlocked = !isUnlocked
                                prefs.setUnlocked(isUnlocked)
                                Toast.makeText(
                                    context,
                                    if (isUnlocked) "Advanced theme engine unlocked!" else "Advanced theme engine hidden",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                )

                SettingsGroup {
                    NavRow(label = "Appearance", icon = Icons.Filled.Palette, caption = "Theme, scale & colors", onClick = { subpage = "appearance" })
                    NavRow(label = "Language & Formatting", icon = Icons.Filled.Language, caption = "Language, currency, numbers", onClick = { subpage = "userinterface" }, showDivider = true)
                }

                SettingsGroup {
                    NavRow(label = "Data", icon = Icons.Filled.Storage, caption = "Backup & restore", onClick = { showDataSheet = true }, showDivider = true)
                    NavRow(label = "Recent Activity", icon = Icons.AutoMirrored.Filled.List, caption = "All transactions & retention", onClick = { subpage = "activity" }, showDivider = true)
                }

                SettingsGroup {
                    NavRow(label = "App Updates", icon = Icons.Filled.SystemUpdate, caption = "v${BuildConfig.VERSION_NAME} • Check for updates", onClick = { subpage = "update" }, showDivider = true)
                    NavRow(label = "Feedback", icon = Icons.Filled.Email, caption = "Report a bug or suggest a feature", onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/abr60/Dena/issues/new/choose"))) }, showDivider = true)
                    NavRow(label = "About", icon = Icons.Filled.Info, caption = "Our story, motto & version", onClick = { subpage = "about" })
                }
            }
        }
    }

    if (showDataSheet) {
        DataBackupBottomSheet(database = database, onDismiss = { showDataSheet = false })
    }
}

@Composable
fun AppearanceSubpage(
    themeMode: DenaThemeMode, onThemeChange: (DenaThemeMode) -> Unit,
    paletteId: String, onPaletteChange: (String) -> Unit,
    onPaletteEnabledChange: (Boolean) -> Unit,
    paletteEnabled: Boolean,
    isUnlocked: Boolean,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    displayScale: Float,
    onDisplayScaleChange: (Float) -> Unit,
    dynamicScheme: String,
    onDynamicSchemeChange: (String) -> Unit,
    // Talom clone — verbatim Follow System
    followSystemTheme: Boolean = true,
    onFollowSystemThemeChange: (Boolean) -> Unit = {},
    dynamicColorsEnabled: Boolean = false,
    onDynamicColorsChange: (Boolean) -> Unit = {},
    manualDark: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    fontKey: String = "spacegrotesk",
    onFontKeyChange: (String) -> Unit = {},
    onUnlock: () -> Unit,
    onBack: () -> Unit
) {
    SettingsSubpageScaffold(title = "Appearance", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionHeader("Appearance")
            SettingsGroup {
                // Android 10+ has a system dark mode to follow; older devices get a manual switch
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Talom clone — verbatim Appearance > Follow system (word-for-word strings & logic)
                    ToggleRow(
                        label = "Follow system",
                        caption = if (followSystemTheme) "Uses your system theme" else "Uses the opposite of your system theme",
                        checked = followSystemTheme,
                        onCheckedChange = onFollowSystemThemeChange,
                    )
                } else {
                    ToggleRow(
                        label = "Dark theme",
                        caption = if (manualDark) "Using dark interface" else "Using light interface",
                        checked = manualDark,
                        onCheckedChange = onDarkModeChange,
                    )
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("SCALE")
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Font size slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Font size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${(fontScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = fontScale,
                                onValueChange = onFontScaleChange,
                                valueRange = 0.85f..1.30f,
                                steps = 6,
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Small", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Large", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        // Display size slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Display size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${(displayScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = displayScale,
                                onValueChange = onDisplayScaleChange,
                                valueRange = 0.85f..1.30f,
                                steps = 6,
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Small", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Large", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Live preview
                        Text(
                            "Preview: The quick brown fox ৳1,200.50",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("FONT")
                val fontOptions = listOf(
                    SelectOption(label = "Space Grotesk", originalIndex = 0),
                    SelectOption(label = "JetBrains Mono", originalIndex = 1),
                    SelectOption(label = "System default", originalIndex = 2),
                )
                val currentFontLabel = when (fontKey) { "jbmono" -> "JetBrains Mono"; "system" -> "System default"; else -> "Space Grotesk" }
                DenaSelect(
                    value = currentFontLabel,
                    options = fontOptions,
                    onSelect = { idx ->
                        val key = when (idx) { 1 -> "jbmono"; 2 -> "system"; else -> "spacegrotesk" }
                        onFontKeyChange(key)
                    },
                    placeholder = "Select Font",
                )
            }

            // Talom clone — verbatim Advanced > Dynamic colors (word-for-word strings & logic)
            if (isUnlocked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Advanced")
                    SettingsGroup {
                        ToggleRow(
                            label = "Dynamic colors (Material You)",
                            caption = "Derives accents from your wallpaper",
                            checked = dynamicColorsEnabled,
                            onCheckedChange = onDynamicColorsChange,
                        )
                    }
                }
            }
            if (dynamicColorsEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                var showDynamicSchemeSheet by remember { mutableStateOf(false) }
                SectionHeader("MATERIAL YOU SCHEME")
                SettingsGroup {
                    Box(modifier = Modifier.fillMaxWidth().clickable { showDynamicSchemeSheet = true }) {
                        SettingsRow(
                            label = "Dynamic scheme",
                            value = when (dynamicScheme) { "light" -> "Light"; "dark" -> "Dark"; else -> "System" },
                            showDivider = false,
                        )
                    }
                }
                if (showDynamicSchemeSheet) {
                    DynamicSchemePickerSheet(
                        currentScheme = dynamicScheme,
                        onPick = { picked -> onDynamicSchemeChange(picked); showDynamicSchemeSheet = false },
                        onDismiss = { showDynamicSchemeSheet = false },
                    )
                }
            }
            if (isUnlocked) {
                SectionHeader("COLOR PALETTE")
                ToggleRow(label = "Enable Omarchy Palette", caption = "Apply curated color theme", checked = paletteEnabled, onCheckedChange = { 
                    onPaletteEnabledChange(it)
                    if (it) {
                        onDynamicColorsChange(false)
                        onThemeChange(DenaThemeMode.LIGHT) // Disable dynamic if enabling palette (legacy sync)
                    }
                })

                if (paletteEnabled) {
                    val paletteOptions = PaletteRegistry.all.mapIndexed { idx, p ->
                        SelectOption(
                            label = p.displayName,
                            swatches = listOf(p.tokens.red, p.tokens.green, p.tokens.yellow, p.tokens.cyan, p.tokens.magenta),
                            group = p.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            originalIndex = idx,
                        )
                    }
                    val currentPalette = PaletteRegistry.find(paletteId) ?: PaletteRegistry.all.first()

                    DenaSelect(
                        value = currentPalette.displayName,
                        options = paletteOptions,
                        onSelect = { onPaletteChange(PaletteRegistry.all[it].id) },
                        placeholder = "Select Palette"
                    )
                }
            }
        }
    }
}

@Composable
fun UserPreferencesSubpage(database: DenaDatabase?, onBack: () -> Unit) {
    SettingsSubpageScaffold(title = "Language & Formatting", onBack = onBack) {
        val context = LocalContext.current
        val prefs = remember { DenaPreferences(context) }
        var language by remember { mutableStateOf(prefs.getLanguage()) }
        var showDecimals by remember { mutableStateOf(prefs.showDecimals()) }
        var currency by remember { mutableStateOf(prefs.getCurrency()) }
        var showCurrencyPicker by remember { mutableStateOf(false) }
        var showLanguagePicker by remember { mutableStateOf(false) }
        var showPercentage by remember { mutableStateOf(prefs.showPercentage()) }
        var showContactNumber by remember { mutableStateOf(prefs.showContactNumber()) }
        var showDateHeaders by remember { mutableStateOf(prefs.showDateHeaders()) }

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionHeader("LOCALIZATION")
            SettingsGroup {
                Box(modifier = Modifier.fillMaxWidth().clickable { showLanguagePicker = true }) {
                    SettingsRow(
                        label = "Language",
                        value = if (language == DenaPreferences.LANG_EN) "English" else "বাংলা",
                        showDivider = true
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().clickable { showCurrencyPicker = true }) {
                    SettingsRow(
                        label = "Currency",
                        value = CurrencyRegistry.shortLabel(currency),
                        showDivider = true
                    )
                }
                ToggleRow(label = "Show decimals", caption = "e.g. ${prefs.getCurrencySymbol()} 1,200.00 vs ${prefs.getCurrencySymbol()} 1,200", checked = showDecimals, onCheckedChange = { showDecimals = it; prefs.setShowDecimals(it) }, showDivider = true)
                ToggleRow(
                    label = "Show contact number",
                    caption = "Display phone number on debt cards",
                    checked = showContactNumber,
                    onCheckedChange = { showContactNumber = it; prefs.setShowContactNumber(it) },
                    showDivider = false,
                )
            }
            if (showLanguagePicker) {
                LanguagePickerDialog(currentLanguage = language, onPick = { picked -> language = picked; prefs.setLanguage(picked); showLanguagePicker = false }, onDismiss = { showLanguagePicker = false })
            }
            if (showCurrencyPicker) {
                CurrencyPickerDialog(currentCode = currency, onPick = { picked -> currency = picked; prefs.setCurrency(picked); showCurrencyPicker = false }, onDismiss = { showCurrencyPicker = false })
            }

            SectionHeader("FORMATTING")
            SettingsGroup {
                ToggleRow(
                    label = "Show percentages",
                    caption = "Show paid % on debtor cards",
                    checked = showPercentage,
                    onCheckedChange = { showPercentage = it; prefs.setShowPercentage(it) },
                    showDivider = true
                )
                ToggleRow(
                    label = "By date",
                    caption = "Group debts by date with headers",
                    checked = showDateHeaders,
                    onCheckedChange = { showDateHeaders = it; prefs.setShowDateHeaders(it) },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
fun RecentActivitySubpage(database: DenaDatabase?, onBack: () -> Unit) {
    val context = LocalContext.current
    val txs by (database?.transactionDao()?.observeAll()?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val debtsMap = remember { mutableStateOf(mapOf<Long, String>()) }
    val prefs = remember { DenaPreferences(context) }
    var historyCleanDays by remember { mutableStateOf(prefs.getHistoryCleanDays()) }
    val filtered = remember(txs, historyCleanDays) { if (historyCleanDays == 0) txs else { val cutoff = System.currentTimeMillis() - (historyCleanDays.toLong() * 24 * 60 * 60 * 1000); txs.filter { it.timestamp >= cutoff } } }
    LaunchedEffect(txs) { if (database != null) { val debts = database.debtDao().getAllOnce(); debtsMap.value = debts.associate { it.id to it.contactName } } }
    SettingsSubpageScaffold(title = "Recent Activity", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("RETENTION")
            SettingsGroup {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Don't show activity older than: ${if (historyCleanDays == 0) "Never" else if (historyCleanDays >= 30 && historyCleanDays < 60) "1 month" else if (historyCleanDays >= 60 && historyCleanDays < 90) "2 months" else "3 months"}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = historyCleanDays.toFloat(),
                        onValueChange = {
                            val days = when {
                                it < 15 -> 0
                                it < 45 -> 30
                                it < 75 -> 60
                                else -> 90
                            }
                            historyCleanDays = days
                            prefs.setHistoryCleanDays(days)
                        },
                        valueRange = 0f..90f,
                        steps = 2,
                    )
                }
            }
        }
        if (filtered.isEmpty()) { val message = if (historyCleanDays > 0) "No activity in the last $historyCleanDays days" else "No transactions yet"; Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val showDecimals = prefs.showDecimals()
                val sym = prefs.getCurrencySymbol()
                filtered.forEach { t ->
                    val name = debtsMap.value[t.debtId] ?: "Debt #${t.debtId}"
                    ActivityRow(
                        contactName = name,
                        direction = t.direction,
                        note = t.note,
                        amount = t.amount,
                        timestamp = t.timestamp,
                        currencySymbol = sym,
                        showDecimals = showDecimals,
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSubpage(onBack: () -> Unit) {
    SettingsSubpageScaffold(title = "About", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Hero
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "DENA",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Keep every promise accounted for.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Dena v${BuildConfig.VERSION_NAME} • build ${BuildConfig.VERSION_CODE}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            SectionHeader("WHAT IS DENA")
            Text(
                "Dena comes from the Bengali word for debt — but also for what is owed in trust. It is the quiet companion that remembers what memory shouldn't have to: every taka lent, every promise made, every handshake between friends that deserves a clear, honest record. Offline-first, on your device, yours completely.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SectionHeader("FEATURES")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•  Two views, one truth — \"I Lent\" and \"I Borrowed\" keep each side of your ledger crisp and separate.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Full history you can edit — every loan starts as its own transaction; tap any entry to correct or delete it and the remaining balance recalculates itself.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Every currency, one tap — searchable picker with dozens of world currencies, defaulting to Bangladeshi Taka (৳), so a loan in dollars or dinar needs no mental math.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Due dates — or none at all — set a deadline or keep the loan beautifully open-ended.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Dressed for your mood — Material You dynamic colors, 20+ curated palettes, light/dark override, and text scaling from small to large.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  English and বাংলা — the whole app speaks the language you trust, with proper Taka formatting.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Yours even after loss — JSON backup & restore, plus per-debt PDF and CSV statements you can share.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Featherweight & fast — 1.3 MB, opens in a blink, runs on Android 7.0+.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            SectionHeader("PRINCIPLES")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•  Private by default — your debts stay on your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Honest by design — no dark patterns, no paywalls for essentials.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Calm, not cold — finance without anxiety.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Built to last — no ads, no tracking, no cloud required.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Psst — try tapping the DENA title 4 times", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Four quick taps on the DENA header in Settings unlocks the Advanced theme engine — dynamic colors, palette picker, and all the hidden customizations. Tap four times again to hide it. A little easter egg for the curious.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                "Made with ♥ for those who keep their word.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupBottomSheet(database: DenaDatabase?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contentResolver = context.contentResolver
    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val debts = database?.debtDao()?.getAllOnce() ?: emptyList()
                    val txs = database?.transactionDao()?.getAllOnce() ?: emptyList()
                    val json = BackupHelper.exportProfileToJson(debts, txs)
                    contentResolver.openOutputStream(uri)?.use { output -> output.write(json.toByteArray()) }
                    val filename = uri.lastPathSegment?.substringAfterLast('/') ?: "backup.json"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup saved as $filename to your chosen location", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (content == null) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid backup file", Toast.LENGTH_LONG).show() }
                        return@launch
                    }
                    val decoded = BackupHelper.decodeBackupString(content)
                    if (decoded == null) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid backup file", Toast.LENGTH_LONG).show() }
                        return@launch
                    }
                    val debts = mutableListOf<com.dena.data.debt.Debt>()
                    val txs = mutableListOf<com.dena.data.transaction.Transaction>()
                    var importError: String? = null
                    val ok = BackupHelper.importProfileFromString(decoded, { d -> debts.addAll(d) }, { t -> txs.addAll(t) })
                    if (!ok) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Import failed: invalid backup format", Toast.LENGTH_LONG).show() }
                        return@launch
                    }
                    try {
                        val db = database ?: throw IllegalStateException("Database unavailable")
                        db.withTransaction {
                            val existing = db.debtDao().getAllOnce()
                            val existingKeys = existing.map { "${it.contactName}|${it.direction}|${it.principalAmount}|${it.creationDate}" }.toSet()
                            val idMap = mutableMapOf<Long, Long>()
                            for (d in debts) {
                                val key = "${d.contactName}|${d.direction}|${d.principalAmount}|${d.creationDate}"
                                if (key in existingKeys) {
                                    val match = existing.first { "${it.contactName}|${it.direction}|${it.principalAmount}|${it.creationDate}" == key }
                                    idMap[d.id] = match.id
                                } else {
                                    val newId = db.debtDao().insert(d.copy(id = 0))
                                    idMap[d.id] = newId
                                }
                            }
                            for (t in txs) {
                                val mappedDebtId = idMap[t.debtId] ?: t.debtId
                                // skip if debt not found after mapping
                                if (db.debtDao().getById(mappedDebtId) == null) continue
                                db.transactionDao().insert(t.copy(id = 0, debtId = mappedDebtId))
                            }
                        }
                    } catch (e: Exception) {
                        importError = e.message
                    }
                    withContext(Dispatchers.Main) {
                        if (importError != null) Toast.makeText(context, "Import failed: $importError", Toast.LENGTH_LONG).show()
                        else Toast.makeText(context, "Restored ${debts.size} debts & ${txs.size} transactions — restart app", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Saves and restores all debts, transactions, and settings as a JSON file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("dena-backup-$today.json")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Export Backup", fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Restore Backup", fontWeight = FontWeight.Medium) }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActivityRow(
    contactName: String,
    direction: String,
    note: String,
    amount: Double,
    timestamp: Long,
    currencySymbol: String,
    showDecimals: Boolean,
) {
    val activityCardContainer = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = activityCardContainer,
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$contactName • $direction",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = note.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrencyRaw(amount, currencySymbol, showDecimals),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatRelativeDate(timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
