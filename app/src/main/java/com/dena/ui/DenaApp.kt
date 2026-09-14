package com.dena.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dena.core.PaletteRegistry
import com.dena.data.DebtRepository
import com.dena.data.DenaDatabase
import com.dena.data.debt.Debt
import com.dena.ui.theme.DenaThemeMode
import com.dena.ui.components.DebtList
import com.dena.ui.components.EmptyState
import com.dena.ui.components.ScreenContainer
import com.dena.ui.screens.DebtDetailScreen
import com.dena.ui.screens.DebtFormScreen
import com.dena.ui.screens.IOweScreen
import com.dena.ui.screens.OwedToMeScreen
import com.dena.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.map

private data class DenaDestination(
    val label: String,
    val icon: ImageVector,
)

private val Destinations = listOf(
    DenaDestination("I Lent", Icons.AutoMirrored.Filled.List),
    DenaDestination("I Borrowed", Icons.Filled.Person),
    DenaDestination("Settings", Icons.Filled.Settings),
)

@Composable
fun DenaApp(database: DenaDatabase) {
    val context = LocalContext.current
    val repository = remember { DebtRepository(database.debtDao(), database.transactionDao(), database) }
    val viewModelFactory = remember { DebtViewModelFactory(repository) }
    val viewModel: DebtViewModel = viewModel(factory = viewModelFactory)

    // Collect summary values from ViewModel
    val summaryOwedToMe by viewModel.summaryOwedToMe.collectAsStateWithLifecycle()
    val summaryIOwe by viewModel.summaryIOwe.collectAsStateWithLifecycle()
    val countOwedToMe by viewModel.countOwedToMe.collectAsStateWithLifecycle()
    val countIOwe by viewModel.countIOwe.collectAsStateWithLifecycle()

    val prefs = remember(context) { com.dena.core.DenaPreferences(context) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var settingsTabReselected by rememberSaveable { mutableStateOf(false) }
    var showDebtForm by rememberSaveable { mutableStateOf(false) }
    var selectedDebtId by rememberSaveable { mutableStateOf<Long?>(null) }
    var themeState by remember {
        mutableStateOf(
            ThemeState(
                themeMode = prefs.getThemeMode(),
                followSystemTheme = prefs.isFollowSystemTheme(),
                dynamicColorsEnabled = prefs.isDynamicColorsEnabled(),
                manualDark = prefs.getDarkMode(),
                paletteId = prefs.getPaletteId(),
                paletteEnabled = prefs.isPaletteEnabled(),
                dynamicScheme = prefs.getDynamicScheme(),
                fontSize = prefs.getFontSize(),
            )
        )
    }

    // Floating action button only on main tabs (not settings)
    val showFab = selectedTab < 2

    var lastBackPressTime by rememberSaveable { mutableLongStateOf(0L) }

    BackHandler(enabled = showDebtForm) { showDebtForm = false }
    BackHandler(enabled = selectedDebtId != null) { selectedDebtId = null }
    BackHandler(enabled = !showDebtForm && selectedDebtId == null) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = now
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    com.dena.ui.theme.DenaTheme(
        themeMode = themeState.themeMode,
        palette = if (!themeState.dynamicColorsEnabled && themeState.paletteEnabled) PaletteRegistry.find(themeState.paletteId) else null,
        fontSize = themeState.fontSize,
        dynamicScheme = themeState.dynamicScheme,
        followSystemTheme = themeState.followSystemTheme,
        dynamicColorsEnabled = themeState.dynamicColorsEnabled,
        manualDark = themeState.manualDark
    ) {
        Scaffold(
            bottomBar = {
                if (selectedDebtId == null && !showDebtForm) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    if (selectedTab == index && index == 2) {
                                        settingsTabReselected = true
                                    }
                                    selectedTab = index
                                },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = {
                                    Text(
                                        destination.label,
                                        fontWeight = if (selectedTab == index) {
                                            androidx.compose.ui.text.font.FontWeight.SemiBold
                                        } else {
                                            androidx.compose.ui.text.font.FontWeight.Normal
                                        },
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showFab && selectedDebtId == null && !showDebtForm) {
                    FloatingActionButton(
                        onClick = { showDebtForm = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add debt")
                    }
                }
            },
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                if (showDebtForm) {
                    DebtFormScreen(
                        viewModel = viewModel, 
                        onBack = { showDebtForm = false },
                        initialIsOwedToMe = selectedTab == 0
                    )
                } else if (selectedDebtId != null) {
                    DebtDetailScreen(
                        debtId = selectedDebtId!!,
                        viewModel = viewModel,
                        onBack = { selectedDebtId = null },
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            0 -> OwedToMeScreen(
                                viewModel = viewModel,
                                summaryTotal = summaryOwedToMe,
                                summaryCount = countOwedToMe,
                                onDebtClick = { debt -> selectedDebtId = debt.id },
                            )
                            1 -> IOweScreen(
                                viewModel = viewModel,
                                summaryTotal = summaryIOwe,
                                summaryCount = countIOwe,
                                onDebtClick = { debt -> selectedDebtId = debt.id },
                            )
                                    2 -> SettingsScreen(
                                        themeMode = themeState.themeMode,
                                        onThemeChange = { newMode ->
                                            themeState = themeState.copy(themeMode = newMode)
                                            prefs.setThemeMode(newMode)
                                        },
                                        followSystemTheme = themeState.followSystemTheme,
                                        onFollowSystemThemeChange = { v ->
                                            themeState = themeState.copy(
                                                followSystemTheme = v,
                                                themeMode = if (v) DenaThemeMode.SYSTEM else DenaThemeMode.LIGHT
                                            )
                                            prefs.setFollowSystemTheme(v)
                                            prefs.setThemeMode(themeState.themeMode)
                                        },
                                        manualDark = themeState.manualDark,
                                        onDarkModeChange = { v ->
                                            themeState = themeState.copy(manualDark = v)
                                            prefs.setDarkMode(v)
                                        },
                                        dynamicColorsEnabled = themeState.dynamicColorsEnabled,
                                        onDynamicColorsChange = { v ->
                                            themeState = themeState.copy(
                                                dynamicColorsEnabled = v,
                                                paletteEnabled = if (v) false else themeState.paletteEnabled,
                                                themeMode = if (v) DenaThemeMode.DYNAMIC else DenaThemeMode.SYSTEM
                                            )
                                            prefs.setDynamicColorsEnabled(v)
                                            prefs.setThemeMode(themeState.themeMode)
                                            if (v) prefs.setPaletteEnabled(false)
                                        },
                                        paletteId = themeState.paletteId,
                                        onPaletteChange = { newId ->
                                            themeState = themeState.copy(paletteId = newId)
                                            prefs.setPaletteId(newId)
                                        },
                                        paletteEnabled = themeState.paletteEnabled,
                                        onPaletteEnabledChange = { enabled ->
                                            themeState = themeState.copy(paletteEnabled = enabled)
                                            prefs.setPaletteEnabled(enabled)
                                        },
                                        dynamicScheme = themeState.dynamicScheme,
                                        onDynamicSchemeChange = { newScheme ->
                                            themeState = themeState.copy(dynamicScheme = newScheme)
                                            prefs.setDynamicScheme(newScheme)
                                        },
                                        fontSize = themeState.fontSize,
                                        onFontSizeChange = { newSize ->
                                            themeState = themeState.copy(fontSize = newSize)
                                            prefs.setFontSize(newSize)
                                        },
                                        database = database,
                                    )
                        }
                    }
                }
            }
        }
    }
}