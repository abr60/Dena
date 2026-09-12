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
import com.dena.ui.components.SectionHeader
import com.dena.ui.components.SegmentedControl
import com.dena.ui.components.SelectOption
import com.dena.ui.components.SettingsGroup
import com.dena.ui.components.SettingsRow
import com.dena.ui.components.SettingsSubpageScaffold
import com.dena.ui.components.ToggleRow
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
    database: DenaDatabase? = null,
) {
    val context = LocalContext.current
    val prefs = remember { DenaPreferences(context) }
    var subpage by remember { mutableStateOf<String?>(null) }
    var isUnlocked by remember { mutableStateOf(prefs.isUnlocked()) }
    var tapCount by remember { mutableStateOf(0) }
    
    val currentPalette = PaletteRegistry.find(paletteId)

    BackHandler(enabled = subpage != null) { subpage = null }

        when (subpage) {
        "activity" -> { RecentActivitySubpage(database = database, onBack = { subpage = null }); return }
        "export" -> { ExportImportSubpage(database = database, onBack = { subpage = null }); return }
        "appearance" -> { 
            AppearanceSubpage(
                themeMode, onThemeChange, paletteId, onPaletteChange, 
                onPaletteEnabledChange, paletteEnabled, isUnlocked, 
                onUnlock = { isUnlocked = true; prefs.setUnlocked(true) }
            ) { subpage = null }
            return 
        }
        "userinterface" -> { UserPreferencesSubpage(database, onBack = { subpage = null }); return }
    }


    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Box(modifier = Modifier.clickable { 
            tapCount++
            if (tapCount == 5 && !isUnlocked) {
                isUnlocked = true
                prefs.setUnlocked(true)
                Toast.makeText(context, "Advanced theme engine unlocked!", Toast.LENGTH_SHORT).show()
            }
        }) {
            AppHeader(secondary = "v${BuildConfig.VERSION_NAME}")
        }
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SettingsGroup {
            NavRow(label = "Appearance", icon = Icons.Filled.Settings, caption = "Theme & color palette", onClick = { subpage = "appearance" })
            NavRow(label = "User Preferences", icon = Icons.Filled.Person, caption = "Language, currency, font size", onClick = { subpage = "userinterface" }, showDivider = true)
            NavRow(label = "Data", icon = Icons.Filled.Info, caption = "Backup & restore", onClick = { subpage = "export" }, showDivider = true)
            NavRow(label = "Recent Activity", icon = Icons.AutoMirrored.Filled.List, caption = "All transactions", onClick = { subpage = "activity" }, showDivider = true)
        }

        SettingsGroup {
            SettingsRow(label = "About", value = "Dena v${BuildConfig.VERSION_NAME}")
        }
    }
}

@Composable
fun AppearanceSubpage(
    themeMode: DenaThemeMode, onThemeChange: (DenaThemeMode) -> Unit,
    paletteId: String, onPaletteChange: (String) -> Unit,
    onPaletteEnabledChange: (Boolean) -> Unit,
    paletteEnabled: Boolean,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    onBack: () -> Unit
) {
    SettingsSubpageScaffold(title = "Appearance", onBack = onBack) {
        val context = LocalContext.current
        val prefs = remember { DenaPreferences(context) }
        var fontSize by remember { mutableStateOf(prefs.getFontSize()) }
        
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionHeader("MONOCHROME")
            ToggleRow(label = "Follow system", caption = "On: match system light/dark. Off: use opposite.", checked = themeMode == DenaThemeMode.SYSTEM, onCheckedChange = { if (it) onThemeChange(DenaThemeMode.SYSTEM) else onThemeChange(DenaThemeMode.LIGHT) })
            
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Font size", style = MaterialTheme.typography.bodyMedium)
                SegmentedControl(options = listOf("Small", "Medium", "Large"), selectedIndex = when (fontSize) { "small" -> 0; "large" -> 2; else -> 1 }, onSelect = { fontSize = when (it) { 0 -> "small"; 2 -> "large"; else -> "medium" }; prefs.setFontSize(fontSize) })
            }

            if (isUnlocked) {
                SectionHeader("ADVANCED")
                ToggleRow(label = "Dynamic colors (Material You)", caption = "Use wallpaper colors (Android 12+)", checked = themeMode == DenaThemeMode.DYNAMIC, onCheckedChange = { 
                    if (it) {
                        onThemeChange(DenaThemeMode.DYNAMIC)
                        onPaletteEnabledChange(false)
                    } else {
                        onThemeChange(DenaThemeMode.LIGHT)
                    }
                })
                
                SectionHeader("COLOR PALETTE")
                ToggleRow(label = "Enable Omarchy Palette", caption = "Apply curated color theme", checked = paletteEnabled, onCheckedChange = { 
                    onPaletteEnabledChange(it)
                    if (it) {
                        onThemeChange(DenaThemeMode.LIGHT) // Disable dynamic if enabling palette
                    }
                })

                if (paletteEnabled) {
                    val paletteOptions = PaletteRegistry.all.map { SelectOption("${it.category.name.lowercase().replaceFirstChar { c -> c.uppercase() }} · ${it.displayName}", it.tokens.accent) }
                    val currentPalette = PaletteRegistry.find(paletteId) ?: PaletteRegistry.all.first()

                    DenaSelect(
                        value = "${currentPalette.category.name.lowercase().replaceFirstChar { c -> c.uppercase() }} · ${currentPalette.displayName}",
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
    SettingsSubpageScaffold(title = "User Preferences", onBack = onBack) {
        val context = LocalContext.current
        val prefs = remember { DenaPreferences(context) }
        var language by remember { mutableStateOf(prefs.getLanguage()) }
        var showDecimals by remember { mutableStateOf(prefs.showDecimals()) }
        var currency by remember { mutableStateOf(prefs.getCurrency()) }
        var showCurrencyPicker by remember { mutableStateOf(false) }
        var showLanguagePicker by remember { mutableStateOf(false) }
        var historyCleanDays by remember { mutableStateOf(prefs.getHistoryCleanDays()) }

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
            
            SectionHeader("RECENT ACTIVITY")
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
    }
}

@Composable
fun RecentActivitySubpage(database: DenaDatabase?, onBack: () -> Unit) {
    val context = LocalContext.current
    val txs by (database?.transactionDao()?.observeAll()?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val debtsMap = remember { mutableStateOf(mapOf<Long, String>()) }
    val prefs = remember { DenaPreferences(context) }
    val cleanDays = remember { prefs.getHistoryCleanDays() }
    val filtered = remember(txs, cleanDays) { if (cleanDays == 0) txs else { val cutoff = System.currentTimeMillis() - (cleanDays.toLong() * 24 * 60 * 60 * 1000); txs.filter { it.timestamp >= cutoff } } }
    LaunchedEffect(txs) { if (database != null) { val debts = database.debtDao().getAllOnce(); debtsMap.value = debts.associate { it.id to it.contactName } } }
    SettingsSubpageScaffold(title = "Recent Activity", onBack = onBack) {
        if (filtered.isEmpty()) { val message = if (cleanDays > 0) "No activity in the last $cleanDays days" else "No transactions yet"; Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { t -> val name = debtsMap.value[t.debtId] ?: "Debt #${t.debtId}"; Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text("$name • ${t.direction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(t.note.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium) }; Column(horizontalAlignment = Alignment.End) { val showDecimals = DenaPreferences(context).showDecimals(); val sym = DenaPreferences(context).getCurrencySymbol(); Text(formatCurrencyRaw(t.amount, sym, showDecimals), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold); Text(formatRelativeDate(t.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } } }
    }
}

@Composable
fun ExportImportSubpage(database: DenaDatabase?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contentResolver = context.contentResolver
    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> if (uri != null) { scope.launch(Dispatchers.IO) { try { val debts = database?.debtDao()?.getAllOnce() ?: emptyList(); val txs = database?.transactionDao()?.getAllOnce() ?: emptyList(); val json = BackupHelper.exportProfileToJson(debts, txs); contentResolver.openOutputStream(uri)?.use { output -> output.write(json.toByteArray()) }; withContext(Dispatchers.Main) { Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show() } } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show() } } } } }
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { scope.launch(Dispatchers.IO) { try { val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }; if (content == null) { withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid backup file", Toast.LENGTH_SHORT).show() }; return@launch }; val decoded = BackupHelper.decodeBackupString(content); if (decoded == null) { withContext(Dispatchers.Main) { Toast.makeText(context, "Invalid backup file", Toast.LENGTH_SHORT).show() }; return@launch }; val ok = BackupHelper.importProfileFromString(decoded, { debts -> scope.launch(Dispatchers.IO) { for (d in debts) database?.debtDao()?.insert(d.copy(id = 0)) } }, { txs -> scope.launch(Dispatchers.IO) { for (t in txs) database?.transactionDao()?.insert(t.copy(id = 0)) } }); withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) "Import done — restart app" else "Import failed", Toast.LENGTH_LONG).show() } } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show() } } } } }
    SettingsSubpageScaffold(title = "Data", onBack = onBack) { Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = { exportLauncher.launch("dena-backup-${System.currentTimeMillis()}.dena") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Export Backup") }; OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Restore Backup") }; Text(text = "Saves and restores all debts, transactions, and settings as a .dena file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}
