package com.dena.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dena.BuildConfig
import com.dena.core.*
import com.dena.data.DenaDatabase
import com.dena.ui.components.AppHeader
import com.dena.ui.components.CurrencyPickerDialog
import com.dena.ui.components.DenaSelect
import com.dena.ui.components.LanguagePickerDialog
import com.dena.ui.components.NavRow
import com.dena.ui.components.ScalePickerSheet
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    themeMode: DenaThemeMode,
    onThemeChange: (DenaThemeMode) -> Unit,
    paletteId: String,
    onPaletteChange: (String) -> Unit,
    onPaletteEnabledChange: (Boolean) -> Unit,
    paletteEnabled: Boolean,
    fontSize: String = "medium",
    onFontSizeChange: (String) -> Unit = {},
    dynamicScheme: String = "system",
    onDynamicSchemeChange: (String) -> Unit = {},
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

        when (subpage) {
        "activity" -> { RecentActivitySubpage(database = database, onBack = { subpage = null }); return }
        "appearance" -> { 
            AppearanceSubpage(
                themeMode, onThemeChange, paletteId, onPaletteChange,
                onPaletteEnabledChange, paletteEnabled, isUnlocked,
                fontSize, onFontSizeChange,
                dynamicScheme, onDynamicSchemeChange,
                onUnlock = { isUnlocked = true; prefs.setUnlocked(true) }
            ) { subpage = null }
            return 
        }
        "userinterface" -> { UserPreferencesSubpage(database, onBack = { subpage = null }); return }
        "update" -> { UpdateSubpage(onBack = { subpage = null }); return }
        "about" -> { AboutSubpage(onBack = { subpage = null }); return }
    }


    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Box(modifier = Modifier.clickable {
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
        }) {
            AppHeader(secondary = "")
        }
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        SettingsGroup {
            NavRow(label = "Appearance", icon = Icons.Filled.Settings, caption = "Theme, scale & colors", onClick = { subpage = "appearance" })
            NavRow(label = "Language & Formatting", icon = Icons.Filled.Person, caption = "Language, currency, numbers", onClick = { subpage = "userinterface" }, showDivider = true)
        }

        SettingsGroup {
            NavRow(label = "Data", icon = Icons.Filled.Info, caption = "Backup & restore", onClick = { showDataSheet = true }, showDivider = true)
            NavRow(label = "Recent Activity", icon = Icons.AutoMirrored.Filled.List, caption = "All transactions & retention", onClick = { subpage = "activity" }, showDivider = true)
        }

        SettingsGroup {
            NavRow(label = "App Updates", icon = Icons.Filled.Refresh, caption = "v${BuildConfig.VERSION_NAME} • Check for updates", onClick = { subpage = "update" }, showDivider = true)
            NavRow(label = "About", icon = Icons.Filled.Info, caption = "Our story, motto & version", onClick = { subpage = "about" })
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
    fontSize: String,
    onFontSizeChange: (String) -> Unit,
    dynamicScheme: String,
    onDynamicSchemeChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onBack: () -> Unit
) {
    SettingsSubpageScaffold(title = "Appearance", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionHeader("THEME")
            ToggleRow(label = "Follow system", caption = "On: match system light/dark. Off: use opposite.", checked = themeMode == DenaThemeMode.SYSTEM, onCheckedChange = { if (it) onThemeChange(DenaThemeMode.SYSTEM) else onThemeChange(DenaThemeMode.LIGHT) })
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var showScaleSheet by remember { mutableStateOf(false) }
                SectionHeader("SCALE")
                SettingsGroup {
                    Box(modifier = Modifier.fillMaxWidth().clickable { showScaleSheet = true }) {
                        SettingsRow(
                            label = "Scale",
                            value = when (fontSize) { "small" -> "Small"; "large" -> "Large"; else -> "Medium" },
                            showDivider = false,
                        )
                    }
                }
                if (showScaleSheet) {
                    ScalePickerSheet(
                        currentScale = fontSize,
                        onPick = { picked -> onFontSizeChange(picked); showScaleSheet = false },
                        onDismiss = { showScaleSheet = false },
                    )
                }
            }

            if (isUnlocked) {
                SectionHeader("ADVANCED")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    ToggleRow(label = "Dynamic colors (Material You)", caption = "Use wallpaper colors (Android 12+)", checked = themeMode == DenaThemeMode.DYNAMIC, onCheckedChange = {
                        if (it) {
                            onThemeChange(DenaThemeMode.DYNAMIC)
                            onPaletteEnabledChange(false)
                        } else {
                            onThemeChange(DenaThemeMode.LIGHT)
                        }
                    })

                    if (themeMode == DenaThemeMode.DYNAMIC) {
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
                }
                
                SectionHeader("COLOR PALETTE")
                ToggleRow(label = "Enable Omarchy Palette", caption = "Apply curated color theme", checked = paletteEnabled, onCheckedChange = { 
                    onPaletteEnabledChange(it)
                    if (it) {
                        onThemeChange(DenaThemeMode.LIGHT) // Disable dynamic if enabling palette
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
                ToggleRow(label = "Show decimals", caption = "e.g. ${prefs.getCurrencySymbol()} 1,200.00 vs ${prefs.getCurrencySymbol()} 1,200", checked = showDecimals, onCheckedChange = { showDecimals = it; prefs.setShowDecimals(it) }, showDivider = false)
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
        if (filtered.isEmpty()) { val message = if (historyCleanDays > 0) "No activity in the last $historyCleanDays days" else "No transactions yet"; Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { filtered.forEach { t -> val name = debtsMap.value[t.debtId] ?: "Debt #${t.debtId}"; Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text("$name • ${t.direction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(t.note.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium) }; Column(horizontalAlignment = Alignment.End) { val showDecimals = DenaPreferences(context).showDecimals(); val sym = DenaPreferences(context).getCurrencySymbol(); Text(formatCurrencyRaw(t.amount, sym, showDecimals), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text(formatRelativeDate(t.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } } }
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

            SectionHeader("OUR MOTTO")
            Text(
                "Dena comes from the Bengali word for debt — but also for what is owed in trust. Our motto is simple: clarity creates trust. Every taka, every promise, every handshake deserves a clear record, free from awkward reminders and forgotten details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SectionHeader("WHY DENA EXISTS")
            Text(
                "We built Dena because lending to friends and family shouldn't ruin relationships — and forgetting shouldn't either. Spreadsheets get lost. Notes apps get messy. Memory fades when you need it most.\n\nDena was born to be the quiet companion that remembers for you: offline-first, private, fast, and respectful of the trust you place in each other.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SectionHeader("BUILT WITH PASSION")
            Text(
                "Crafted with care for people who take their word seriously. Every card, every animation, every palette is chosen to feel calm and trustworthy — never stressful. No ads. No cloud. No tracking. Just you and the people you trust.\n\nWe obsess over the small things: the Bengali Taka symbol rendering just right, the overpaid state feeling relieved, not alarming, the joy of marking a debt settled. Dena is made to be opened, used in seconds, and closed — with peace of mind.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SectionHeader("PRINCIPLES")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•  Private by default — your debts stay on your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Honest by design — no dark patterns, no paywalls for essentials.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Calm, not cold — finance without anxiety.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("•  Built to last — export any time as PDF, CSV or .dena backup.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            // Editable footer hint
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Edit this freely", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Replace the texts above in SettingsScreen.kt → AboutSubpage with your own story. Version is shown automatically from BuildConfig.",
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
    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> if (uri != null) { scope.launch(Dispatchers.IO) { try { val debts = database?.debtDao()?.getAllOnce() ?: emptyList(); val txs = database?.transactionDao()?.getAllOnce() ?: emptyList(); val json = BackupHelper.exportProfileToJson(debts, txs); contentResolver.openOutputStream(uri)?.use { output -> output.write(json.toByteArray()) }; withContext(Dispatchers.Main) { Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show() } } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show() } } } } }
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { scope.launch(Dispatchers.IO) { try { val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }; if (content == null) { withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid backup file", Toast.LENGTH_SHORT).show() }; return@launch }; val decoded = BackupHelper.decodeBackupString(content); if (decoded == null) { withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid backup file", Toast.LENGTH_SHORT).show() }; return@launch }; val ok = BackupHelper.importProfileFromString(decoded, { debts -> scope.launch(Dispatchers.IO) { for (d in debts) database?.debtDao()?.insert(d.copy(id = 0)) } }, { txs -> scope.launch(Dispatchers.IO) { for (t in txs) database?.transactionDao()?.insert(t.copy(id = 0)) } }); withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) "Import done — restart app" else "Import failed", Toast.LENGTH_LONG).show() } } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show() } } } } }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Saves and restores all debts, transactions, and settings as a .dena file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { exportLauncher.launch("dena-backup-${System.currentTimeMillis()}.dena") },
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
